package com.lapxpert.backend.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.websocket.config.RedisPubSubConfig;
import com.lapxpert.backend.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced Message Routing Service
 *
 * Provides enhanced message routing capabilities for the monolithic application's
 * WebSocket functionality. Routes messages to Redis Pub/Sub channels for improved
 * message broadcasting within the same application instance.
 *
 * Routing Logic:
 * - Price updates: Redis price channel for enhanced broadcasting
 * - Voucher notifications: Redis voucher channel for enhanced broadcasting
 * - Chatbox messages: Redis chatbox channel for future features
 * - General messages: Redis global channel for enhanced broadcasting
 *
 * Can be enabled/disabled via: websocket.enhanced.enabled=true/false
 */
@Service
@ConditionalOnProperty(name = "websocket.enhanced.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MessageRoutingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Metrics
    private final AtomicLong totalMessagesRouted = new AtomicLong(0);
    private final AtomicLong redisPublishCount = new AtomicLong(0);
    private final AtomicLong routingErrors = new AtomicLong(0);
    private final AtomicLong queuedMessages = new AtomicLong(0);
    private final AtomicLong transactionRollbacks = new AtomicLong(0);

    /**
     * Transaction-aware message queue for coordinating message delivery with database transactions
     * Vietnamese Business Context: Hàng đợi tin nhắn nhận biết giao dịch để điều phối gửi tin nhắn với giao dịch cơ sở dữ liệu
     */
    private static final ThreadLocal<List<QueuedMessage>> transactionMessageQueue = new ThreadLocal<>();

    /**
     * Queued message for transaction coordination
     */
    private static class QueuedMessage {
        final String channel;
        final WebSocketMessage message;
        final Instant queuedAt;

        QueuedMessage(String channel, WebSocketMessage message) {
            this.channel = channel;
            this.message = message;
            this.queuedAt = Instant.now();
        }
    }

    /**
     * Route message to appropriate channels based on destination
     */
    public void routeMessage(String destination, Object payload) {
        routeMessage(destination, payload, null, null);
    }

    /**
     * Route message with message type
     */
    public void routeMessage(String destination, Object payload, String messageType) {
        routeMessage(destination, payload, messageType, null);
    }

    /**
     * Route message with full context
     */
    public void routeMessage(String destination, Object payload, String messageType, String sourceService) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                    .destination(destination)
                    .payload(payload)
                    .messageType(messageType)
                    .sourceService(sourceService)
                    .timestamp(Instant.now())
                    .build();

            routeWebSocketMessage(message);
            totalMessagesRouted.incrementAndGet();

        } catch (Exception e) {
            routingErrors.incrementAndGet();
            log.error("Error routing message to destination {}: {}", destination, e.getMessage(), e);
        }
    }

    /**
     * Route user-specific message
     */
    public void routeUserMessage(String username, String destination, Object payload, String messageType) {
        try {
            WebSocketMessage message = WebSocketMessage.builder()
                    .destination("/user/" + username + destination)
                    .payload(payload)
                    .messageType(messageType)
                    .targetUser(username)
                    .timestamp(Instant.now())
                    .build();

            routeWebSocketMessage(message);
            totalMessagesRouted.incrementAndGet();

        } catch (Exception e) {
            routingErrors.incrementAndGet();
            log.error("Error routing user message to {}: {}", username, e.getMessage(), e);
        }
    }

    /**
     * Route WebSocket message to appropriate Redis channel and local STOMP
     */
    private void routeWebSocketMessage(WebSocketMessage message) {
        String destination = message.getDestination();
        String redisChannel = determineRedisChannel(destination);

        // Publish to Redis for enhanced message broadcasting
        publishToRedis(redisChannel, message);
    }

    /**
     * Determine appropriate Redis channel based on destination
     */
    private String determineRedisChannel(String destination) {
        if (isPriceDestination(destination)) {
            return RedisPubSubConfig.PRICE_CHANNEL;
        } else if (isVoucherDestination(destination)) {
            return RedisPubSubConfig.VOUCHER_CHANNEL;
        } else if (isHealthDestination(destination)) {
            return RedisPubSubConfig.HEALTH_CHANNEL;
        } else if (isChatboxDestination(destination)) {
            return RedisPubSubConfig.CHATBOX_CHANNEL;
        } else {
            return RedisPubSubConfig.GLOBAL_CHANNEL;
        }
    }

    /**
     * Publish message to Redis Pub/Sub channel with transaction coordination
     * Vietnamese Business Context: Xuất bản tin nhắn đến kênh Redis Pub/Sub với điều phối giao dịch
     */
    private void publishToRedis(String channel, WebSocketMessage message) {
        // Check if we're in a transaction context
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Queue message for after-commit delivery
            queueMessageForTransaction(channel, message);
        } else {
            // Send immediately if not in transaction
            publishMessageImmediately(channel, message);
        }
    }

    /**
     * Queue message for transaction-aware delivery
     * Vietnamese Business Context: Xếp hàng tin nhắn để gửi nhận biết giao dịch
     */
    private void queueMessageForTransaction(String channel, WebSocketMessage message) {
        try {
            // Initialize queue for this transaction if needed
            List<QueuedMessage> queue = transactionMessageQueue.get();
            if (queue == null) {
                queue = new ArrayList<>();
                transactionMessageQueue.set(queue);

                // Register transaction synchronization for message delivery
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deliverQueuedMessages();
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            handleTransactionRollback();
                        }
                        // Clean up thread local
                        transactionMessageQueue.remove();
                    }
                });
            }

            // Add message to queue
            queue.add(new QueuedMessage(channel, message));
            queuedMessages.incrementAndGet();

            log.debug("Queued message for after-commit delivery: destination={}", message.getDestination());

        } catch (Exception e) {
            routingErrors.incrementAndGet();
            log.error("Failed to queue message for transaction: destination={}, error={}",
                message.getDestination(), e.getMessage(), e);
            // Fallback to immediate delivery
            publishMessageImmediately(channel, message);
        }
    }

    /**
     * Publish message immediately without transaction coordination
     */
    private void publishMessageImmediately(String channel, WebSocketMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, messageJson);
            redisPublishCount.incrementAndGet();

            log.debug("Published message immediately to Redis channel {}: destination={}",
                    channel, message.getDestination());

        } catch (JsonProcessingException e) {
            routingErrors.incrementAndGet();
            log.error("Error serializing message for Redis: {}", e.getMessage(), e);
        } catch (Exception e) {
            routingErrors.incrementAndGet();
            log.error("Error publishing to Redis channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Deliver all queued messages after transaction commit
     * Vietnamese Business Context: Gửi tất cả tin nhắn đã xếp hàng sau khi giao dịch commit
     */
    private void deliverQueuedMessages() {
        List<QueuedMessage> queue = transactionMessageQueue.get();
        if (queue == null || queue.isEmpty()) {
            return;
        }

        try {
            log.debug("Delivering {} queued messages after transaction commit", queue.size());

            for (QueuedMessage queuedMessage : queue) {
                try {
                    publishMessageImmediately(queuedMessage.channel, queuedMessage.message);

                    log.debug("Delivered queued message: destination={}, queued_at={}",
                        queuedMessage.message.getDestination(), queuedMessage.queuedAt);

                } catch (Exception e) {
                    routingErrors.incrementAndGet();
                    log.error("Failed to deliver queued message: destination={}, error={}",
                        queuedMessage.message.getDestination(), e.getMessage(), e);
                }
            }

            log.info("Successfully delivered {} messages after transaction commit", queue.size());

        } catch (Exception e) {
            routingErrors.incrementAndGet();
            log.error("Error delivering queued messages: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle transaction rollback by discarding queued messages
     * Vietnamese Business Context: Xử lý rollback giao dịch bằng cách loại bỏ tin nhắn đã xếp hàng
     */
    private void handleTransactionRollback() {
        List<QueuedMessage> queue = transactionMessageQueue.get();
        if (queue != null && !queue.isEmpty()) {
            transactionRollbacks.incrementAndGet();
            log.warn("Transaction rolled back - discarding {} queued messages", queue.size());

            // Log discarded messages for debugging
            for (QueuedMessage message : queue) {
                log.debug("Discarded message due to rollback: destination={}, queued_at={}",
                    message.message.getDestination(), message.queuedAt);
            }
        }
    }

    // Note: Local broadcasting is handled by the existing WebSocket configuration
    // This service focuses on Redis Pub/Sub message publishing for enhanced capabilities

    /**
     * Broadcast price update with Vietnamese topic structure
     */
    public void broadcastPriceUpdate(String variantId, Object priceData) {
        String destination = "/topic/gia-san-pham/" + variantId;
        routeMessage(destination, priceData, "PRICE_UPDATE", "WEBSOCKET_SERVICE");
        
        // Also broadcast to general price topic for dashboard monitoring
        routeMessage("/topic/gia-san-pham/all", priceData, "PRICE_UPDATE", "WEBSOCKET_SERVICE");
    }

    /**
     * Broadcast voucher notification with Vietnamese topic structure
     */
    public void broadcastVoucherNotification(String voucherId, String voucherType, Object voucherData) {
        String topicPrefix = "PHIEU_GIAM_GIA".equals(voucherType) ? "phieu-giam-gia" : "dot-giam-gia";
        String destination = "/topic/" + topicPrefix + "/" + voucherId;
        
        routeMessage(destination, voucherData, "VOUCHER_NOTIFICATION", "WEBSOCKET_SERVICE");
        
        // Also broadcast to general voucher topic for dashboard monitoring
        routeMessage("/topic/voucher/all", voucherData, "VOUCHER_NOTIFICATION", "WEBSOCKET_SERVICE");
    }

    /**
     * Broadcast health status
     */
    public void broadcastHealthStatus(Object healthData) {
        routeMessage("/topic/websocket/health", healthData, "HEALTH_STATUS", "WEBSOCKET_SERVICE");
    }

    /**
     * Broadcast chatbox message (future feature)
     */
    public void broadcastChatboxMessage(String sessionId, Object chatData) {
        String destination = "/topic/chatbox/" + sessionId;
        routeMessage(destination, chatData, "CHAT_MESSAGE", "WEBSOCKET_SERVICE");
    }

    /**
     * Send private chatbox message to user (future feature)
     */
    public void sendPrivateChatMessage(String username, Object chatData) {
        routeUserMessage(username, "/queue/chat", chatData, "PRIVATE_CHAT");
    }

    /**
     * Get routing metrics
     */
    public RoutingMetrics getRoutingMetrics() {
        return new RoutingMetrics(
                totalMessagesRouted.get(),
                redisPublishCount.get(),
                routingErrors.get(),
                Instant.now()
        );
    }

    /**
     * Destination validation methods
     */
    private boolean isPriceDestination(String destination) {
        return destination.startsWith("/topic/gia-san-pham/");
    }

    private boolean isVoucherDestination(String destination) {
        return destination.startsWith("/topic/phieu-giam-gia/") ||
               destination.startsWith("/topic/dot-giam-gia/") ||
               destination.equals("/topic/voucher/all");
    }

    private boolean isHealthDestination(String destination) {
        return destination.startsWith("/topic/websocket/health") ||
               destination.startsWith("/topic/health/");
    }

    private boolean isChatboxDestination(String destination) {
        return destination.startsWith("/topic/chatbox/") ||
               (destination.startsWith("/user/") && destination.contains("/queue/chat"));
    }

    /**
     * Routing metrics holder
     */
    public static class RoutingMetrics {
        private final long totalMessagesRouted;
        private final long redisPublishCount;
        private final long routingErrors;
        private final Instant timestamp;

        public RoutingMetrics(long totalMessagesRouted, long redisPublishCount,
                            long routingErrors, Instant timestamp) {
            this.totalMessagesRouted = totalMessagesRouted;
            this.redisPublishCount = redisPublishCount;
            this.routingErrors = routingErrors;
            this.timestamp = timestamp;
        }

        // Getters
        public long getTotalMessagesRouted() { return totalMessagesRouted; }
        public long getRedisPublishCount() { return redisPublishCount; }
        public long getRoutingErrors() { return routingErrors; }
        public Instant getTimestamp() { return timestamp; }
    }
}
