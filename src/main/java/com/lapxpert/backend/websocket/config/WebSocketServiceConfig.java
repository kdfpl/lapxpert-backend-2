package com.lapxpert.backend.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Unified WebSocket Configuration for LapXpert Application
 *
 * Consolidates WebSocket STOMP configuration with enhanced capabilities including:
 * - STOMP messaging protocol for public notifications
 * - Redis Pub/Sub integration for horizontal scaling
 * - Vietnamese topic naming convention support
 * - Production-ready transport limits and heartbeat configuration
 * - High-concurrency thread pool optimization
 * - Future Customer Service Chatbox preparation
 *
 * This configuration replaces the previous WebSocketConfig in common/config
 * and provides a unified WebSocket setup within the websocket module.
 *
 * Features:
 * - No authentication required (used only for push notifications)
 * - Supports Vietnamese topic naming: /topic/gia-san-pham, /topic/phieu-giam-gia
 * - Redis Pub/Sub message broadcasting for improved scalability
 * - Enhanced TaskScheduler for better performance
 *
 * Can be enabled/disabled via: websocket.enhanced.enabled=true/false
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = "websocket.enhanced.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class WebSocketServiceConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.enhanced.task-scheduler.pool-size:4}")
    private int taskSchedulerPoolSize;

    @Value("${websocket.heartbeat.interval:10000}")
    private long heartbeatInterval;

    /**
     * Primary TaskScheduler bean for WebSocket heartbeat functionality
     * Replaces the basic heartBeatScheduler with enhanced capabilities
     */
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();

        log.info("WebSocket heartbeat TaskScheduler configured");
        return scheduler;
    }

    /**
     * Enhanced TaskScheduler for WebSocket operations
     * Provides better performance for Redis Pub/Sub and health monitoring
     */
    @Bean(name = "enhancedWebSocketTaskScheduler")
    public TaskScheduler enhancedWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(taskSchedulerPoolSize);
        scheduler.setThreadNamePrefix("enhanced-websocket-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        log.info("Enhanced WebSocket TaskScheduler configured with {} threads", taskSchedulerPoolSize);
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics and queues with heartbeat configuration
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{heartbeatInterval, heartbeatInterval})
                .setTaskScheduler(heartBeatScheduler());

        // Set application destination prefixes
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for private messages (future chatbox feature)
        config.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured with Vietnamese topic naming and heartbeat monitoring ({}ms)", heartbeatInterval);
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
