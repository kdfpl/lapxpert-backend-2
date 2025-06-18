package com.lapxpert.backend.shipping.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provider performance tracking service
 * Tracks success rates, response times, and reliability metrics for shipping providers
 */
@Slf4j
@Service
public class ProviderPerformanceTracker {
    
    private final ConcurrentHashMap<String, ProviderMetrics> providerMetrics = new ConcurrentHashMap<>();
    
    /**
     * Record a successful API call
     */
    public void recordSuccess(String providerName, long responseTimeMs, BigDecimal shippingFee) {
        ProviderMetrics metrics = getOrCreateMetrics(providerName);
        metrics.recordSuccess(responseTimeMs, shippingFee);
        
        log.debug("Recorded success for {}: {}ms, fee: {}", providerName, responseTimeMs, shippingFee);
    }
    
    /**
     * Record a failed API call
     */
    public void recordFailure(String providerName, long responseTimeMs, String errorReason) {
        ProviderMetrics metrics = getOrCreateMetrics(providerName);
        metrics.recordFailure(responseTimeMs, errorReason);
        
        log.debug("Recorded failure for {}: {}ms, reason: {}", providerName, responseTimeMs, errorReason);
    }
    
    /**
     * Get reliability score for a provider (0.0 to 1.0)
     */
    public BigDecimal getReliabilityScore(String providerName) {
        ProviderMetrics metrics = providerMetrics.get(providerName);
        if (metrics == null) {
            return new BigDecimal("0.8"); // Default reliability score
        }
        return metrics.getReliabilityScore();
    }
    
    /**
     * Get average response time for a provider
     */
    public long getAverageResponseTime(String providerName) {
        ProviderMetrics metrics = providerMetrics.get(providerName);
        if (metrics == null) {
            return 5000L; // Default 5 seconds
        }
        return metrics.getAverageResponseTime();
    }
    
    /**
     * Get average shipping fee for a provider
     */
    public BigDecimal getAverageShippingFee(String providerName) {
        ProviderMetrics metrics = providerMetrics.get(providerName);
        if (metrics == null) {
            return BigDecimal.ZERO;
        }
        return metrics.getAverageShippingFee();
    }
    
    /**
     * Reset metrics for a provider (useful for testing or maintenance)
     */
    public void resetMetrics(String providerName) {
        providerMetrics.remove(providerName);
        log.info("Reset metrics for provider: {}", providerName);
    }
    
    /**
     * Get or create metrics for a provider
     */
    private ProviderMetrics getOrCreateMetrics(String providerName) {
        return providerMetrics.computeIfAbsent(providerName, k -> new ProviderMetrics());
    }
    
    /**
     * Internal class to track provider metrics
     */
    private static class ProviderMetrics {
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger successfulCalls = new AtomicInteger(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong totalShippingFee = new AtomicLong(0);
        private volatile LocalDateTime lastUpdated = LocalDateTime.now();
        
        void recordSuccess(long responseTimeMs, BigDecimal shippingFee) {
            totalCalls.incrementAndGet();
            successfulCalls.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            if (shippingFee != null) {
                totalShippingFee.addAndGet(shippingFee.longValue());
            }
            lastUpdated = LocalDateTime.now();
        }
        
        void recordFailure(long responseTimeMs, String errorReason) {
            totalCalls.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            lastUpdated = LocalDateTime.now();
        }
        
        BigDecimal getReliabilityScore() {
            int total = totalCalls.get();
            if (total == 0) {
                return new BigDecimal("0.8"); // Default score
            }
            
            int successful = successfulCalls.get();
            BigDecimal successRate = new BigDecimal(successful)
                .divide(new BigDecimal(total), 4, RoundingMode.HALF_UP);
            
            // Apply time decay factor (metrics older than 24 hours have reduced weight)
            LocalDateTime now = LocalDateTime.now();
            long hoursOld = java.time.Duration.between(lastUpdated, now).toHours();
            if (hoursOld > 24) {
                BigDecimal decayFactor = new BigDecimal("0.9"); // 10% reduction per day
                successRate = successRate.multiply(decayFactor);
            }
            
            return successRate.min(BigDecimal.ONE).max(new BigDecimal("0.1"));
        }
        
        long getAverageResponseTime() {
            int total = totalCalls.get();
            if (total == 0) {
                return 5000L; // Default 5 seconds
            }
            return totalResponseTime.get() / total;
        }
        
        BigDecimal getAverageShippingFee() {
            int successful = successfulCalls.get();
            if (successful == 0) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(totalShippingFee.get()).divide(new BigDecimal(successful), 0, RoundingMode.HALF_UP);
        }
    }
}
