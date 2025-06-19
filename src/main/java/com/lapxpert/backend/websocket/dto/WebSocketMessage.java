package com.lapxpert.backend.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket Message DTO for Redis Pub/Sub Communication with Enhanced Ordering and Delivery
 *
 * Data transfer object for messages sent between services via Redis Pub/Sub
 * and forwarded to WebSocket clients via STOMP destinations.
 *
 * Enhanced Features:
 * - Message sequencing with unique IDs and sequence numbers
 * - Delivery confirmation mechanisms
 * - Message deduplication support
 * - Ordering guarantees for reliable message delivery
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

    // Static sequence generator for message ordering
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);

    /**
     * Unique message identifier for deduplication and tracking
     * Generated automatically for each message
     */
    private String messageId;

    /**
     * Sequence number for message ordering within the same channel
     * Ensures proper ordering of messages in Redis Pub/Sub
     */
    private Long sequenceNumber;

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
     * Cache invalidation metadata for frontend coordination
     * Vietnamese Business Context: Metadata vô hiệu hóa cache cho điều phối frontend
     */
    private Map<String, Object> cacheInvalidation;

    /**
     * Delivery confirmation settings for reliable messaging
     * Vietnamese Business Context: Cài đặt xác nhận giao hàng cho tin nhắn đáng tin cậy
     */
    private DeliveryConfirmation deliveryConfirmation;

    /**
     * Message retry configuration for failed deliveries
     */
    private RetryConfiguration retryConfiguration;

    /**
     * Message ordering group for maintaining sequence within related messages
     * Messages with the same ordering group will be processed in sequence
     */
    private String orderingGroup;

    /**
     * Convenience constructor for simple topic messages
     */
    public WebSocketMessage(String destination, Object payload) {
        this.destination = destination;
        this.payload = payload;
        this.timestamp = Instant.now();
        this.messageId = UUID.randomUUID().toString();
        this.sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
    }

    /**
     * Convenience constructor for typed messages
     */
    public WebSocketMessage(String destination, Object payload, String messageType) {
        this.destination = destination;
        this.payload = payload;
        this.messageType = messageType;
        this.timestamp = Instant.now();
        this.messageId = UUID.randomUUID().toString();
        this.sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
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
        this.messageId = UUID.randomUUID().toString();
        this.sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
    }

    /**
     * Builder pattern for complex message construction
     */
    public static WebSocketMessageBuilder builder() {
        return new WebSocketMessageBuilder();
    }

    /**
     * Add cache invalidation metadata to the message
     * Vietnamese Business Context: Thêm metadata vô hiệu hóa cache vào tin nhắn
     */
    public WebSocketMessage withCacheInvalidation(Map<String, Object> cacheMetadata) {
        this.cacheInvalidation = cacheMetadata;
        return this;
    }

    /**
     * Add cache invalidation scope for frontend coordination
     */
    public WebSocketMessage withCacheInvalidationScope(String scope, String version) {
        if (this.cacheInvalidation == null) {
            this.cacheInvalidation = new HashMap<>();
        }
        this.cacheInvalidation.put("scope", scope);
        this.cacheInvalidation.put("version", version);
        this.cacheInvalidation.put("requiresRefresh", true);
        this.cacheInvalidation.put("timestamp", Instant.now());
        return this;
    }

    /**
     * Check if message contains cache invalidation metadata
     */
    public boolean hasCacheInvalidation() {
        return this.cacheInvalidation != null && !this.cacheInvalidation.isEmpty();
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
            if (message.messageId == null) {
                message.messageId = UUID.randomUUID().toString();
            }
            if (message.sequenceNumber == null) {
                message.sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
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

    /**
     * Check if message requires delivery confirmation
     */
    public boolean requiresDeliveryConfirmation() {
        return deliveryConfirmation != null && deliveryConfirmation.isEnabled();
    }

    /**
     * Check if message supports retry on failure
     */
    public boolean supportsRetry() {
        return retryConfiguration != null && retryConfiguration.getMaxRetries() > 0;
    }

    /**
     * Get message age in milliseconds
     */
    public long getMessageAge() {
        return Instant.now().toEpochMilli() - timestamp.toEpochMilli();
    }

    /**
     * Check if message has expired based on TTL
     */
    public boolean isExpired() {
        if (retryConfiguration == null || retryConfiguration.getTtlMillis() <= 0) {
            return false;
        }
        return getMessageAge() > retryConfiguration.getTtlMillis();
    }

    /**
     * Delivery confirmation configuration for reliable messaging
     * Vietnamese Business Context: Cấu hình xác nhận giao hàng cho tin nhắn đáng tin cậy
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryConfirmation {
        /**
         * Whether delivery confirmation is enabled
         */
        private boolean enabled = false;

        /**
         * Timeout for delivery confirmation in milliseconds
         */
        private long timeoutMillis = 30000; // 30 seconds default

        /**
         * Callback destination for delivery acknowledgments
         */
        private String ackDestination;

        /**
         * Whether to require explicit acknowledgment from client
         */
        private boolean requireClientAck = false;

        /**
         * Maximum time to wait for acknowledgment
         */
        private long maxWaitMillis = 60000; // 1 minute default
    }

    /**
     * Retry configuration for failed message deliveries
     * Vietnamese Business Context: Cấu hình thử lại cho việc giao tin nhắn thất bại
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RetryConfiguration {
        /**
         * Maximum number of retry attempts
         */
        private int maxRetries = 3;

        /**
         * Initial retry delay in milliseconds
         */
        private long initialDelayMillis = 1000; // 1 second

        /**
         * Retry delay multiplier for exponential backoff
         */
        private double backoffMultiplier = 2.0;

        /**
         * Maximum retry delay in milliseconds
         */
        private long maxDelayMillis = 30000; // 30 seconds

        /**
         * Message time-to-live in milliseconds
         */
        private long ttlMillis = 300000; // 5 minutes default

        /**
         * Whether to use exponential backoff
         */
        private boolean useExponentialBackoff = true;
    }
}
