package com.lapxpert.backend.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket Error Recovery Service
 * Handles connection failures, implements retry logic, and manages graceful degradation
 * Maintains Vietnamese business terminology for LapXpert system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketErrorRecovery {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketHealthMonitor healthMonitor;

    // Error tracking
    private final ConcurrentHashMap<String, ErrorInfo> sessionErrors = new ConcurrentHashMap<>();
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    private final ScheduledExecutorService recoveryExecutor = Executors.newScheduledThreadPool(2);

    // Recovery configuration
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long BASE_RETRY_DELAY_MS = 1000; // 1 second
    private static final long MAX_RETRY_DELAY_MS = 30000; // 30 seconds
    private static final long ERROR_CLEANUP_INTERVAL_MS = 300000; // 5 minutes

    /**
     * Error information tracking
     */
    public static class ErrorInfo {
        private final String sessionId;
        private final Instant firstErrorTime;
        private volatile Instant lastErrorTime;
        private volatile int errorCount;
        private volatile int retryAttempts;
        private volatile String lastErrorMessage;
        private volatile ErrorType errorType;
        private volatile boolean isRecovering;

        public ErrorInfo(String sessionId, String errorMessage, ErrorType errorType) {
            this.sessionId = sessionId;
            this.firstErrorTime = Instant.now();
            this.lastErrorTime = Instant.now();
            this.errorCount = 1;
            this.retryAttempts = 0;
            this.lastErrorMessage = errorMessage;
            this.errorType = errorType;
            this.isRecovering = false;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public Instant getFirstErrorTime() { return firstErrorTime; }
        public Instant getLastErrorTime() { return lastErrorTime; }
        public int getErrorCount() { return errorCount; }
        public int getRetryAttempts() { return retryAttempts; }
        public String getLastErrorMessage() { return lastErrorMessage; }
        public ErrorType getErrorType() { return errorType; }
        public boolean isRecovering() { return isRecovering; }

        // Update methods
        public void recordError(String errorMessage, ErrorType errorType) {
            this.lastErrorTime = Instant.now();
            this.errorCount++;
            this.lastErrorMessage = errorMessage;
            this.errorType = errorType;
        }

        public void incrementRetryAttempts() { this.retryAttempts++; }
        public void setRecovering(boolean recovering) { this.isRecovering = recovering; }
        public void resetRetryAttempts() { this.retryAttempts = 0; }
    }

    /**
     * Error types for categorization
     */
    public enum ErrorType {
        CONNECTION_FAILED("Kết nối thất bại"),
        MESSAGE_SEND_FAILED("Gửi tin nhắn thất bại"),
        SUBSCRIPTION_FAILED("Đăng ký topic thất bại"),
        HEARTBEAT_TIMEOUT("Timeout heartbeat"),
        AUTHENTICATION_FAILED("Xác thực thất bại"),
        UNKNOWN("Lỗi không xác định");

        private final String vietnameseDescription;

        ErrorType(String vietnameseDescription) {
            this.vietnameseDescription = vietnameseDescription;
        }

        public String getVietnameseDescription() {
            return vietnameseDescription;
        }
    }

    /**
     * Record an error for a session
     */
    public void recordError(String sessionId, String errorMessage, ErrorType errorType) {
        totalErrors.incrementAndGet();
        
        ErrorInfo errorInfo = sessionErrors.computeIfAbsent(sessionId, 
            id -> new ErrorInfo(id, errorMessage, errorType));
        
        if (errorInfo.getSessionId().equals(sessionId)) {
            errorInfo.recordError(errorMessage, errorType);
        }
        
        log.warn("WebSocket error recorded for session {}: {} ({})", 
            sessionId, errorMessage, errorType.getVietnameseDescription());
        
        // Trigger recovery if not already in progress
        if (!errorInfo.isRecovering() && errorInfo.getRetryAttempts() < MAX_RETRY_ATTEMPTS) {
            initiateRecovery(sessionId, errorInfo);
        }
        
        // Broadcast error status
        broadcastErrorStatus(sessionId, errorInfo);
    }

    /**
     * Initiate recovery process for a session
     */
    private void initiateRecovery(String sessionId, ErrorInfo errorInfo) {
        errorInfo.setRecovering(true);
        errorInfo.incrementRetryAttempts();
        
        // Calculate exponential backoff delay
        long delay = calculateRetryDelay(errorInfo.getRetryAttempts());
        
        log.info("Initiating WebSocket recovery for session {} (attempt {}/{})", 
            sessionId, errorInfo.getRetryAttempts(), MAX_RETRY_ATTEMPTS);
        
        recoveryExecutor.schedule(() -> {
            try {
                performRecovery(sessionId, errorInfo);
            } catch (Exception e) {
                log.error("Recovery attempt failed for session {}: {}", sessionId, e.getMessage(), e);
                errorInfo.setRecovering(false);
                
                // Schedule next retry if attempts remaining
                if (errorInfo.getRetryAttempts() < MAX_RETRY_ATTEMPTS) {
                    initiateRecovery(sessionId, errorInfo);
                } else {
                    log.error("Max retry attempts reached for session {}. Giving up recovery.", sessionId);
                    broadcastRecoveryFailed(sessionId, errorInfo);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Perform actual recovery operations
     */
    private void performRecovery(String sessionId, ErrorInfo errorInfo) {
        log.debug("Performing recovery for session {} (error type: {})", 
            sessionId, errorInfo.getErrorType());
        
        switch (errorInfo.getErrorType()) {
            case CONNECTION_FAILED:
                recoverConnection(sessionId, errorInfo);
                break;
            case MESSAGE_SEND_FAILED:
                recoverMessageSending(sessionId, errorInfo);
                break;
            case SUBSCRIPTION_FAILED:
                recoverSubscription(sessionId, errorInfo);
                break;
            case HEARTBEAT_TIMEOUT:
                recoverHeartbeat(sessionId, errorInfo);
                break;
            default:
                performGenericRecovery(sessionId, errorInfo);
                break;
        }
        
        errorInfo.setRecovering(false);
    }

    /**
     * Recover connection issues
     */
    private void recoverConnection(String sessionId, ErrorInfo errorInfo) {
        try {
            // Send connection recovery notification
            messagingTemplate.convertAndSend("/topic/websocket/recovery", 
                new RecoveryNotification(sessionId, "CONNECTION_RECOVERY", 
                    "Đang khôi phục kết nối WebSocket", errorInfo.getRetryAttempts()));
            
            // Connection recovery is handled by client-side reconnection logic
            log.info("Connection recovery notification sent for session {}", sessionId);
            
            // Mark as recovered if no new errors in the last 30 seconds
            if (errorInfo.getLastErrorTime().isBefore(Instant.now().minusSeconds(30))) {
                markRecovered(sessionId);
            }
            
        } catch (Exception e) {
            log.error("Failed to recover connection for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Recover message sending issues
     */
    private void recoverMessageSending(String sessionId, ErrorInfo errorInfo) {
        try {
            // Test message sending capability
            messagingTemplate.convertAndSend("/topic/websocket/test", 
                new TestMessage(sessionId, "Recovery test message", Instant.now()));
            
            log.info("Message sending recovery test completed for session {}", sessionId);
            markRecovered(sessionId);
            
        } catch (Exception e) {
            log.error("Failed to recover message sending for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Recover subscription issues
     */
    private void recoverSubscription(String sessionId, ErrorInfo errorInfo) {
        try {
            // Send subscription recovery notification
            messagingTemplate.convertAndSend("/topic/websocket/recovery", 
                new RecoveryNotification(sessionId, "SUBSCRIPTION_RECOVERY", 
                    "Đang khôi phục đăng ký topic", errorInfo.getRetryAttempts()));
            
            log.info("Subscription recovery notification sent for session {}", sessionId);
            markRecovered(sessionId);
            
        } catch (Exception e) {
            log.error("Failed to recover subscription for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Recover heartbeat timeout issues
     */
    private void recoverHeartbeat(String sessionId, ErrorInfo errorInfo) {
        try {
            // Send heartbeat recovery notification
            messagingTemplate.convertAndSend("/topic/websocket/heartbeat", 
                new HeartbeatMessage(sessionId, Instant.now(), "RECOVERY"));
            
            log.info("Heartbeat recovery completed for session {}", sessionId);
            markRecovered(sessionId);
            
        } catch (Exception e) {
            log.error("Failed to recover heartbeat for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Perform generic recovery
     */
    private void performGenericRecovery(String sessionId, ErrorInfo errorInfo) {
        try {
            // Send generic recovery notification
            messagingTemplate.convertAndSend("/topic/websocket/recovery", 
                new RecoveryNotification(sessionId, "GENERIC_RECOVERY", 
                    "Đang thực hiện khôi phục chung", errorInfo.getRetryAttempts()));
            
            log.info("Generic recovery completed for session {}", sessionId);
            markRecovered(sessionId);
            
        } catch (Exception e) {
            log.error("Failed to perform generic recovery for session {}: {}", sessionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Mark session as recovered
     */
    private void markRecovered(String sessionId) {
        ErrorInfo errorInfo = sessionErrors.remove(sessionId);
        if (errorInfo != null) {
            log.info("Session {} marked as recovered after {} attempts", 
                sessionId, errorInfo.getRetryAttempts());
            
            // Broadcast recovery success
            broadcastRecoverySuccess(sessionId, errorInfo);
        }
    }

    /**
     * Calculate exponential backoff delay
     */
    private long calculateRetryDelay(int retryAttempt) {
        long delay = BASE_RETRY_DELAY_MS * (long) Math.pow(2, retryAttempt - 1);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Broadcast error status
     */
    private void broadcastErrorStatus(String sessionId, ErrorInfo errorInfo) {
        try {
            ErrorStatusNotification notification = new ErrorStatusNotification(
                sessionId,
                errorInfo.getErrorType().name(),
                errorInfo.getErrorType().getVietnameseDescription(),
                errorInfo.getErrorCount(),
                errorInfo.getRetryAttempts(),
                errorInfo.isRecovering()
            );
            
            messagingTemplate.convertAndSend("/topic/websocket/errors", notification);
            
        } catch (Exception e) {
            log.error("Failed to broadcast error status: {}", e.getMessage());
        }
    }

    /**
     * Broadcast recovery success
     */
    private void broadcastRecoverySuccess(String sessionId, ErrorInfo errorInfo) {
        try {
            RecoveryNotification notification = new RecoveryNotification(
                sessionId, "RECOVERY_SUCCESS", 
                "Khôi phục WebSocket thành công", errorInfo.getRetryAttempts());
            
            messagingTemplate.convertAndSend("/topic/websocket/recovery", notification);
            
        } catch (Exception e) {
            log.error("Failed to broadcast recovery success: {}", e.getMessage());
        }
    }

    /**
     * Broadcast recovery failure
     */
    private void broadcastRecoveryFailed(String sessionId, ErrorInfo errorInfo) {
        try {
            RecoveryNotification notification = new RecoveryNotification(
                sessionId, "RECOVERY_FAILED", 
                "Khôi phục WebSocket thất bại sau " + MAX_RETRY_ATTEMPTS + " lần thử", 
                errorInfo.getRetryAttempts());
            
            messagingTemplate.convertAndSend("/topic/websocket/recovery", notification);
            
        } catch (Exception e) {
            log.error("Failed to broadcast recovery failure: {}", e.getMessage());
        }
    }

    /**
     * Get error statistics
     */
    public ErrorStatistics getErrorStatistics() {
        return new ErrorStatistics(
            totalErrors.get(),
            sessionErrors.size(),
            sessionErrors.values().stream().mapToInt(ErrorInfo::getRetryAttempts).sum(),
            sessionErrors.values().stream().anyMatch(ErrorInfo::isRecovering)
        );
    }

    // DTO classes for notifications
    public static class RecoveryNotification {
        private final String sessionId;
        private final String type;
        private final String message;
        private final int attempt;

        public RecoveryNotification(String sessionId, String type, String message, int attempt) {
            this.sessionId = sessionId;
            this.type = type;
            this.message = message;
            this.attempt = attempt;
        }

        public String getSessionId() { return sessionId; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public int getAttempt() { return attempt; }
    }

    public static class TestMessage {
        private final String sessionId;
        private final String content;
        private final Instant timestamp;

        public TestMessage(String sessionId, String content, Instant timestamp) {
            this.sessionId = sessionId;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getSessionId() { return sessionId; }
        public String getContent() { return content; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class HeartbeatMessage {
        private final String sessionId;
        private final Instant timestamp;
        private final String type;

        public HeartbeatMessage(String sessionId, Instant timestamp, String type) {
            this.sessionId = sessionId;
            this.timestamp = timestamp;
            this.type = type;
        }

        public String getSessionId() { return sessionId; }
        public Instant getTimestamp() { return timestamp; }
        public String getType() { return type; }
    }

    public static class ErrorStatusNotification {
        private final String sessionId;
        private final String errorType;
        private final String description;
        private final int errorCount;
        private final int retryAttempts;
        private final boolean recovering;

        public ErrorStatusNotification(String sessionId, String errorType, String description, 
                                     int errorCount, int retryAttempts, boolean recovering) {
            this.sessionId = sessionId;
            this.errorType = errorType;
            this.description = description;
            this.errorCount = errorCount;
            this.retryAttempts = retryAttempts;
            this.recovering = recovering;
        }

        public String getSessionId() { return sessionId; }
        public String getErrorType() { return errorType; }
        public String getDescription() { return description; }
        public int getErrorCount() { return errorCount; }
        public int getRetryAttempts() { return retryAttempts; }
        public boolean isRecovering() { return recovering; }
    }

    public static class ErrorStatistics {
        private final int totalErrors;
        private final int activeErrorSessions;
        private final int totalRetryAttempts;
        private final boolean hasRecoveringSession;

        public ErrorStatistics(int totalErrors, int activeErrorSessions, 
                             int totalRetryAttempts, boolean hasRecoveringSession) {
            this.totalErrors = totalErrors;
            this.activeErrorSessions = activeErrorSessions;
            this.totalRetryAttempts = totalRetryAttempts;
            this.hasRecoveringSession = hasRecoveringSession;
        }

        public int getTotalErrors() { return totalErrors; }
        public int getActiveErrorSessions() { return activeErrorSessions; }
        public int getTotalRetryAttempts() { return totalRetryAttempts; }
        public boolean isHasRecoveringSession() { return hasRecoveringSession; }
    }
}
