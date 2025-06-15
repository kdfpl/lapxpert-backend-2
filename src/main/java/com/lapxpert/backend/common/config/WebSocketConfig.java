package com.lapxpert.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration with STOMP messaging protocol for public notifications.
 * No authentication required - used only for push notifications (price updates, voucher alerts, etc.)
 * All sensitive operations are handled through authenticated REST APIs.
 * Supports Vietnamese topic naming conventions for real-time features.
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics and queues
        config.enableSimpleBroker("/topic", "/queue");

        // Set application destination prefixes
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured with Vietnamese topic naming");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register native WebSocket endpoint - NO AUTHENTICATION REQUIRED
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*"); // Allow all localhost ports for development

        // Also register SockJS fallback endpoint
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("http://localhost:*")
                .withSockJS();

        log.info("WebSocket STOMP endpoints registered: /ws (native) and /ws-sockjs (SockJS) - public access");
    }

    // No authentication interceptor needed
    // WebSocket is used only for push notifications (public information)
    // All sensitive operations are handled through authenticated REST APIs
}
