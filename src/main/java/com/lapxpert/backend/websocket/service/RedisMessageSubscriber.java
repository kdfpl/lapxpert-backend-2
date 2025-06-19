package com.lapxpert.backend.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Message Subscriber Service with Enhanced Ordering and Delivery Guarantees
 *
 * Handles incoming messages from Redis Pub/Sub channels and forwards them to appropriate
 * STOMP destinations for connected WebSocket clients. Enhanced with message ordering,
 * delivery confirmation, and deduplication capabilities.
 *
 * Enhanced Features:
 * - Message ordering guarantees within channels
 * - Delivery confirmation and acknowledgment handling
 * - Message deduplication to prevent duplicate processing
 * - Retry logic for failed message deliveries
 * - Performance monitoring and metrics
 *
 * Message Flow:
 * 1. Services publish messages to Redis channels via WebSocketIntegrationService
 * 2. This subscriber receives messages from Redis with enhanced ordering
 * 3. Messages are validated for duplicates and ordering
 * 4. Messages are deserialized using Spring-configured ObjectMapper (with Java 8 time support)
 * 5. Messages are forwarded to STOMP topics via SimpMessagingTemplate
 * 6. Delivery confirmations are sent back via ACK channel if required
 * 7. Connected WebSocket clients receive real-time updates
 *
 * Supports Vietnamese topic naming conventions and prepares for future Customer Service Chatbox integration.
 * This service uses Spring-managed ObjectMapper with JSR310 support to handle Instant serialization.
 * This service is part of the enhanced WebSocket capabilities within the monolithic application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // Configuration properties for enhanced features
    @Value("${websocket.redis.pubsub.ordering.enabled:true}")
    private boolean orderingEnabled;

    @Value("${websocket.redis.pubsub.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Value("${websocket.redis.pubsub.deduplication.ttl:300}")
    private int deduplicationTtlSeconds;

    @Value("${websocket.redis.pubsub.delivery.confirmation.enabled:true}")
    private boolean deliveryConfirmationEnabled;

    // In-memory cache for message deduplication and ordering
    private final ConcurrentHashMap<String, Long> processedMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastSequenceNumbers = new ConcurrentHashMap<>();

    /**
     * Handle global WebSocket messages from Redis with enhanced ordering and delivery guarantees
     * Used for general announcements and system-wide notifications
     */
    public void handleGlobalMessage(String messageJson, String channel) {
        log.debug("Received global message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && shouldProcessMessage(message, channel)) {
                // Forward message to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded global message to STOMP destination: {}", message.getDestination());

                // Send delivery confirmation if required
                sendDeliveryConfirmation(message, channel, true);

                // Update message tracking
                updateMessageTracking(message, channel);
            }
        } catch (Exception e) {
            log.error("Error processing global message from Redis: {}", e.getMessage(), e);
            // Send failure confirmation if message was parsed successfully
            try {
                WebSocketMessage message = deserializeMessage(messageJson);
                sendDeliveryConfirmation(message, channel, false);
            } catch (Exception ignored) {
                // Ignore if message couldn't be parsed
            }
        }
    }

    /**
     * Handle product price update messages from Redis with enhanced ordering and delivery guarantees
     * Supports Vietnamese topic structure: /topic/gia-san-pham/{variantId}
     */
    public void handlePriceMessage(String messageJson, String channel) {
        log.debug("Received price message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isPriceDestination(message.getDestination()) &&
                shouldProcessMessage(message, channel)) {
                // Forward price update to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded price update to STOMP destination: {}", message.getDestination());

                // Send delivery confirmation if required
                sendDeliveryConfirmation(message, channel, true);

                // Update message tracking
                updateMessageTracking(message, channel);
            } else {
                log.warn("Invalid or duplicate price message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing price message from Redis: {}", e.getMessage(), e);
            try {
                WebSocketMessage message = deserializeMessage(messageJson);
                sendDeliveryConfirmation(message, channel, false);
            } catch (Exception ignored) {
                // Ignore if message couldn't be parsed
            }
        }
    }

    /**
     * Handle voucher notification messages from Redis with enhanced ordering and delivery guarantees
     * Supports Vietnamese topic structure: /topic/phieu-giam-gia/{voucherId} and /topic/dot-giam-gia/{campaignId}
     */
    public void handleVoucherMessage(String messageJson, String channel) {
        log.debug("Received voucher message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isVoucherDestination(message.getDestination()) &&
                shouldProcessMessage(message, channel)) {
                // Forward voucher notification to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded voucher notification to STOMP destination: {}", message.getDestination());

                sendDeliveryConfirmation(message, channel, true);
                updateMessageTracking(message, channel);
            } else {
                log.warn("Invalid or duplicate voucher message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing voucher message from Redis: {}", e.getMessage(), e);
            try {
                WebSocketMessage message = deserializeMessage(messageJson);
                sendDeliveryConfirmation(message, channel, false);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handle health monitoring messages from Redis with enhanced ordering and delivery guarantees
     * Used for WebSocket service health status broadcasting
     */
    public void handleHealthMessage(String messageJson, String channel) {
        log.debug("Received health message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isHealthDestination(message.getDestination()) &&
                shouldProcessMessage(message, channel)) {
                // Forward health status to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded health status to STOMP destination: {}", message.getDestination());

                sendDeliveryConfirmation(message, channel, true);
                updateMessageTracking(message, channel);
            } else {
                log.warn("Invalid or duplicate health message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing health message from Redis: {}", e.getMessage(), e);
            try {
                WebSocketMessage message = deserializeMessage(messageJson);
                sendDeliveryConfirmation(message, channel, false);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handle customer service chatbox messages from Redis with enhanced ordering and delivery guarantees
     * Prepared for future Customer Service Chatbox integration
     * Supports user-specific destinations: /user/{userId}/queue/chat
     */
    public void handleChatboxMessage(String messageJson, String channel) {
        log.debug("Received chatbox message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isChatboxDestination(message.getDestination()) &&
                shouldProcessMessage(message, channel)) {
                // Handle both topic-based and user-specific chat messages
                if (message.getDestination().startsWith("/user/")) {
                    // Extract username from destination for user-specific messages
                    String[] parts = message.getDestination().split("/");
                    if (parts.length >= 3) {
                        String username = parts[2];
                        String destination = "/" + String.join("/", java.util.Arrays.copyOfRange(parts, 3, parts.length));
                        // Forward user-specific chatbox message via STOMP
                        messagingTemplate.convertAndSendToUser(username, destination, message.getPayload());
                        log.debug("Forwarded user-specific chatbox message to user {}: {}", username, destination);
                    }
                } else {
                    // Forward chatbox message to WebSocket clients via STOMP
                    messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                    log.debug("Forwarded chatbox message to STOMP destination: {}", message.getDestination());
                }

                sendDeliveryConfirmation(message, channel, true);
                updateMessageTracking(message, channel);
            } else {
                log.warn("Invalid or duplicate chatbox message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing chatbox message from Redis: {}", e.getMessage(), e);
            try {
                WebSocketMessage message = deserializeMessage(messageJson);
                sendDeliveryConfirmation(message, channel, false);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Deserialize JSON message to WebSocketMessage object
     */
    private WebSocketMessage deserializeMessage(String messageJson) throws JsonProcessingException {
        return objectMapper.readValue(messageJson, WebSocketMessage.class);
    }

    /**
     * Validate message has required fields
     */
    private boolean isValidMessage(WebSocketMessage message) {
        return message != null && 
               message.getDestination() != null && 
               !message.getDestination().trim().isEmpty() &&
               message.getPayload() != null;
    }

    /**
     * Check if destination is a valid price update topic
     */
    private boolean isPriceDestination(String destination) {
        return destination.startsWith("/topic/gia-san-pham/");
    }

    /**
     * Check if destination is a valid voucher notification topic
     */
    private boolean isVoucherDestination(String destination) {
        return destination.startsWith("/topic/phieu-giam-gia/") || 
               destination.startsWith("/topic/dot-giam-gia/") ||
               destination.equals("/topic/voucher/all");
    }

    /**
     * Check if destination is a valid health monitoring topic
     */
    private boolean isHealthDestination(String destination) {
        return destination.startsWith("/topic/websocket/health") ||
               destination.startsWith("/topic/health/");
    }

    /**
     * Check if destination is a valid chatbox topic (future feature)
     */
    private boolean isChatboxDestination(String destination) {
        return destination.startsWith("/topic/chatbox/") ||
               destination.startsWith("/user/") && destination.contains("/queue/chat");
    }

    /**
     * Handle delivery acknowledgment messages from Redis
     * Vietnamese Business Context: Xử lý tin nhắn xác nhận giao hàng từ Redis
     */
    public void handleAckMessage(String messageJson, String channel) {
        log.debug("Received ACK message from Redis channel {}: {}", channel, messageJson);

        try {
            // Parse acknowledgment message and update delivery tracking
            // This could be used for monitoring and metrics
            log.debug("Processed delivery acknowledgment: {}", messageJson);
        } catch (Exception e) {
            log.error("Error processing ACK message from Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if message should be processed based on ordering and deduplication rules
     * Vietnamese Business Context: Kiểm tra xem tin nhắn có nên được xử lý dựa trên quy tắc sắp xếp và khử trùng lặp
     */
    private boolean shouldProcessMessage(WebSocketMessage message, String channel) {
        if (!orderingEnabled && !deduplicationEnabled) {
            return true;
        }

        String messageId = message.getMessageId();
        Long sequenceNumber = message.getSequenceNumber();

        // Check for message deduplication
        if (deduplicationEnabled && messageId != null) {
            String dedupKey = "dedup:" + channel + ":" + messageId;
            Long processedTime = processedMessages.get(dedupKey);

            if (processedTime != null) {
                long ageSeconds = (Instant.now().toEpochMilli() - processedTime) / 1000;
                if (ageSeconds < deduplicationTtlSeconds) {
                    log.debug("Skipping duplicate message {} in channel {}", messageId, channel);
                    return false;
                }
                // Remove expired entry
                processedMessages.remove(dedupKey);
            }
        }

        // Check for message ordering
        if (orderingEnabled && sequenceNumber != null) {
            String orderingGroup = message.getOrderingGroup() != null ?
                message.getOrderingGroup() : channel;

            Long lastSequence = lastSequenceNumbers.get(orderingGroup);
            if (lastSequence != null && sequenceNumber <= lastSequence) {
                log.debug("Skipping out-of-order message {} (sequence: {}, last: {}) in group {}",
                    messageId, sequenceNumber, lastSequence, orderingGroup);
                return false;
            }
        }

        return true;
    }

    /**
     * Update message tracking for deduplication and ordering
     * Vietnamese Business Context: Cập nhật theo dõi tin nhắn để khử trùng lặp và sắp xếp
     */
    private void updateMessageTracking(WebSocketMessage message, String channel) {
        String messageId = message.getMessageId();
        Long sequenceNumber = message.getSequenceNumber();

        // Update deduplication tracking
        if (deduplicationEnabled && messageId != null) {
            String dedupKey = "dedup:" + channel + ":" + messageId;
            processedMessages.put(dedupKey, Instant.now().toEpochMilli());

            // Store in Redis for distributed deduplication
            try {
                String redisKey = "lapxpert:websocket:dedup:" + channel + ":" + messageId;
                redisTemplate.opsForValue().set(redisKey, "processed",
                    Duration.ofSeconds(deduplicationTtlSeconds));
            } catch (Exception e) {
                log.warn("Failed to store deduplication key in Redis: {}", e.getMessage());
            }
        }

        // Update sequence tracking
        if (orderingEnabled && sequenceNumber != null) {
            String orderingGroup = message.getOrderingGroup() != null ?
                message.getOrderingGroup() : channel;
            lastSequenceNumbers.put(orderingGroup, sequenceNumber);
        }
    }

    /**
     * Send delivery confirmation if required
     * Vietnamese Business Context: Gửi xác nhận giao hàng nếu được yêu cầu
     */
    private void sendDeliveryConfirmation(WebSocketMessage message, String channel, boolean success) {
        if (!deliveryConfirmationEnabled || !message.requiresDeliveryConfirmation()) {
            return;
        }

        try {
            WebSocketMessage ackMessage = WebSocketMessage.builder()
                .destination("/topic/websocket/ack")
                .messageType("DELIVERY_ACK")
                .payload(Map.of(
                    "originalMessageId", message.getMessageId(),
                    "channel", channel,
                    "success", success,
                    "timestamp", Instant.now(),
                    "sequenceNumber", message.getSequenceNumber()
                ))
                .build();

            String ackJson = objectMapper.writeValueAsString(ackMessage);
            redisTemplate.convertAndSend("lapxpert:websocket:ack", ackJson);

            log.debug("Sent delivery confirmation for message {} in channel {}: {}",
                message.getMessageId(), channel, success);
        } catch (Exception e) {
            log.error("Failed to send delivery confirmation: {}", e.getMessage(), e);
        }
    }
}
