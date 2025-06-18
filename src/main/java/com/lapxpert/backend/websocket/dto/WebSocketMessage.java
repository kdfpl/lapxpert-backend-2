package com.lapxpert.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * WebSocket Message DTO for Redis Pub/Sub Communication
 * 
 * Data transfer object for messages sent between services via Redis Pub/Sub
 * and forwarded to WebSocket clients via STOMP destinations.
 * 
 * Supports Vietnamese topic naming conventions and various message types:
 * - Product price updates (gia-san-pham)
 * - Voucher notifications (phieu-giam-gia, dot-giam-gia)
 * - Health monitoring (websocket/health)
 * - Future chatbox messages (chatbox)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    /**
     * Target STOMP destination for the message
     * Examples:
     * - /topic/gia-san-pham/12345 (product price update)
     * - /topic/phieu-giam-gia/67890 (voucher notification)
     * - /topic/websocket/health (health status)
     * - /user/username/queue/chat (private chat message)
     */
    private String destination;

    /**
     * Message payload - can be any serializable object
     * Will be sent as-is to WebSocket clients
     */
    private Object payload;

    /**
     * Message type for categorization and routing
     * Examples: PRICE_UPDATE, VOUCHER_NOTIFICATION, HEALTH_STATUS, CHAT_MESSAGE
     */
    private String messageType;

    /**
     * Source service that originated the message
     * Examples: MAIN_APP, INVENTORY_SERVICE, PAYMENT_SERVICE
     */
    private String sourceService;

    /**
     * Target user for user-specific messages (optional)
     * Used for private messages and user-specific notifications
     */
    private String targetUser;

    /**
     * Message timestamp for tracking and debugging (UTC timezone)
     * Stored as Instant for better distributed system compatibility
     */
    private Instant timestamp;

    /**
     * Additional metadata for message processing
     */
    private Map<String, Object> metadata;

    /**
     * Message priority for future queue management
     * HIGH, MEDIUM, LOW
     */
    private String priority;

    /**
     * Convenience constructor for simple topic messages
     */
    public WebSocketMessage(String destination, Object payload) {
        this.destination = destination;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    /**
     * Convenience constructor for typed messages
     */
    public WebSocketMessage(String destination, Object payload, String messageType) {
        this.destination = destination;
        this.payload = payload;
        this.messageType = messageType;
        this.timestamp = Instant.now();
    }

    /**
     * Convenience constructor for user-specific messages
     */
    public WebSocketMessage(String destination, Object payload, String messageType, String targetUser) {
        this.destination = destination;
        this.payload = payload;
        this.messageType = messageType;
        this.targetUser = targetUser;
        this.timestamp = Instant.now();
    }

    /**
     * Builder pattern for complex message construction
     */
    public static WebSocketMessageBuilder builder() {
        return new WebSocketMessageBuilder();
    }

    public static class WebSocketMessageBuilder {
        private WebSocketMessage message = new WebSocketMessage();

        public WebSocketMessageBuilder destination(String destination) {
            message.destination = destination;
            return this;
        }

        public WebSocketMessageBuilder payload(Object payload) {
            message.payload = payload;
            return this;
        }

        public WebSocketMessageBuilder messageType(String messageType) {
            message.messageType = messageType;
            return this;
        }

        public WebSocketMessageBuilder sourceService(String sourceService) {
            message.sourceService = sourceService;
            return this;
        }

        public WebSocketMessageBuilder targetUser(String targetUser) {
            message.targetUser = targetUser;
            return this;
        }

        public WebSocketMessageBuilder timestamp(Instant timestamp) {
            message.timestamp = timestamp;
            return this;
        }

        public WebSocketMessageBuilder metadata(Map<String, Object> metadata) {
            message.metadata = metadata;
            return this;
        }

        public WebSocketMessageBuilder priority(String priority) {
            message.priority = priority;
            return this;
        }

        public WebSocketMessage build() {
            if (message.timestamp == null) {
                message.timestamp = Instant.now();
            }
            return message;
        }
    }

    /**
     * Check if this is a user-specific message
     */
    public boolean isUserSpecific() {
        return targetUser != null && !targetUser.trim().isEmpty();
    }

    /**
     * Check if this is a high priority message
     */
    public boolean isHighPriority() {
        return "HIGH".equalsIgnoreCase(priority);
    }

    /**
     * Get formatted destination for logging
     */
    public String getFormattedDestination() {
        if (isUserSpecific()) {
            return String.format("%s (user: %s)", destination, targetUser);
        }
        return destination;
    }
}
