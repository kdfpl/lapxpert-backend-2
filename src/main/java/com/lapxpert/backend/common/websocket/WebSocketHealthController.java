package com.lapxpert.backend.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket Health Monitoring REST Controller
 * Provides endpoints for monitoring WebSocket connection health and error recovery
 * Maintains Vietnamese business terminology for LapXpert system
 */
@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebSocketHealthController {

    private final WebSocketHealthMonitor healthMonitor;
    private final WebSocketErrorRecovery errorRecovery;

    /**
     * Get current WebSocket health status
     */
    @GetMapping("/health")
    public ResponseEntity<WebSocketHealthMonitor.WebSocketHealthMetrics> getHealthStatus() {
        try {
            WebSocketHealthMonitor.WebSocketHealthMetrics metrics = healthMonitor.getHealthMetrics();
            log.debug("WebSocket health status requested: {}", metrics.getStatus());
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Failed to get WebSocket health status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get error recovery statistics
     */
    @GetMapping("/errors")
    public ResponseEntity<WebSocketErrorRecovery.ErrorStatistics> getErrorStatistics() {
        try {
            WebSocketErrorRecovery.ErrorStatistics stats = errorRecovery.getErrorStatistics();
            log.debug("WebSocket error statistics requested");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get WebSocket error statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Trigger manual health check
     */
    @PostMapping("/health/check")
    public ResponseEntity<String> triggerHealthCheck() {
        try {
            healthMonitor.performHealthCheck();
            log.info("Manual WebSocket health check triggered");
            return ResponseEntity.ok("Health check completed successfully");
        } catch (Exception e) {
            log.error("Failed to trigger health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to trigger health check: " + e.getMessage());
        }
    }

    /**
     * Broadcast health status manually
     */
    @PostMapping("/health/broadcast")
    public ResponseEntity<String> broadcastHealthStatus() {
        try {
            healthMonitor.broadcastHealthStatus();
            log.info("Manual WebSocket health status broadcast triggered");
            return ResponseEntity.ok("Health status broadcasted successfully");
        } catch (Exception e) {
            log.error("Failed to broadcast health status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to broadcast health status: " + e.getMessage());
        }
    }

    /**
     * Record a test error for testing recovery mechanisms
     */
    @PostMapping("/test-error")
    public ResponseEntity<String> recordTestError(
            @RequestParam String sessionId,
            @RequestParam String errorMessage,
            @RequestParam(defaultValue = "UNKNOWN") String errorType) {
        try {
            WebSocketErrorRecovery.ErrorType type = WebSocketErrorRecovery.ErrorType.valueOf(errorType);
            errorRecovery.recordError(sessionId, errorMessage, type);
            
            log.info("Test error recorded for session {}: {} ({})", sessionId, errorMessage, errorType);
            return ResponseEntity.ok("Test error recorded successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid error type: {}", errorType);
            return ResponseEntity.badRequest()
                .body("Invalid error type: " + errorType);
        } catch (Exception e) {
            log.error("Failed to record test error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to record test error: " + e.getMessage());
        }
    }

    /**
     * Get detailed connection information
     */
    @GetMapping("/connections")
    public ResponseEntity<ConnectionSummary> getConnectionSummary() {
        try {
            WebSocketHealthMonitor.WebSocketHealthMetrics metrics = healthMonitor.getHealthMetrics();
            WebSocketErrorRecovery.ErrorStatistics errorStats = errorRecovery.getErrorStatistics();
            
            ConnectionSummary summary = new ConnectionSummary(
                metrics.getActiveConnections(),
                metrics.getTotalConnections(),
                metrics.getTotalSubscriptions(),
                metrics.getTotalMessagesReceived(),
                metrics.getTotalMessagesSent(),
                errorStats.getTotalErrors(),
                errorStats.getActiveErrorSessions(),
                errorStats.getTotalRetryAttempts(),
                metrics.isHealthy(),
                metrics.getStatus()
            );
            
            log.debug("Connection summary requested");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get connection summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Connection summary DTO
     */
    public static class ConnectionSummary {
        private final int activeConnections;
        private final int totalConnections;
        private final int totalSubscriptions;
        private final long totalMessagesReceived;
        private final long totalMessagesSent;
        private final int totalErrors;
        private final int activeErrorSessions;
        private final int totalRetryAttempts;
        private final boolean healthy;
        private final String status;

        public ConnectionSummary(int activeConnections, int totalConnections, int totalSubscriptions,
                               long totalMessagesReceived, long totalMessagesSent, int totalErrors,
                               int activeErrorSessions, int totalRetryAttempts, boolean healthy, String status) {
            this.activeConnections = activeConnections;
            this.totalConnections = totalConnections;
            this.totalSubscriptions = totalSubscriptions;
            this.totalMessagesReceived = totalMessagesReceived;
            this.totalMessagesSent = totalMessagesSent;
            this.totalErrors = totalErrors;
            this.activeErrorSessions = activeErrorSessions;
            this.totalRetryAttempts = totalRetryAttempts;
            this.healthy = healthy;
            this.status = status;
        }

        // Getters
        public int getActiveConnections() { return activeConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
        public long getTotalMessagesReceived() { return totalMessagesReceived; }
        public long getTotalMessagesSent() { return totalMessagesSent; }
        public int getTotalErrors() { return totalErrors; }
        public int getActiveErrorSessions() { return activeErrorSessions; }
        public int getTotalRetryAttempts() { return totalRetryAttempts; }
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
    }
}
