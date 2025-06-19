package com.lapxpert.backend.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket Integration Service
 * 
 * Provides integration between the main LapXpert application and the dedicated WebSocket service.
 * Publishes messages to Redis Pub/Sub channels that the WebSocket service subscribes to,
 * enabling real-time notifications to be sent to connected clients.
 * 
 * This service acts as the bridge for the main application to send real-time updates
 * without directly managing WebSocket connections, supporting horizontal scaling
 * and separation of concerns.
 * 
 * Usage:
 * - Inject this service in controllers/services that need to send real-time updates
 * - Call appropriate methods to publish price updates, voucher notifications, etc.
 * - Messages are automatically routed to the WebSocket service via Redis Pub/Sub
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketIntegrationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${websocket.integration.enabled:true}")
    private boolean integrationEnabled;

    // Redis channel constants (must match WebSocket service configuration)
    private static final String GLOBAL_CHANNEL = "lapxpert:websocket:global";
    private static final String PRICE_CHANNEL = "lapxpert:websocket:price";
    private static final String VOUCHER_CHANNEL = "lapxpert:websocket:voucher";
    private static final String HEALTH_CHANNEL = "lapxpert:websocket:health";
    private static final String CHATBOX_CHANNEL = "lapxpert:websocket:chatbox";

    // Metrics
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong sendErrors = new AtomicLong(0);
    private final AtomicLong queuedMessages = new AtomicLong(0);
    private final AtomicLong transactionRollbacks = new AtomicLong(0);

    /**
     * Transaction-aware message queue for coordinating WebSocket delivery with database transactions
     * Vietnamese Business Context: Hàng đợi tin nhắn nhận biết giao dịch để điều phối gửi WebSocket với giao dịch cơ sở dữ liệu
     */
    private static final ThreadLocal<List<QueuedWebSocketMessage>> transactionMessageQueue = new ThreadLocal<>();

    /**
     * Queued WebSocket message for transaction coordination
     */
    private static class QueuedWebSocketMessage {
        final String channel;
        final String destination;
        final Object payload;
        final String messageType;
        final Instant queuedAt;

        QueuedWebSocketMessage(String channel, String destination, Object payload, String messageType) {
            this.channel = channel;
            this.destination = destination;
            this.payload = payload;
            this.messageType = messageType;
            this.queuedAt = Instant.now();
        }
    }

    /**
     * Send price update notification
     * Vietnamese topic: /topic/gia-san-pham/{variantId}
     */
    public void sendPriceUpdate(String variantId, Double newPrice, String message) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping price update");
            return;
        }

        try {
            Map<String, Object> priceData = new HashMap<>();
            priceData.put("variantId", variantId);
            priceData.put("newPrice", newPrice);
            priceData.put("message", message != null ? message : "Giá sản phẩm đã được cập nhật");
            priceData.put("timestamp", Instant.now());

            String destination = "/topic/gia-san-pham/" + variantId;
            publishMessage(PRICE_CHANNEL, destination, priceData, "PRICE_UPDATE");

            // Also send to general price monitoring topic
            publishMessage(PRICE_CHANNEL, "/topic/gia-san-pham/all", priceData, "PRICE_UPDATE");

            log.debug("Sent price update for variant {}: {}", variantId, newPrice);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send price update for variant {}: {}", variantId, e.getMessage(), e);
        }
    }

    /**
     * Send voucher notification
     * Vietnamese topics: /topic/phieu-giam-gia/{voucherId} or /topic/dot-giam-gia/{campaignId}
     */
    public void sendVoucherNotification(String voucherId, String voucherType, String message, Object voucherData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping voucher notification");
            return;
        }

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("voucherId", voucherId);
            notification.put("voucherType", voucherType);
            notification.put("message", message);
            notification.put("timestamp", Instant.now());
            
            if (voucherData != null) {
                notification.put("data", voucherData);
            }

            String topicPrefix = "PHIEU_GIAM_GIA".equals(voucherType) ? "phieu-giam-gia" : "dot-giam-gia";
            String destination = "/topic/" + topicPrefix + "/" + voucherId;
            
            publishMessage(VOUCHER_CHANNEL, destination, notification, "VOUCHER_NOTIFICATION");

            // Also send to general voucher monitoring topic
            publishMessage(VOUCHER_CHANNEL, "/topic/voucher/all", notification, "VOUCHER_NOTIFICATION");

            log.debug("Sent voucher notification for {} {}: {}", voucherType, voucherId, message);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send voucher notification for {} {}: {}", voucherType, voucherId, e.getMessage(), e);
        }
    }

    /**
     * Send inventory update notification
     * Vietnamese topic: /topic/ton-kho/{productId}
     */
    public void sendInventoryUpdate(String productId, Object inventoryData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping inventory update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("productId", productId);
            update.put("data", inventoryData);
            update.put("timestamp", Instant.now());

            String destination = "/topic/ton-kho/" + productId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "INVENTORY_UPDATE");

            // Also send to general inventory monitoring topic
            publishMessage(GLOBAL_CHANNEL, "/topic/ton-kho/updates", update, "INVENTORY_UPDATE");

            log.debug("Sent inventory update for product {}", productId);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send inventory update for product {}: {}", productId, e.getMessage(), e);
        }
    }

    /**
     * Send order status update notification
     * Vietnamese topic: /topic/hoa-don/{orderId}
     */
    public void sendOrderUpdate(String orderId, String status, Object orderData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping order update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("orderId", orderId);
            update.put("status", status);
            update.put("data", orderData);
            update.put("timestamp", Instant.now());

            String destination = "/topic/hoa-don/" + orderId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "ORDER_UPDATE");

            // Also send to general order monitoring topics based on action type
            if ("CREATED".equals(status)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/hoa-don/new", update, "ORDER_CREATED");
            } else if ("UPDATED".equals(status)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/hoa-don/updated", update, "ORDER_UPDATED");
            } else {
                publishMessage(GLOBAL_CHANNEL, "/topic/hoa-don/status-changed", update, "ORDER_STATUS_CHANGED");
            }

            log.debug("Sent order update for order {}: {}", orderId, status);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send order update for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Send system notification
     */
    public void sendSystemNotification(String message, String level) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping system notification");
            return;
        }

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("message", message);
            notification.put("level", level != null ? level : "INFO");
            notification.put("timestamp", Instant.now());

            String destination = "/topic/system/notifications";
            publishMessage(GLOBAL_CHANNEL, destination, notification, "SYSTEM_NOTIFICATION");

            log.debug("Sent system notification: {} ({})", message, level);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send system notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send health status update
     */
    public void sendHealthStatus(String serviceName, Object healthData) {
        if (!integrationEnabled) {
            return; // Don't log for health updates to avoid spam
        }

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("serviceName", serviceName);
            status.put("data", healthData);
            status.put("timestamp", Instant.now());

            String destination = "/topic/health/" + serviceName;
            publishMessage(HEALTH_CHANNEL, destination, status, "HEALTH_STATUS");

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send health status for service {}: {}", serviceName, e.getMessage(), e);
        }
    }

    /**
     * Send product update notification
     * Vietnamese topic: /topic/san-pham/{productId}
     */
    public void sendProductUpdate(String productId, String action, Object productData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping product update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("productId", productId);
            update.put("action", action);
            update.put("data", productData);
            update.put("timestamp", Instant.now());

            String destination = "/topic/san-pham/" + productId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "PRODUCT_UPDATE");

            // Also send to general product monitoring topics based on action type
            if ("CREATED".equals(action)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/san-pham/new", update, "PRODUCT_CREATED");
            } else if ("UPDATED".equals(action)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/san-pham/updated", update, "PRODUCT_UPDATED");
            }

            log.debug("Sent product update for product {}: {}", productId, action);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send product update for product {}: {}", productId, e.getMessage(), e);
        }
    }

    /**
     * Send user update notification
     * Vietnamese topic: /topic/nguoi-dung/{userId}
     */
    public void sendUserUpdate(String userId, String action, Object userData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping user update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("userId", userId);
            update.put("action", action);
            update.put("data", userData);
            update.put("timestamp", Instant.now());

            String destination = "/topic/nguoi-dung/" + userId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "USER_UPDATE");

            // Also send to general user monitoring topics based on action type
            if ("CREATED".equals(action)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/nguoi-dung/new", update, "USER_CREATED");
            } else if ("UPDATED".equals(action)) {
                publishMessage(GLOBAL_CHANNEL, "/topic/nguoi-dung/updated", update, "USER_UPDATED");
            }

            log.debug("Sent user update for user {}: {}", userId, action);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send user update for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send discount campaign update notification
     * Vietnamese topic: /topic/dot-giam-gia/{campaignId}
     */
    public void sendDiscountCampaignUpdate(String campaignId, String action, Object campaignData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping discount campaign update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("campaignId", campaignId);
            update.put("action", action);
            update.put("data", campaignData);
            update.put("timestamp", Instant.now());

            String destination = "/topic/dot-giam-gia/" + campaignId;
            publishMessage(VOUCHER_CHANNEL, destination, update, "DISCOUNT_CAMPAIGN_UPDATE");

            // Also send to general discount campaign monitoring topics based on action type
            if ("CREATED".equals(action)) {
                publishMessage(VOUCHER_CHANNEL, "/topic/dot-giam-gia/new", update, "DISCOUNT_CAMPAIGN_CREATED");
            } else if ("UPDATED".equals(action)) {
                publishMessage(VOUCHER_CHANNEL, "/topic/dot-giam-gia/updated", update, "DISCOUNT_CAMPAIGN_UPDATED");
            } else if ("STATUS_CHANGED".equals(action)) {
                publishMessage(VOUCHER_CHANNEL, "/topic/dot-giam-gia/status-changed", update, "DISCOUNT_CAMPAIGN_STATUS_CHANGED");
            }

            log.debug("Sent discount campaign update for campaign {}: {}", campaignId, action);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send discount campaign update for campaign {}: {}", campaignId, e.getMessage(), e);
        }
    }

    /**
     * Send statistics update notification
     * Vietnamese topic: /topic/thong-ke/updated
     */
    public void sendStatisticsUpdate(Object statsData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping statistics update");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("data", statsData);
            update.put("timestamp", Instant.now());

            publishMessage(GLOBAL_CHANNEL, "/topic/thong-ke/updated", update, "STATISTICS_UPDATE");

            log.debug("Sent statistics update");

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send statistics update: {}", e.getMessage(), e);
        }
    }

    /**
     * Send dashboard refresh notification
     * Vietnamese topic: /topic/dashboard/refresh
     */
    public void sendDashboardRefresh(Object dashboardData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping dashboard refresh");
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("data", dashboardData);
            update.put("timestamp", Instant.now());

            publishMessage(GLOBAL_CHANNEL, "/topic/dashboard/refresh", update, "DASHBOARD_REFRESH");

            log.debug("Sent dashboard refresh");

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send dashboard refresh: {}", e.getMessage(), e);
        }
    }

    /**
     * Send low stock alert notification
     * Vietnamese topic: /topic/ton-kho/low-stock
     */
    public void sendLowStockAlert(String productId, Object alertData) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping low stock alert");
            return;
        }

        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("productId", productId);
            alert.put("data", alertData);
            alert.put("timestamp", Instant.now());

            publishMessage(GLOBAL_CHANNEL, "/topic/ton-kho/low-stock", alert, "LOW_STOCK_ALERT");

            log.debug("Sent low stock alert for product {}", productId);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send low stock alert for product {}: {}", productId, e.getMessage(), e);
        }
    }

    /**
     * Send custom message to any destination
     */
    public void sendCustomMessage(String destination, Object payload, String messageType) {
        if (!integrationEnabled) {
            log.debug("WebSocket integration disabled - skipping custom message");
            return;
        }

        try {
            String channel = determineChannelByDestination(destination);
            publishMessage(channel, destination, payload, messageType);

            log.debug("Sent custom message to {}: {}", destination, messageType);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send custom message to {}: {}", destination, e.getMessage(), e);
        }
    }

    /**
     * Core method to publish message to Redis Pub/Sub with transaction coordination
     * Vietnamese Business Context: Phương thức cốt lõi để xuất bản tin nhắn đến Redis Pub/Sub với điều phối giao dịch
     */
    private void publishMessage(String channel, String destination, Object payload, String messageType) {
        // Check if we're in a transaction context
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Queue message for after-commit delivery
            queueMessageForTransaction(channel, destination, payload, messageType);
        } else {
            // Send immediately if not in transaction
            sendMessageImmediately(channel, destination, payload, messageType);
        }
    }

    /**
     * Queue message for transaction-aware delivery
     * Vietnamese Business Context: Xếp hàng tin nhắn để gửi nhận biết giao dịch
     */
    private void queueMessageForTransaction(String channel, String destination, Object payload, String messageType) {
        try {
            // Initialize queue for this transaction if needed
            List<QueuedWebSocketMessage> queue = transactionMessageQueue.get();
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
            queue.add(new QueuedWebSocketMessage(channel, destination, payload, messageType));
            queuedMessages.incrementAndGet();

            log.debug("Queued WebSocket message for after-commit delivery: destination={}", destination);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to queue message for transaction: destination={}, error={}", destination, e.getMessage(), e);
            // Fallback to immediate delivery
            sendMessageImmediately(channel, destination, payload, messageType);
        }
    }

    /**
     * Send message immediately without transaction coordination
     */
    private void sendMessageImmediately(String channel, String destination, Object payload, String messageType) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("destination", destination);
            message.put("payload", payload);
            message.put("messageType", messageType);
            message.put("sourceService", "MAIN_APPLICATION");
            message.put("timestamp", Instant.now());

            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, messageJson);
            totalMessagesSent.incrementAndGet();

            log.debug("Published message immediately to Redis channel {}: destination={}", channel, destination);

        } catch (JsonProcessingException e) {
            sendErrors.incrementAndGet();
            log.error("Error serializing message for Redis: {}", e.getMessage(), e);
        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Error publishing message to Redis channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Deliver all queued messages after transaction commit
     * Vietnamese Business Context: Gửi tất cả tin nhắn đã xếp hàng sau khi giao dịch commit
     */
    private void deliverQueuedMessages() {
        List<QueuedWebSocketMessage> queue = transactionMessageQueue.get();
        if (queue == null || queue.isEmpty()) {
            return;
        }

        try {
            log.debug("Delivering {} queued WebSocket messages after transaction commit", queue.size());

            for (QueuedWebSocketMessage queuedMessage : queue) {
                try {
                    sendMessageImmediately(
                        queuedMessage.channel,
                        queuedMessage.destination,
                        queuedMessage.payload,
                        queuedMessage.messageType
                    );

                    log.debug("Delivered queued message: destination={}, queued_at={}",
                        queuedMessage.destination, queuedMessage.queuedAt);

                } catch (Exception e) {
                    sendErrors.incrementAndGet();
                    log.error("Failed to deliver queued message: destination={}, error={}",
                        queuedMessage.destination, e.getMessage(), e);
                }
            }

            log.info("Successfully delivered {} WebSocket messages after transaction commit", queue.size());

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Error delivering queued WebSocket messages: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle transaction rollback by discarding queued messages
     * Vietnamese Business Context: Xử lý rollback giao dịch bằng cách loại bỏ tin nhắn đã xếp hàng
     */
    private void handleTransactionRollback() {
        List<QueuedWebSocketMessage> queue = transactionMessageQueue.get();
        if (queue != null && !queue.isEmpty()) {
            transactionRollbacks.incrementAndGet();
            log.warn("Transaction rolled back - discarding {} queued WebSocket messages", queue.size());

            // Log discarded messages for debugging
            for (QueuedWebSocketMessage message : queue) {
                log.debug("Discarded WebSocket message due to rollback: destination={}, queued_at={}",
                    message.destination, message.queuedAt);
            }
        }
    }

    /**
     * Determine appropriate Redis channel based on destination
     */
    private String determineChannelByDestination(String destination) {
        if (destination.contains("/gia-san-pham/")) {
            return PRICE_CHANNEL;
        } else if (destination.contains("/phieu-giam-gia/") || destination.contains("/dot-giam-gia/") || destination.contains("/voucher/")) {
            return VOUCHER_CHANNEL;
        } else if (destination.contains("/health/")) {
            return HEALTH_CHANNEL;
        } else if (destination.contains("/chatbox/")) {
            return CHATBOX_CHANNEL;
        } else {
            return GLOBAL_CHANNEL;
        }
    }

    /**
     * Get integration metrics with transaction-aware messaging statistics
     */
    public IntegrationMetrics getMetrics() {
        return new IntegrationMetrics(
                totalMessagesSent.get(),
                sendErrors.get(),
                queuedMessages.get(),
                transactionRollbacks.get(),
                integrationEnabled,
                Instant.now()
        );
    }

    /**
     * Check if there are pending messages in the current transaction
     * Vietnamese Business Context: Kiểm tra xem có tin nhắn đang chờ trong giao dịch hiện tại không
     */
    public boolean hasPendingMessages() {
        List<QueuedWebSocketMessage> queue = transactionMessageQueue.get();
        return queue != null && !queue.isEmpty();
    }

    /**
     * Get count of pending messages in current transaction
     */
    public int getPendingMessageCount() {
        List<QueuedWebSocketMessage> queue = transactionMessageQueue.get();
        return queue != null ? queue.size() : 0;
    }

    /**
     * Integration metrics holder with transaction-aware messaging statistics
     */
    public static class IntegrationMetrics {
        private final long totalMessagesSent;
        private final long sendErrors;
        private final long queuedMessages;
        private final long transactionRollbacks;
        private final boolean enabled;
        private final Instant timestamp;

        public IntegrationMetrics(long totalMessagesSent, long sendErrors, long queuedMessages,
                                long transactionRollbacks, boolean enabled, Instant timestamp) {
            this.totalMessagesSent = totalMessagesSent;
            this.sendErrors = sendErrors;
            this.queuedMessages = queuedMessages;
            this.transactionRollbacks = transactionRollbacks;
            this.enabled = enabled;
            this.timestamp = timestamp;
        }

        public long getTotalMessagesSent() { return totalMessagesSent; }
        public long getSendErrors() { return sendErrors; }
        public long getQueuedMessages() { return queuedMessages; }
        public long getTransactionRollbacks() { return transactionRollbacks; }
        public boolean isEnabled() { return enabled; }
        public Instant getTimestamp() { return timestamp; }

        public double getErrorRate() {
            return totalMessagesSent > 0 ? (double) sendErrors / totalMessagesSent : 0.0;
        }

        public double getTransactionSuccessRate() {
            long totalTransactions = queuedMessages + transactionRollbacks;
            return totalTransactions > 0 ? (double) queuedMessages / totalTransactions : 1.0;
        }
    }
}
