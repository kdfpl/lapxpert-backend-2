package com.lapxpert.backend.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.websocket.service.RedisMessageSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;


/**
 * Redis Pub/Sub Configuration for Enhanced WebSocket Messaging with Ordering and Delivery Guarantees
 *
 * Configures Redis message broadcasting within the monolithic application for improved
 * WebSocket message distribution with enhanced reliability features:
 * - Message ordering guarantees within channels
 * - Delivery confirmation mechanisms
 * - Message deduplication support
 * - Retry logic for failed deliveries
 * - Performance monitoring and metrics
 *
 * Supports Vietnamese topic naming conventions and prepares for future chatbox integration.
 *
 * Redis Channels:
 * - lapxpert:websocket:global - General WebSocket messages
 * - lapxpert:websocket:price - Product price updates
 * - lapxpert:websocket:voucher - Voucher notifications
 * - lapxpert:websocket:chatbox - Future customer service chat
 * - lapxpert:websocket:ack - Delivery acknowledgments
 *
 * Can be enabled/disabled via: websocket.redis.pubsub.enabled=true/false
 */
@Configuration
@ConditionalOnProperty(name = "websocket.redis.pubsub.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RedisPubSubConfig {

    // Redis channel constants for WebSocket message broadcasting
    public static final String GLOBAL_CHANNEL = "lapxpert:websocket:global";
    public static final String PRICE_CHANNEL = "lapxpert:websocket:price";
    public static final String VOUCHER_CHANNEL = "lapxpert:websocket:voucher";
    public static final String HEALTH_CHANNEL = "lapxpert:websocket:health";
    public static final String CHATBOX_CHANNEL = "lapxpert:websocket:chatbox";
    public static final String ACK_CHANNEL = "lapxpert:websocket:ack";

    // Redis keys for message ordering and deduplication
    public static final String MESSAGE_SEQUENCE_KEY = "lapxpert:websocket:sequence";
    public static final String MESSAGE_DEDUP_KEY_PREFIX = "lapxpert:websocket:dedup:";
    public static final String PENDING_ACK_KEY_PREFIX = "lapxpert:websocket:pending_ack:";

    @Value("${redis.pubsub.enabled:true}")
    private boolean pubSubEnabled;

    @Value("${websocket.redis.pubsub.ordering.enabled:true}")
    private boolean orderingEnabled;

    @Value("${websocket.redis.pubsub.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Value("${websocket.redis.pubsub.deduplication.ttl:300}")
    private int deduplicationTtlSeconds;

    @Value("${websocket.redis.pubsub.delivery.confirmation.enabled:true}")
    private boolean deliveryConfirmationEnabled;

    /**
     * Redis message listener container for handling incoming messages from other services
     * Enhanced with ordering guarantees and delivery confirmation support
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter globalMessageAdapter,
            MessageListenerAdapter priceMessageAdapter,
            MessageListenerAdapter voucherMessageAdapter,
            MessageListenerAdapter healthMessageAdapter,
            MessageListenerAdapter chatboxMessageAdapter,
            MessageListenerAdapter ackMessageAdapter) {

        if (!pubSubEnabled) {
            log.warn("Redis Pub/Sub is disabled - WebSocket service will not scale horizontally");
            return null;
        }

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to all WebSocket-related Redis channels
        container.addMessageListener(globalMessageAdapter, globalChannelTopic());
        container.addMessageListener(priceMessageAdapter, priceChannelTopic());
        container.addMessageListener(voucherMessageAdapter, voucherChannelTopic());
        container.addMessageListener(healthMessageAdapter, healthChannelTopic());
        container.addMessageListener(chatboxMessageAdapter, chatboxChannelTopic());
        container.addMessageListener(ackMessageAdapter, ackChannelTopic());

        // Configure container for production stability and enhanced reliability
        container.setTaskExecutor(null); // Use default task executor
        container.setSubscriptionExecutor(null); // Use default subscription executor
        container.setRecoveryInterval(3000L); // 3 seconds recovery interval for faster recovery

        // Enhanced error handling and connection management
        container.setErrorHandler(throwable -> {
            log.error("Redis message listener error - attempting recovery", throwable);
        });

        log.info("Redis message listener container configured for {} channels", 6);
        log.info("Subscribed channels: {}, {}, {}, {}, {}, {}",
                GLOBAL_CHANNEL, PRICE_CHANNEL, VOUCHER_CHANNEL, HEALTH_CHANNEL, CHATBOX_CHANNEL, ACK_CHANNEL);
        log.info("Enhanced features enabled - Ordering: {}, Deduplication: {}, Delivery Confirmation: {}",
                orderingEnabled, deduplicationEnabled, deliveryConfirmationEnabled);

        return container;
    }

    /**
     * Channel topic beans for Redis Pub/Sub
     */
    @Bean
    public ChannelTopic globalChannelTopic() {
        return new ChannelTopic(GLOBAL_CHANNEL);
    }

    @Bean
    public ChannelTopic priceChannelTopic() {
        return new ChannelTopic(PRICE_CHANNEL);
    }

    @Bean
    public ChannelTopic voucherChannelTopic() {
        return new ChannelTopic(VOUCHER_CHANNEL);
    }

    @Bean
    public ChannelTopic healthChannelTopic() {
        return new ChannelTopic(HEALTH_CHANNEL);
    }

    @Bean
    public ChannelTopic chatboxChannelTopic() {
        return new ChannelTopic(CHATBOX_CHANNEL);
    }

    @Bean
    public ChannelTopic ackChannelTopic() {
        return new ChannelTopic(ACK_CHANNEL);
    }

    /**
     * Message listener adapters for different channel types
     */
    @Bean
    public MessageListenerAdapter globalMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleGlobalMessage");
    }

    @Bean
    public MessageListenerAdapter priceMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handlePriceMessage");
    }

    @Bean
    public MessageListenerAdapter voucherMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleVoucherMessage");
    }

    @Bean
    public MessageListenerAdapter healthMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleHealthMessage");
    }

    @Bean
    public MessageListenerAdapter chatboxMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleChatboxMessage");
    }

    @Bean
    public MessageListenerAdapter ackMessageAdapter(RedisMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleAckMessage");
    }

    /**
     * Redis message subscriber service
     * Autowired with SimpMessagingTemplate, ObjectMapper, and RedisTemplate for enhanced message processing
     */
    @Bean
    public RedisMessageSubscriber redisMessageSubscriber(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        return new RedisMessageSubscriber(messagingTemplate, objectMapper, redisTemplate);
    }
}
