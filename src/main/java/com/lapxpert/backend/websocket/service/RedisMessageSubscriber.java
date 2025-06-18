package com.lapxpert.backend.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Message Subscriber Service
 *
 * Handles incoming messages from Redis Pub/Sub channels and forwards them to appropriate
 * STOMP destinations for connected WebSocket clients. Supports Vietnamese topic naming
 * conventions and prepares for future Customer Service Chatbox integration.
 *
 * Message Flow:
 * 1. Services publish messages to Redis channels via WebSocketIntegrationService
 * 2. This subscriber receives messages from Redis
 * 3. Messages are deserialized using Spring-configured ObjectMapper (with Java 8 time support)
 * 4. Messages are forwarded to STOMP topics via SimpMessagingTemplate
 * 5. Connected WebSocket clients receive real-time updates
 *
 * This service uses Spring-managed ObjectMapper with JSR310 support to handle LocalDateTime serialization.
 * This service is part of the enhanced WebSocket capabilities within the monolithic application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Handle global WebSocket messages from Redis
     * Used for general announcements and system-wide notifications
     */
    public void handleGlobalMessage(String messageJson, String channel) {
        log.debug("Received global message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message)) {
                // Forward message to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded global message to STOMP destination: {}", message.getDestination());
            }
        } catch (Exception e) {
            log.error("Error processing global message from Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle product price update messages from Redis
     * Supports Vietnamese topic structure: /topic/gia-san-pham/{variantId}
     */
    public void handlePriceMessage(String messageJson, String channel) {
        log.debug("Received price message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isPriceDestination(message.getDestination())) {
                // Forward price update to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded price update to STOMP destination: {}", message.getDestination());
            } else {
                log.warn("Invalid price message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing price message from Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle voucher notification messages from Redis
     * Supports Vietnamese topic structure: /topic/phieu-giam-gia/{voucherId} and /topic/dot-giam-gia/{campaignId}
     */
    public void handleVoucherMessage(String messageJson, String channel) {
        log.debug("Received voucher message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isVoucherDestination(message.getDestination())) {
                // Forward voucher notification to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded voucher notification to STOMP destination: {}", message.getDestination());
            } else {
                log.warn("Invalid voucher message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing voucher message from Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle health monitoring messages from Redis
     * Used for WebSocket service health status broadcasting
     */
    public void handleHealthMessage(String messageJson, String channel) {
        log.debug("Received health message from Redis channel {}: {}", channel, messageJson);

        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isHealthDestination(message.getDestination())) {
                // Forward health status to WebSocket clients via STOMP
                messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
                log.debug("Forwarded health status to STOMP destination: {}", message.getDestination());
            } else {
                log.warn("Invalid health message destination: {}",
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing health message from Redis: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle customer service chatbox messages from Redis
     * Prepared for future Customer Service Chatbox integration
     * Supports user-specific destinations: /user/{userId}/queue/chat
     */
    public void handleChatboxMessage(String messageJson, String channel) {
        log.debug("Received chatbox message from Redis channel {}: {}", channel, messageJson);
        
        try {
            WebSocketMessage message = deserializeMessage(messageJson);
            if (isValidMessage(message) && isChatboxDestination(message.getDestination())) {
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
            } else {
                log.warn("Invalid chatbox message destination: {}", 
                        message != null ? message.getDestination() : "null");
            }
        } catch (Exception e) {
            log.error("Error processing chatbox message from Redis: {}", e.getMessage(), e);
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
}
