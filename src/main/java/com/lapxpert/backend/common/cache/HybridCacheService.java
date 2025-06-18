package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Hybrid Cache Service for LapXpert E-commerce System
 * 
 * Provides a unified interface for accessing both strong and eventual consistency
 * caching patterns. Automatically routes cache operations based on data criticality
 * classification from CacheConsistencyManager.
 * 
 * Vietnamese Business Context:
 * - Dịch vụ cache lai: Hybrid caching service for optimal performance
 * - Tự động định tuyến: Automatic routing based on data criticality
 * - Tối ưu hiệu suất: Performance optimization with consistency guarantees
 * 
 * Usage Examples:
 * - Critical data (inventory, pricing): Uses StrongConsistencyCache
 * - Non-critical data (descriptions, categories): Uses EventualConsistencyCache
 * - Automatic strategy selection based on cache key patterns
 */
@Service
@Slf4j
public class HybridCacheService {

    @Autowired
    private CacheConsistencyManager consistencyManager;

    @Autowired
    private StrongConsistencyCache strongConsistencyCache;

    @Autowired
    private EventualConsistencyCache eventualConsistencyCache;

    @Autowired
    private ImmediateInvalidationStrategy immediateInvalidationStrategy;

    @Autowired
    private EventualInvalidationStrategy eventualInvalidationStrategy;

