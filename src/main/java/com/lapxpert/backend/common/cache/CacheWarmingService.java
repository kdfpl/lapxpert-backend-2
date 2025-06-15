package com.lapxpert.backend.common.cache;

import com.lapxpert.backend.danhgia.domain.service.ProductRatingCacheService;
import com.lapxpert.backend.sanpham.domain.service.SanPhamService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic cache warming service for LapXpert application
 * Implements simple cache warming strategies for critical data
 * Focuses on frequently accessed data to improve performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingService {

    private final SanPhamService sanPhamService;
    private final ProductRatingCacheService productRatingCacheService;

    private final AtomicInteger warmingCount = new AtomicInteger(0);
    private Instant lastWarmingTime = null;

    // ==================== APPLICATION STARTUP WARMING ====================

    /**
     * Warm cache on application startup
     * Uses @EventListener to ensure all beans are initialized
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("Starting cache warming on application startup");
        
        CompletableFuture.runAsync(() -> {
            try {
                warmCriticalData();
                log.info("✅ Application startup cache warming completed successfully");
            } catch (Exception e) {
                log.error("❌ Application startup cache warming failed", e);
            }
        });
    }

    /**
     * Alternative startup warming using @PostConstruct
     * Kept as backup method
     */
    @PostConstruct
    public void initializeCache() {
        log.debug("Cache warming service initialized");
    }

    // ==================== SCHEDULED WARMING ====================

    /**
     * Scheduled cache warming for critical data
     * Runs every 2 hours to refresh frequently accessed data
     */
    @Scheduled(fixedRate = 7200000) // Every 2 hours
    public void scheduledCacheWarming() {
        log.debug("Starting scheduled cache warming");
        
        try {
            warmCriticalData();
            log.debug("Scheduled cache warming completed successfully");
        } catch (Exception e) {
            log.warn("Scheduled cache warming failed", e);
        }
    }

    /**
     * Scheduled warming for product lists
     * Runs every hour to keep product data fresh
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledProductListWarming() {
        log.debug("Starting scheduled product list warming");
        
        try {
            warmProductLists();
            log.debug("Scheduled product list warming completed");
        } catch (Exception e) {
            log.warn("Scheduled product list warming failed", e);
        }
    }

    // ==================== MANUAL WARMING METHODS ====================

    /**
     * Manual cache warming for all critical data
     * Can be called by admin endpoints
     */
    public void warmAllCaches() {
        log.info("Starting manual cache warming for all data");
        
        try {
            warmCriticalData();
            warmProductLists();
            warmProductRatings();
            
            log.info("✅ Manual cache warming completed successfully");
        } catch (Exception e) {
            log.error("❌ Manual cache warming failed", e);
            throw new RuntimeException("Cache warming failed", e);
        }
    }

    /**
     * Manual warming for specific cache type
     */
    public void warmSpecificCache(String cacheType) {
        log.info("Starting manual warming for cache type: {}", cacheType);
        
        try {
            switch (cacheType.toLowerCase()) {
                case "products" -> warmProductLists();
                case "ratings" -> warmProductRatings();
                case "critical" -> warmCriticalData();
                default -> {
                    log.warn("Unknown cache type: {}", cacheType);
                    throw new IllegalArgumentException("Unknown cache type: " + cacheType);
                }
            }
            
            log.info("✅ Manual warming for {} completed", cacheType);
        } catch (Exception e) {
            log.error("❌ Manual warming for {} failed", cacheType, e);
            throw new RuntimeException("Cache warming failed for " + cacheType, e);
        }
    }

    // ==================== POST-INVALIDATION WARMING ====================

    /**
     * Warm cache after invalidation
     * Called when caches are cleared
     */
    public void warmAfterInvalidation() {
        log.info("Starting cache warming after invalidation");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Wait a bit to ensure invalidation is complete
                Thread.sleep(1000);
                
                warmCriticalData();
                log.info("✅ Post-invalidation cache warming completed");
            } catch (Exception e) {
                log.error("❌ Post-invalidation cache warming failed", e);
            }
        });
    }

    // ==================== CORE WARMING METHODS ====================

    /**
     * Warm critical data that's accessed most frequently
     */
    private void warmCriticalData() {
        log.debug("Warming critical data caches");
        
        int warmedItems = 0;
        
        try {
            // Warm active products list (most frequently accessed)
            sanPhamService.getActiveProducts();
            warmedItems++;
            log.debug("Warmed active products list");
            
            // Warm full product list
            sanPhamService.findAll();
            warmedItems++;
            log.debug("Warmed full products list");
            
            // Update warming statistics
            warmingCount.incrementAndGet();
            lastWarmingTime = Instant.now();
            
            log.debug("Critical data warming completed: {} items warmed", warmedItems);
            
        } catch (Exception e) {
            log.error("Error warming critical data", e);
            throw e;
        }
    }

    /**
     * Warm product lists specifically
     */
    private void warmProductLists() {
        log.debug("Warming product list caches");
        
        try {
            // Warm both product list caches
            sanPhamService.findAll();
            sanPhamService.getActiveProducts();
            
            log.debug("Product list warming completed");
            
        } catch (Exception e) {
            log.error("Error warming product lists", e);
            throw e;
        }
    }

    /**
     * Warm product ratings for popular products
     */
    private void warmProductRatings() {
        log.debug("Warming product rating caches");
        
        try {
            // Use existing warming method from ProductRatingCacheService
            productRatingCacheService.warmUpCache();
            
            log.debug("Product rating warming completed");
            
        } catch (Exception e) {
            log.error("Error warming product ratings", e);
            throw e;
        }
    }

    // ==================== MONITORING AND STATISTICS ====================

    /**
     * Get cache warming statistics
     */
    public CacheWarmingStats getWarmingStats() {
        return new CacheWarmingStats(
            warmingCount.get(),
            lastWarmingTime
        );
    }

    /**
     * Cache warming statistics class
     */
    public static class CacheWarmingStats {
        private final int totalWarmingCount;
        private final Instant lastWarmingTime;

        public CacheWarmingStats(int totalWarmingCount, Instant lastWarmingTime) {
            this.totalWarmingCount = totalWarmingCount;
            this.lastWarmingTime = lastWarmingTime;
        }

        public int getTotalWarmingCount() { return totalWarmingCount; }
        public Instant getLastWarmingTime() { return lastWarmingTime; }
    }
}
