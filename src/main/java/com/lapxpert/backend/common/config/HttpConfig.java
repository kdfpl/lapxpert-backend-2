package com.lapxpert.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP client configuration for external API calls
 * Provides RestTemplate bean with appropriate timeout settings
 */
@Configuration
@Slf4j
public class HttpConfig {
    
    /**
     * RestTemplate bean for HTTP API calls
     * Configured with reasonable timeout settings for external services
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        log.info("RestTemplate configured with timeout settings");
        return restTemplate;
    }
    
    /**
     * HTTP request factory with timeout configuration
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // Connection timeout: 10 seconds
        factory.setConnectTimeout(Duration.ofSeconds(10));
        
        // Read timeout: 30 seconds (suitable for shipping API calls)
        factory.setReadTimeout(Duration.ofSeconds(30));
        
        return factory;
    }
}
