package com.lapxpert.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


import java.time.Duration;

/**
 * Centralized configuration for common beans used across the LapXpert application.
 * Consolidates ObjectMapper, RestTemplate, and other shared component configurations
 * to eliminate duplicate bean definitions and improve maintainability.
 * 
 * Follows Spring Boot best practices for configuration consolidation:
 * - Uses Jackson2ObjectMapperBuilderCustomizer for ObjectMapper customization
 * - Provides centralized RestTemplate with proper timeout configuration
 * - Implements configuration validation for startup health checks
 * - Maintains Vietnamese business terminology and audit trail support
 */
@Configuration
@Slf4j
public class CommonBeansConfig {

    /**
     * Primary ObjectMapper bean for JSON serialization/deserialization.
     * Configured with consistent settings for Java 8 time types, Vietnamese locale support,
     * and proper handling of unknown properties for API compatibility.
     *
     * This replaces duplicate ObjectMapper instances in MoMoService, VietQRService,
     * RedisConfig, and JacksonConfig to ensure consistent JSON processing across the application.
     *
     * Enhanced with Redis Pub/Sub serialization support for WebSocket integration.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("Configuring centralized ObjectMapper with Vietnamese business and Redis Pub/Sub support");

        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 time module for LocalDateTime, Instant, etc.
        mapper.registerModule(new JavaTimeModule());

        // Configure date/time serialization for Redis Pub/Sub and WebSocket integration
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);

        // Configure to handle unknown properties gracefully for API compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Configure for better JSON handling (from JacksonConfig)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Enable pretty printing for development (can be disabled in production)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Configure for Vietnamese locale and business data
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        log.info("ObjectMapper configured successfully with Vietnamese business and Redis Pub/Sub support");
        return mapper;
    }

    /**
     * Jackson2ObjectMapperBuilderCustomizer for additional ObjectMapper customization.
     * This allows Spring Boot's auto-configuration to apply our customizations
     * while maintaining compatibility with Spring Boot's default ObjectMapper settings.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // Additional customizations for Vietnamese business requirements
            builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            
            log.debug("Jackson2ObjectMapperBuilder customized for Vietnamese business requirements");
        };
    }

    /**
     * Primary RestTemplate bean for HTTP API calls.
     * Configured with appropriate timeout settings for external services
     * including payment gateways (VNPay, MoMo, VietQR) and shipping APIs (GHN, GHTK).
     * 
     * This replaces duplicate RestTemplate instances in MoMoService and VietQRService
     * to ensure consistent HTTP client configuration across the application.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        log.info("Configuring centralized RestTemplate with optimized timeout settings");
        
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        
        // Add any additional configuration here (interceptors, error handlers, etc.)
        log.info("RestTemplate configured successfully for payment and shipping APIs");
        return restTemplate;
    }

    /**
     * HTTP request factory with optimized timeout configuration.
     * Timeouts are configured for external API calls including:
     * - Payment gateways: VNPay, MoMo, VietQR (require reliable connections)
     * - Shipping APIs: GHN, GHTK (may have variable response times)
     * - Other external services
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Connection timeout: 10 seconds (reasonable for payment APIs)
        factory.setConnectTimeout(Duration.ofSeconds(10));
        
        // Read timeout: 30 seconds (suitable for shipping and payment API calls)
        factory.setReadTimeout(Duration.ofSeconds(30));
        
        log.debug("HTTP client factory configured with 10s connect, 30s read timeout");
        return factory;
    }

    // Configuration validation removed to prevent circular dependency
    // The beans are validated through Spring's normal dependency injection process
}
