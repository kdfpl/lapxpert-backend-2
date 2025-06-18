package com.lapxpert.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

// Redisson imports for distributed locking
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

/**
 * Simplified Redis configuration for caching
 * Provides basic Redis setup with reasonable defaults
 * Uses simple TTL categories instead of complex per-cache configuration
 */
@Configuration
@Slf4j
public class RedisConfig {

    // Inject centralized ObjectMapper from CommonBeansConfig
    @Autowired
    private ObjectMapper centralObjectMapper;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    // Production Connection Pool Configuration
    @Value("${spring.data.redis.lettuce.pool.max-active:20}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:10}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Value("${spring.data.redis.lettuce.pool.max-wait:5000}")
    private long maxWaitMillis;

    @Value("${spring.data.redis.timeout:10000}")
    private long timeoutMillis;

    @Value("${spring.data.redis.lettuce.shutdown-timeout:5000}")
    private long shutdownTimeoutMillis;

    // Simplified TTL Configuration - 3 basic categories
    @Value("${cache.ttl.short:15}")
    private int shortTermTtlMinutes;

    @Value("${cache.ttl.medium:60}")
    private int mediumTermTtlMinutes;

    @Value("${cache.ttl.long:1440}")
    private int longTermTtlMinutes;

    // ==================== CONNECTION FACTORY ====================

    /**
     * Production-optimized Redis connection factory with Lettuce client
     * Provides timeout configuration and resource management
     * Configured for standalone Redis deployment with production settings
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring production Redis connection to {}:{} (database: {})", redisHost, redisPort, redisDatabase);

        try {
            // Redis server configuration
            RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
            serverConfig.setHostName(redisHost);
            serverConfig.setPort(redisPort);
            serverConfig.setDatabase(redisDatabase);

            // Set password if provided
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                serverConfig.setPassword(redisPassword);
                log.info("Redis password authentication enabled");
            } else {
                log.info("Redis password authentication disabled");
            }

            // Production client configuration with optimized timeouts
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofMillis(timeoutMillis))
                    .shutdownTimeout(Duration.ofMillis(shutdownTimeoutMillis))
                    .clientResources(clientResources())
                    .build();

            LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);

            // Production settings
            factory.setValidateConnection(true);
            factory.setShareNativeConnection(false); // Avoid read-only issues
            factory.setEagerInitialization(true); // Catch connection issues early

            factory.afterPropertiesSet();

            log.info("Production Redis connection factory configured successfully with timeout={}ms, shutdownTimeout={}ms",
                    timeoutMillis, shutdownTimeoutMillis);
            return factory;

        } catch (Exception e) {
            log.error("Failed to configure Redis connection factory: {}", e.getMessage(), e);
            throw new RuntimeException("Redis configuration failed", e);
        }
    }

    /**
     * Production-optimized client resources for Lettuce
     * Manages I/O threads and connection resources efficiently
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4) // Optimize for production load
                .computationThreadPoolSize(4)
                .build();
    }

    // ==================== REDISSON CLIENT ====================

    /**
     * Redisson client for distributed locking
     * Configured to use the same Redis instance as the cache
     * Provides distributed locks for inventory race condition prevention
     */
    @Bean
    public RedissonClient redissonClient() {
        log.info("Configuring Redisson client for distributed locking");

        try {
            Config config = new Config();

            // Configure single server setup to match existing Redis configuration
            SingleServerConfig singleServerConfig = config.useSingleServer()
                    .setAddress("redis://" + redisHost + ":" + redisPort)
                    .setDatabase(redisDatabase)
                    .setConnectionMinimumIdleSize(2)
                    .setConnectionPoolSize(10)
                    .setConnectTimeout((int) timeoutMillis)
                    .setTimeout((int) timeoutMillis)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);

            // Set password if provided
            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                singleServerConfig.setPassword(redisPassword);
                log.info("Redisson password authentication enabled");
            } else {
                log.info("Redisson password authentication disabled");
            }

            // Configure lock watchdog timeout for automatic lock renewal
            config.setLockWatchdogTimeout(30000); // 30 seconds

            RedissonClient redissonClient = Redisson.create(config);

            log.info("Redisson client configured successfully for distributed locking");
            return redissonClient;

        } catch (Exception e) {
            log.error("Failed to configure Redisson client: {}", e.getMessage(), e);
            throw new RuntimeException("Redisson configuration failed", e);
        }
    }

    // ==================== REDIS TEMPLATE ====================

    /**
     * Primary RedisTemplate for String-Object operations
     * Used by ProductRatingCacheService and other caching services
     * Configured for reliable read/write operations
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Configuring primary RedisTemplate<String, Object>");

        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Configure serializers
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

            // Key serialization
            template.setKeySerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);

            // Value serialization
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);

            // Enable transaction support for write operations
            template.setEnableTransactionSupport(false);

            template.afterPropertiesSet();

            log.info("RedisTemplate<String, Object> configured successfully");
            return template;

        } catch (Exception e) {
            log.error("Failed to configure RedisTemplate: {}", e.getMessage(), e);
            throw new RuntimeException("RedisTemplate configuration failed", e);
        }
    }

    // ==================== CACHE MANAGER ====================

    /**
     * Simplified Redis cache manager with basic TTL categories
     * Uses reasonable defaults instead of complex per-cache configuration
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring simplified Redis CacheManager");

        try {
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(mediumTermTtlMinutes)) // Default to medium-term TTL
                    .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                            .fromSerializer(createJsonSerializer()))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    // Short-term caches (15 minutes) - frequently changing data
                    .withCacheConfiguration("activeSanPhamList",
                        defaultConfig.entryTtl(Duration.ofMinutes(shortTermTtlMinutes)))
                    .withCacheConfiguration("searchResults",
                        defaultConfig.entryTtl(Duration.ofMinutes(shortTermTtlMinutes)))
                    .withCacheConfiguration("cartData",
                        defaultConfig.entryTtl(Duration.ofMinutes(shortTermTtlMinutes)))
                    // Medium-term caches (60 minutes) - moderately changing data
                    .withCacheConfiguration("productRatings",
                        defaultConfig.entryTtl(Duration.ofMinutes(mediumTermTtlMinutes)))
                    .withCacheConfiguration("sanPhamList",
                        defaultConfig.entryTtl(Duration.ofMinutes(mediumTermTtlMinutes)))
                    .withCacheConfiguration("userSessions",
                        defaultConfig.entryTtl(Duration.ofMinutes(mediumTermTtlMinutes)))
                    .withCacheConfiguration("shippingFees",
                        defaultConfig.entryTtl(Duration.ofMinutes(mediumTermTtlMinutes)))
                    // Long-term caches (24 hours) - rarely changing data
                    .withCacheConfiguration("categories",
                        defaultConfig.entryTtl(Duration.ofMinutes(longTermTtlMinutes)))
                    .withCacheConfiguration("systemConfig",
                        defaultConfig.entryTtl(Duration.ofMinutes(longTermTtlMinutes)))
                    .withCacheConfiguration("popularProducts",
                        defaultConfig.entryTtl(Duration.ofMinutes(longTermTtlMinutes)))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to configure Redis cache manager, falling back to simple cache: {}", e.getMessage());
            return fallbackCacheManager();
        }
    }

    /**
     * Fallback cache manager when Redis is not available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager fallbackCacheManager() {
        log.info("Configuring fallback ConcurrentMapCacheManager");
        return new ConcurrentMapCacheManager(
            "productRatings", "sanPhamList", "activeSanPhamList",
            "searchResults", "popularProducts", "userSessions",
            "cartData", "categories", "systemConfig", "shippingFees"
        );
    }

    // ==================== HYBRID CONSISTENCY INITIALIZATION ====================

    /**
     * Initialize hybrid cache consistency model on startup
     */
    @PostConstruct
    public void initializeHybridConsistency() {
        try {
            log.info("Initializing hybrid cache consistency model for LapXpert system");

            // The CacheConsistencyManager will initialize its mappings via @PostConstruct
            // This method serves as a coordination point for Redis-specific initialization
            log.info("Redis configuration ready for hybrid cache consistency model");

        } catch (Exception e) {
            log.error("Failed to initialize hybrid cache consistency model: {}", e.getMessage(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Test Redis connection and write capability
     * This method can be called manually to verify Redis connectivity
     */
    public boolean testRedisConnection() {
        try {
            RedisConnectionFactory factory = redisConnectionFactory();
            RedisTemplate<String, Object> template = redisTemplate(factory);

            // Test basic connectivity
            String testKey = "lapxpert:health:test";
            String testValue = "connection-test-" + System.currentTimeMillis();

            // Test write operation
            template.opsForValue().set(testKey, testValue, Duration.ofSeconds(10));
            log.info("Redis write test successful");

            // Test read operation
            String retrievedValue = (String) template.opsForValue().get(testKey);
            if (testValue.equals(retrievedValue)) {
                log.info("Redis read test successful");

                // Clean up test key
                template.delete(testKey);
                log.info("Redis connection test completed successfully");
                return true;
            } else {
                log.warn("Redis read test failed: expected '{}', got '{}'", testValue, retrievedValue);
                return false;
            }

        } catch (Exception e) {
            log.error("Redis connection test failed: {}", e.getMessage());
            log.warn("Application will fall back to simple cache if Redis operations fail");
            return false;
        }
    }

    /**
     * Create simplified Jackson JSON serializer using centralized ObjectMapper.
     * Uses the ObjectMapper from CommonBeansConfig to ensure consistent JSON processing
     * across Redis caching and other application components.
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        log.debug("Creating Redis JSON serializer using centralized ObjectMapper");

        // Use centralized ObjectMapper from CommonBeansConfig instead of creating new instance
        // This ensures consistent JSON serialization across Redis and other components
        return new Jackson2JsonRedisSerializer<>(centralObjectMapper, Object.class);
    }
}
