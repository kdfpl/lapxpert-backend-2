package com.lapxpert.backend.shipping.domain.config;

import com.lapxpert.backend.shipping.domain.dto.ProviderComparisonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration class for shipping provider comparison
 * Manages comparison criteria and provider preferences
 */
@Slf4j
@Component
public class ShippingComparisonConfig {
    
    @Value("${shipping.comparison.preferred-provider:GHN}")
    private String preferredProvider;
    
    @Value("${shipping.comparison.fallback-provider:GHTK}")
    private String fallbackProvider;
    
    @Value("${shipping.comparison.cost-weight:0.5}")
    private BigDecimal costWeight;
    
    @Value("${shipping.comparison.reliability-weight:0.3}")
    private BigDecimal reliabilityWeight;
    
    @Value("${shipping.comparison.speed-weight:0.2}")
    private BigDecimal speedWeight;
    
    @Value("${shipping.comparison.max-acceptable-fee:500000}")
    private BigDecimal maxAcceptableFee;
    
    @Value("${shipping.comparison.max-response-time-ms:10000}")
    private long maxResponseTimeMs;
    
    @Value("${shipping.comparison.min-reliability-score:0.7}")
    private BigDecimal minReliabilityScore;
    
    /**
     * Get default comparison criteria from configuration
     */
    public ProviderComparisonResult.ComparisonCriteria getDefaultCriteria() {
        return ProviderComparisonResult.ComparisonCriteria.builder()
            .preferredProvider(preferredProvider)
            .fallbackProvider(fallbackProvider)
            .costWeight(costWeight)
            .reliabilityWeight(reliabilityWeight)
            .speedWeight(speedWeight)
            .maxAcceptableFee(maxAcceptableFee)
            .maxResponseTimeMs(maxResponseTimeMs)
            .minReliabilityScore(minReliabilityScore)
            .build();
    }
    
    /**
     * Validate configuration on startup
     */
    public boolean validateConfiguration() {
        boolean isValid = true;
        
        // Validate weights sum to approximately 1.0
        BigDecimal totalWeight = costWeight.add(reliabilityWeight).add(speedWeight);
        if (totalWeight.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Shipping comparison weights do not sum to 1.0: {}", totalWeight);
            isValid = false;
        }
        
        // Validate provider names
        if (!isValidProviderName(preferredProvider)) {
            log.error("Invalid preferred provider: {}", preferredProvider);
            isValid = false;
        }
        
        if (!isValidProviderName(fallbackProvider)) {
            log.error("Invalid fallback provider: {}", fallbackProvider);
            isValid = false;
        }
        
        // Validate thresholds
        if (maxAcceptableFee.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Max acceptable fee must be positive: {}", maxAcceptableFee);
            isValid = false;
        }
        
        if (maxResponseTimeMs <= 0) {
            log.error("Max response time must be positive: {}", maxResponseTimeMs);
            isValid = false;
        }
        
        if (minReliabilityScore.compareTo(BigDecimal.ZERO) < 0 || 
            minReliabilityScore.compareTo(BigDecimal.ONE) > 0) {
            log.error("Min reliability score must be between 0 and 1: {}", minReliabilityScore);
            isValid = false;
        }
        
        if (isValid) {
            log.info("Shipping comparison configuration validation successful");
            log.info("Preferred provider: {}, Fallback: {}", preferredProvider, fallbackProvider);
            log.info("Weights - Cost: {}, Reliability: {}, Speed: {}", 
                costWeight, reliabilityWeight, speedWeight);
        } else {
            log.error("Shipping comparison configuration validation failed");
        }
        
        return isValid;
    }
    
    /**
     * Check if provider name is valid
     */
    private boolean isValidProviderName(String providerName) {
        return providerName != null && 
               (providerName.equals("GHN") || providerName.equals("GHTK"));
    }
    
    // Getters for individual properties
    public String getPreferredProvider() { return preferredProvider; }
    public String getFallbackProvider() { return fallbackProvider; }
    public BigDecimal getCostWeight() { return costWeight; }
    public BigDecimal getReliabilityWeight() { return reliabilityWeight; }
    public BigDecimal getSpeedWeight() { return speedWeight; }
    public BigDecimal getMaxAcceptableFee() { return maxAcceptableFee; }
    public long getMaxResponseTimeMs() { return maxResponseTimeMs; }
    public BigDecimal getMinReliabilityScore() { return minReliabilityScore; }
}
