package com.lapxpert.backend.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket Health Monitor Service
 * Tracks connection health, monitors heartbeats, and provides connection quality metrics
 * Maintains Vietnamese business terminology for LapXpert system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketHealthMonitor {

    private final SimpMessagingTemplate messagingTemplate;

    // Connection tracking
    private final ConcurrentHashMap<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalMessagesSent = new AtomicLong(0);

    // Health metrics
    private volatile Instant lastHealthCheck = Instant.now();
    private volatile boolean isHealthy = true;
    private volatile String healthStatus = "HEALTHY";

    /**
     * Connection information tracking
     */
    public static class ConnectionInfo {
        private final String sessionId;
        private final Instant connectedAt;
        private volatile Instant lastActivity;
        private volatile int subscriptionCount;
        private volatile long messagesReceived;
        private volatile long messagesSent;
        private volatile boolean isActive;

        public ConnectionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.connectedAt = Instant.now();
            this.lastActivity = Instant.now();
            this.subscriptionCount = 0;
            this.messagesReceived = 0;
            this.messagesSent = 0;
            this.isActive = true;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public Instant getConnectedAt() { return connectedAt; }
        public Instant getLastActivity() { return lastActivity; }
        public int getSubscriptionCount() { return subscriptionCount; }
        public long getMessagesReceived() { return messagesReceived; }
        public long getMessagesSent() { return messagesSent; }
        public boolean isActive() { return isActive; }

        // Update methods
        public void updateActivity() { this.lastActivity = Instant.now(); }
        public void incrementSubscriptions() { this.subscriptionCount++; }
        public void decrementSubscriptions() { this.subscriptionCount--; }
        public void incrementMessagesReceived() { this.messagesReceived++; }
        public void incrementMessagesSent() { this.messagesSent++; }
        public void setInactive() { this.isActive = false; }
    }

    /**
     * Handle WebSocket connection events
     */
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = new ConnectionInfo(sessionId);
            activeConnections.put(sessionId, connectionInfo);
            totalConnections.incrementAndGet();
            
            log.info("WebSocket session connected: {} (Total active: {})", 
                sessionId, activeConnections.size());
            
            // Broadcast connection status update
            broadcastHealthStatus();
        }
    }

    /**
     * Handle WebSocket disconnection events
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = activeConnections.remove(sessionId);
            if (connectionInfo != null) {
                connectionInfo.setInactive();
                log.info("WebSocket session disconnected: {} (Total active: {})", 
                    sessionId, activeConnections.size());
                
                // Broadcast connection status update
                broadcastHealthStatus();
            }
        }
    }

    /**
     * Handle subscription events
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = activeConnections.get(sessionId);
            if (connectionInfo != null) {
                connectionInfo.incrementSubscriptions();
                connectionInfo.updateActivity();
                
                log.debug("Session {} subscribed to: {}", sessionId, destination);
            }
        }
    }

    /**
     * Handle unsubscription events
     */
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = activeConnections.get(sessionId);
            if (connectionInfo != null) {
                connectionInfo.decrementSubscriptions();
                connectionInfo.updateActivity();
                
                log.debug("Session {} unsubscribed", sessionId);
            }
        }
    }

    /**
     * Periodic health check - runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void performHealthCheck() {
        try {
            lastHealthCheck = Instant.now();
            
            // Check for stale connections (inactive for more than 5 minutes)
            Instant staleThreshold = Instant.now().minusSeconds(300);
            int staleConnections = 0;
            
            for (ConnectionInfo connection : activeConnections.values()) {
                if (connection.getLastActivity().isBefore(staleThreshold)) {
                    staleConnections++;
                }
            }
            
            // Update health status
            if (staleConnections > activeConnections.size() * 0.5) {
                isHealthy = false;
                healthStatus = "DEGRADED - Many stale connections";
            } else if (staleConnections > 0) {
                isHealthy = true;
                healthStatus = "WARNING - Some stale connections";
            } else {
                isHealthy = true;
                healthStatus = "HEALTHY";
            }
            
            log.debug("WebSocket health check completed. Status: {} (Active: {}, Stale: {})", 
                healthStatus, activeConnections.size(), staleConnections);
            
            // Broadcast health status every 5 minutes
            if (lastHealthCheck.getEpochSecond() % 300 == 0) {
                broadcastHealthStatus();
            }
            
        } catch (Exception e) {
            log.error("Error during WebSocket health check: {}", e.getMessage(), e);
            isHealthy = false;
            healthStatus = "ERROR - Health check failed";
        }
    }

    /**
     * Broadcast health status to monitoring topics
     */
    public void broadcastHealthStatus() {
        try {
            WebSocketHealthStatus status = new WebSocketHealthStatus(
                isHealthy,
                healthStatus,
                activeConnections.size(),
                totalConnections.get(),
                totalMessagesReceived.get(),
                totalMessagesSent.get(),
                lastHealthCheck
            );
            
            // Send to health monitoring topic
            messagingTemplate.convertAndSend("/topic/websocket/health", status);
            
            log.debug("Broadcasted WebSocket health status: {}", healthStatus);
            
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket health status: {}", e.getMessage(), e);
        }
    }

    /**
     * Get current health metrics
     */
    public WebSocketHealthMetrics getHealthMetrics() {
        return new WebSocketHealthMetrics(
            isHealthy,
            healthStatus,
            activeConnections.size(),
            totalConnections.get(),
            totalMessagesReceived.get(),
            totalMessagesSent.get(),
            lastHealthCheck,
            activeConnections.values().stream()
                .mapToInt(ConnectionInfo::getSubscriptionCount)
                .sum()
        );
    }

    /**
     * Record message activity
     */
    public void recordMessageReceived(String sessionId) {
        totalMessagesReceived.incrementAndGet();
        ConnectionInfo connection = activeConnections.get(sessionId);
        if (connection != null) {
            connection.incrementMessagesReceived();
            connection.updateActivity();
        }
    }

    public void recordMessageSent(String sessionId) {
        totalMessagesSent.incrementAndGet();
        ConnectionInfo connection = activeConnections.get(sessionId);
        if (connection != null) {
            connection.incrementMessagesSent();
            connection.updateActivity();
        }
    }

    /**
     * Health status DTO for broadcasting
     */
    public static class WebSocketHealthStatus {
        private final boolean healthy;
        private final String status;
        private final int activeConnections;
        private final int totalConnections;
        private final long totalMessagesReceived;
        private final long totalMessagesSent;
        private final Instant lastCheck;

        public WebSocketHealthStatus(boolean healthy, String status, int activeConnections, 
                                   int totalConnections, long totalMessagesReceived, 
                                   long totalMessagesSent, Instant lastCheck) {
            this.healthy = healthy;
            this.status = status;
            this.activeConnections = activeConnections;
            this.totalConnections = totalConnections;
            this.totalMessagesReceived = totalMessagesReceived;
            this.totalMessagesSent = totalMessagesSent;
            this.lastCheck = lastCheck;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public int getActiveConnections() { return activeConnections; }
        public int getTotalConnections() { return totalConnections; }
        public long getTotalMessagesReceived() { return totalMessagesReceived; }
        public long getTotalMessagesSent() { return totalMessagesSent; }
        public Instant getLastCheck() { return lastCheck; }
    }

    /**
     * Detailed health metrics DTO
     */
    public static class WebSocketHealthMetrics {
        private final boolean healthy;
        private final String status;
        private final int activeConnections;
        private final int totalConnections;
        private final long totalMessagesReceived;
        private final long totalMessagesSent;
        private final Instant lastCheck;
        private final int totalSubscriptions;

        public WebSocketHealthMetrics(boolean healthy, String status, int activeConnections, 
                                    int totalConnections, long totalMessagesReceived, 
                                    long totalMessagesSent, Instant lastCheck, int totalSubscriptions) {
            this.healthy = healthy;
            this.status = status;
            this.activeConnections = activeConnections;
            this.totalConnections = totalConnections;
            this.totalMessagesReceived = totalMessagesReceived;
            this.totalMessagesSent = totalMessagesSent;
            this.lastCheck = lastCheck;
            this.totalSubscriptions = totalSubscriptions;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public int getActiveConnections() { return activeConnections; }
        public int getTotalConnections() { return totalConnections; }
        public long getTotalMessagesReceived() { return totalMessagesReceived; }
        public long getTotalMessagesSent() { return totalMessagesSent; }
        public Instant getLastCheck() { return lastCheck; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
    }
}
