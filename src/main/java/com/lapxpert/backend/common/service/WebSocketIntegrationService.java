package com.lapxpert.backend.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
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

            String destination = "/topic/inventory/" + productId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "INVENTORY_UPDATE");

            log.debug("Sent inventory update for product {}", productId);

        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Failed to send inventory update for product {}: {}", productId, e.getMessage(), e);
        }
    }

    /**
     * Send order status update notification
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

            String destination = "/topic/orders/" + orderId;
            publishMessage(GLOBAL_CHANNEL, destination, update, "ORDER_UPDATE");

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
     * Core method to publish message to Redis Pub/Sub
     */
    private void publishMessage(String channel, String destination, Object payload, String messageType) {
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

        } catch (JsonProcessingException e) {
            sendErrors.incrementAndGet();
            log.error("Error serializing message for Redis: {}", e.getMessage(), e);
        } catch (Exception e) {
            sendErrors.incrementAndGet();
            log.error("Error publishing message to Redis channel {}: {}", channel, e.getMessage(), e);
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
     * Get integration metrics
     */
    public IntegrationMetrics getMetrics() {
        return new IntegrationMetrics(
                totalMessagesSent.get(),
                sendErrors.get(),
                integrationEnabled,
                Instant.now()
        );
    }

    /**
     * Integration metrics holder
     */
    public static class IntegrationMetrics {
        private final long totalMessagesSent;
        private final long sendErrors;
        private final boolean enabled;
        private final Instant timestamp;

        public IntegrationMetrics(long totalMessagesSent, long sendErrors, boolean enabled, Instant timestamp) {
            this.totalMessagesSent = totalMessagesSent;
            this.sendErrors = sendErrors;
            this.enabled = enabled;
            this.timestamp = timestamp;
        }

        public long getTotalMessagesSent() { return totalMessagesSent; }
        public long getSendErrors() { return sendErrors; }
        public boolean isEnabled() { return enabled; }
        public Instant getTimestamp() { return timestamp; }
        
        public double getErrorRate() {
            return totalMessagesSent > 0 ? (double) sendErrors / totalMessagesSent : 0.0;
        }
    }
}