    /**
     * Get data from cache with automatic consistency level selection
     */
    public <T> T get(String cacheKey, Class<T> dataType, Supplier<T> databaseReader) {
        try {
            if (consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.debug("Using strong consistency cache for key '{}'", cacheKey);
                return strongConsistencyCache.readThrough(cacheKey, dataType, databaseReader);
            } else {
                log.debug("Using eventual consistency cache for key '{}'", cacheKey);
                return eventualConsistencyCache.cacheAside(cacheKey, dataType, databaseReader);
            }
        } catch (Exception e) {
            log.error("Hybrid cache get operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Fallback to database
            return databaseReader.get();
        }
    }

    /**
     * Store data in cache with automatic consistency level selection
     */
    public <T> T put(String cacheKey, T data, Supplier<T> databaseWriter) {
        try {
            if (consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.debug("Using strong consistency write-through for key '{}'", cacheKey);
                return strongConsistencyCache.writeThrough(cacheKey, data, databaseWriter);
            } else {
                log.debug("Using eventual consistency lazy update for key '{}'", cacheKey);
                // For eventual consistency, write to database first, then update cache
                T result = databaseWriter.get();
                if (result != null) {
                    eventualConsistencyCache.lazyUpdate(cacheKey, result);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Hybrid cache put operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Fallback to database only
            return databaseWriter.get();
        }
    }

    /**
     * Update data with automatic consistency level selection
     */
    public <T> boolean update(String cacheKey, T newData, Supplier<T> databaseUpdater) {
        try {
            if (consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.debug("Using strong consistency optimistic update for key '{}'", cacheKey);
                return strongConsistencyCache.optimisticUpdate(cacheKey, newData, databaseUpdater);
            } else {
                log.debug("Using eventual consistency update for key '{}'", cacheKey);
                // For eventual consistency, update database then cache
                T result = databaseUpdater.get();
                if (result != null) {
                    eventualConsistencyCache.lazyUpdate(cacheKey, result);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Hybrid cache update operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Invalidate cache entry with automatic strategy selection
     */
    public boolean invalidate(String cacheKey) {
        try {
            if (consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.debug("Using immediate invalidation for critical key '{}'", cacheKey);
                return immediateInvalidationStrategy.invalidate(cacheKey);
            } else {
                log.debug("Using eventual invalidation for non-critical key '{}'", cacheKey);
                return eventualInvalidationStrategy.invalidate(cacheKey);
            }
        } catch (Exception e) {
            log.error("Hybrid cache invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Warm up cache for frequently accessed data
     */
    public <T> void warmUp(String cacheKey, Supplier<T> dataLoader) {
        try {
            if (consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.debug("Warming up critical cache for key '{}'", cacheKey);
                strongConsistencyCache.forceConsistencyCheck(cacheKey, Object.class, () -> dataLoader.get());
            } else {
                log.debug("Warming up non-critical cache for key '{}'", cacheKey);
                eventualConsistencyCache.warmUpCache(cacheKey, dataLoader);
            }
        } catch (Exception e) {
            log.error("Cache warmup failed for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheOperationResult getCacheStats(String cacheKey) {
        try {
            boolean isCritical = consistencyManager.requiresStrongConsistency(cacheKey);
            CacheConsistencyManager.ConsistencyLevel level = consistencyManager.getConsistencyLevel(cacheKey);
            
            if (isCritical) {
                boolean isConsistent = strongConsistencyCache.validateConsistency(cacheKey);
                return new CacheOperationResult(cacheKey, level, isConsistent, "Strong consistency validation");
            } else {
                EventualConsistencyCache.CacheStats stats = eventualConsistencyCache.getCacheStats(cacheKey);
                return new CacheOperationResult(cacheKey, level, stats.exists, 
                    String.format("TTL: %d minutes, Version: %s", stats.ttlMinutes, stats.version));
            }
        } catch (Exception e) {
            log.error("Failed to get cache stats for key '{}': {}", cacheKey, e.getMessage(), e);
            return new CacheOperationResult(cacheKey, 
                consistencyManager.getConsistencyLevel(cacheKey), false, "Error: " + e.getMessage());
        }
    }

    /**
     * Cache operation result data class
     */
    public static class CacheOperationResult {
        public final String cacheKey;
        public final CacheConsistencyManager.ConsistencyLevel consistencyLevel;
        public final boolean success;
        public final String details;

        public CacheOperationResult(String cacheKey, CacheConsistencyManager.ConsistencyLevel consistencyLevel, 
                                  boolean success, String details) {
            this.cacheKey = cacheKey;
            this.consistencyLevel = consistencyLevel;
            this.success = success;
            this.details = details;
        }
    }

    // ==================== BUSINESS-SPECIFIC HELPER METHODS ====================

    /**
     * Cache inventory data with strong consistency
     */
    public <T> T cacheInventoryData(String productId, Class<T> dataType, Supplier<T> inventoryReader) {
        String cacheKey = "inventory:" + productId;
        return get(cacheKey, dataType, inventoryReader);
    }

    /**
     * Cache pricing data with strong consistency
     */
    public <T> T cachePricingData(String variantId, Class<T> dataType, Supplier<T> pricingReader) {
        String cacheKey = "productPricing:" + variantId;
        return get(cacheKey, dataType, pricingReader);
    }

    /**
     * Cache product information with eventual consistency
     */
    public <T> T cacheProductInfo(String productId, Class<T> dataType, Supplier<T> productReader) {
        String cacheKey = "sanPhamList:" + productId;
        return get(cacheKey, dataType, productReader);
    }

    /**
     * Cache user session with eventual consistency
     */
    public <T> T cacheUserSession(String userId, Class<T> dataType, Supplier<T> sessionReader) {
        String cacheKey = "userSessions:" + userId;
        return get(cacheKey, dataType, sessionReader);
    }

    /**
     * Invalidate all product-related caches
     */
    public void invalidateProductCaches(String productId) {
        // Invalidate critical pricing data immediately
        invalidate("productPricing:" + productId);
        invalidate("inventory:" + productId);
        
        // Invalidate non-critical product info eventually
        invalidate("sanPhamList:" + productId);
        invalidate("productRatings:" + productId);
    }

    /**
     * Invalidate all order-related caches
     */
    public void invalidateOrderCaches(String orderId) {
        // Critical order data requires immediate invalidation
        invalidate("orderProcessing:" + orderId);
        invalidate("paymentStatus:" + orderId);
        
        // Update inventory caches that might be affected
        // This would typically be called with specific product IDs
        log.debug("Order cache invalidation completed for order '{}'", orderId);
    }

    /**
     * Validate system-wide cache consistency
     */
    public boolean validateSystemConsistency() {
        try {
            // Check critical caches
            boolean criticalConsistent = consistencyManager.getCriticalCaches().stream()
                .allMatch(cacheKey -> {
                    try {
                        return strongConsistencyCache.validateConsistency(cacheKey);
                    } catch (Exception e) {
                        log.warn("Consistency validation failed for critical cache '{}': {}", cacheKey, e.getMessage());
                        return false;
                    }
                });

            if (criticalConsistent) {
                log.info("System-wide cache consistency validation: PASSED");
            } else {
                log.warn("System-wide cache consistency validation: FAILED - some critical caches are inconsistent");
            }

            return criticalConsistent;

        } catch (Exception e) {
            log.error("System-wide consistency validation failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
