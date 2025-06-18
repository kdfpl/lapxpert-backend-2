package com.lapxpert.backend.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced WebSocket Connection Manager
 *
 * Provides enhanced connection tracking and metrics for the monolithic application's
 * WebSocket capabilities. Tracks connections and subscriptions with Vietnamese topic support.
 *
 * Features:
 * - Connection lifecycle tracking
 * - Subscription management with Vietnamese topic support
 * - Basic metrics collection
 * - Graceful connection cleanup
 *
 * Can be enabled/disabled via: websocket.enhanced.connection-tracking.enabled=true/false
 */
@Service
@ConditionalOnProperty(name = "websocket.enhanced.connection-tracking.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionManager {

    // Connection tracking
    private final ConcurrentHashMap<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToUser = new ConcurrentHashMap<>();
    
    // Metrics
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicInteger totalSubscriptions = new AtomicInteger(0);

    /**
     * Handle new WebSocket connection
     */
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = new ConnectionInfo(
                sessionId,
                LocalDateTime.now(),
                event.getUser() != null ? event.getUser().getName() : null
            );
            
            activeConnections.put(sessionId, connectionInfo);
            totalConnections.incrementAndGet();
            
            // Track user session mapping if authenticated
            if (event.getUser() != null) {
                sessionToUser.put(sessionId, event.getUser().getName());
            }
            
            log.info("WebSocket connection established - Session: {}, User: {}, Total connections: {}", 
                    sessionId, 
                    event.getUser() != null ? event.getUser().getName() : "anonymous",
                    activeConnections.size());
            
            // Log connection event for monitoring
            logConnectionEvent("CONNECT", sessionId, event.getUser() != null ? event.getUser().getName() : null);
        }
    }

    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = activeConnections.remove(sessionId);
            String username = sessionToUser.remove(sessionId);
            
            if (connectionInfo != null) {
                // Update metrics
                totalSubscriptions.addAndGet(-connectionInfo.getSubscriptionCount());
                
                log.info("WebSocket connection closed - Session: {}, User: {}, Duration: {}ms, Total connections: {}", 
                        sessionId,
                        username != null ? username : "anonymous",
                        connectionInfo.getDurationMs(),
                        activeConnections.size());
                
                // Log disconnection event for monitoring
                logConnectionEvent("DISCONNECT", sessionId, username);
            }
        }
    }

    /**
     * Handle topic subscription
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        String destination = event.getMessage().getHeaders().get("simpDestination", String.class);
        
        if (sessionId != null && destination != null) {
            ConnectionInfo connectionInfo = activeConnections.get(sessionId);
            if (connectionInfo != null) {
                connectionInfo.addSubscription(destination);
                totalSubscriptions.incrementAndGet();
                
                log.debug("Client subscribed - Session: {}, Destination: {}, Total subscriptions: {}", 
                        sessionId, destination, connectionInfo.getSubscriptionCount());
                
                // Special handling for Vietnamese topics
                if (isVietnameseTopicDestination(destination)) {
                    log.debug("Vietnamese topic subscription: {}", destination);
                }
            }
        }
    }

    /**
     * Handle topic unsubscription
     */
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        String subscriptionId = event.getMessage().getHeaders().get("simpSubscriptionId", String.class);
        
        if (sessionId != null) {
            ConnectionInfo connectionInfo = activeConnections.get(sessionId);
            if (connectionInfo != null) {
                boolean removed = connectionInfo.removeSubscription(subscriptionId);
                if (removed) {
                    totalSubscriptions.decrementAndGet();
                    
                    log.debug("Client unsubscribed - Session: {}, Subscription: {}, Remaining: {}", 
                            sessionId, subscriptionId, connectionInfo.getSubscriptionCount());
                }
            }
        }
    }

    /**
     * Record message activity
     */
    public void recordMessageReceived(String sessionId) {
        totalMessagesReceived.incrementAndGet();
        ConnectionInfo connectionInfo = activeConnections.get(sessionId);
        if (connectionInfo != null) {
            connectionInfo.incrementMessagesReceived();
            connectionInfo.updateActivity();
        }
    }

    public void recordMessageSent(String sessionId) {
        totalMessagesSent.incrementAndGet();
        ConnectionInfo connectionInfo = activeConnections.get(sessionId);
        if (connectionInfo != null) {
            connectionInfo.incrementMessagesSent();
        }
    }

    /**
     * Get connection metrics
     */
    public ConnectionMetrics getConnectionMetrics() {
        return new ConnectionMetrics(
            activeConnections.size(),
            totalConnections.get(),
            totalMessagesReceived.get(),
            totalMessagesSent.get(),
            totalSubscriptions.get(),
            LocalDateTime.now()
        );
    }

    /**
     * Get active connections count
     */
    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }

    /**
     * Get user sessions
     */
    public int getUserSessionsCount() {
        return sessionToUser.size();
    }

    /**
     * Check if user is connected
     */
    public boolean isUserConnected(String username) {
        return sessionToUser.containsValue(username);
    }

    /**
     * Get sessions for user
     */
    public java.util.Set<String> getSessionsForUser(String username) {
        return sessionToUser.entrySet().stream()
                .filter(entry -> username.equals(entry.getValue()))
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Log connection event for monitoring
     */
    private void logConnectionEvent(String eventType, String sessionId, String username) {
        log.info("WebSocket connection event: {} - Session: {}, User: {}, Total connections: {}",
                eventType, sessionId, username != null ? username : "anonymous", activeConnections.size());
    }

    /**
     * Check if destination is a Vietnamese topic
     */
    private boolean isVietnameseTopicDestination(String destination) {
        return destination.contains("/gia-san-pham/") ||
               destination.contains("/phieu-giam-gia/") ||
               destination.contains("/dot-giam-gia/") ||
               destination.contains("/chatbox/");
    }

    /**
     * Connection information holder
     */
    public static class ConnectionInfo {
        private final String sessionId;
        private final LocalDateTime connectTime;
        private final String username;
        private final ConcurrentHashMap<String, String> subscriptions = new ConcurrentHashMap<>();
        private final AtomicInteger messagesReceived = new AtomicInteger(0);
        private final AtomicInteger messagesSent = new AtomicInteger(0);
        private volatile LocalDateTime lastActivity;

        public ConnectionInfo(String sessionId, LocalDateTime connectTime, String username) {
            this.sessionId = sessionId;
            this.connectTime = connectTime;
            this.username = username;
            this.lastActivity = connectTime;
        }

        public void addSubscription(String destination) {
            subscriptions.put(java.util.UUID.randomUUID().toString(), destination);
        }

        public boolean removeSubscription(String subscriptionId) {
            return subscriptions.remove(subscriptionId) != null;
        }

        public int getSubscriptionCount() {
            return subscriptions.size();
        }

        public void incrementMessagesReceived() {
            messagesReceived.incrementAndGet();
        }

        public void incrementMessagesSent() {
            messagesSent.incrementAndGet();
        }

        public void updateActivity() {
            lastActivity = LocalDateTime.now();
        }

        public long getDurationMs() {
            return java.time.Duration.between(connectTime, LocalDateTime.now()).toMillis();
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public LocalDateTime getConnectTime() { return connectTime; }
        public String getUsername() { return username; }
        public int getMessagesReceived() { return messagesReceived.get(); }
        public int getMessagesSent() { return messagesSent.get(); }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    /**
     * Connection metrics holder
     */
    public static class ConnectionMetrics {
        private final int activeConnections;
        private final int totalConnections;
        private final long totalMessagesReceived;
        private final long totalMessagesSent;
        private final int totalSubscriptions;
        private final LocalDateTime timestamp;

        public ConnectionMetrics(int activeConnections, int totalConnections, 
                               long totalMessagesReceived, long totalMessagesSent,
                               int totalSubscriptions, LocalDateTime timestamp) {
            this.activeConnections = activeConnections;
            this.totalConnections = totalConnections;
            this.totalMessagesReceived = totalMessagesReceived;
            this.totalMessagesSent = totalMessagesSent;
            this.totalSubscriptions = totalSubscriptions;
            this.timestamp = timestamp;
        }

        // Getters
        public int getActiveConnections() { return activeConnections; }
        public int getTotalConnections() { return totalConnections; }
        public long getTotalMessagesReceived() { return totalMessagesReceived; }
        public long getTotalMessagesSent() { return totalMessagesSent; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * Connection event for monitoring
     */
    public static class ConnectionEvent {
        private final String eventType;
        private final String sessionId;
        private final String username;
        private final LocalDateTime timestamp;
        private final int activeConnections;

        public ConnectionEvent(String eventType, String sessionId, String username, 
                             LocalDateTime timestamp, int activeConnections) {
            this.eventType = eventType;
            this.sessionId = sessionId;
            this.username = username;
            this.timestamp = timestamp;
            this.activeConnections = activeConnections;
        }

        // Getters
        public String getEventType() { return eventType; }
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getActiveConnections() { return activeConnections; }
    }
}
