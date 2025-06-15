package com.lapxpert.backend.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic cache invalidation service for LapXpert application
 * Implements simple, maintainable cache invalidation strategies
 * Focuses on service-level invalidation and manual clearing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheWarmingService cacheWarmingService;

    private final AtomicInteger invalidationCount = new AtomicInteger(0);
    private Instant lastInvalidationTime = null;

    // ==================== SINGLE CACHE INVALIDATION ====================

    /**
     * Invalidate specific cache by name
     * @param cacheName name of the cache to invalidate
     */
    public void invalidateCache(String cacheName) {
        try {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
                log.debug("Invalidated cache: {}", cacheName);
                updateInvalidationStats();
            } else {
                log.warn("Cache not found: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache: {}", cacheName, e);
            throw new RuntimeException("Cache invalidation failed for: " + cacheName, e);
        }
    }

    /**
     * Invalidate specific cache entry by key
     * @param cacheName name of the cache
     * @param key cache key to invalidate
     */
    public void invalidateCacheEntry(String cacheName, Object key) {
        try {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).evict(key);
                log.debug("Invalidated cache entry: {} from cache: {}", key, cacheName);
                updateInvalidationStats();
            } else {
                log.warn("Cache not found: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache entry: {} from cache: {}", key, cacheName, e);
            throw new RuntimeException("Cache entry invalidation failed", e);
        }
    }

    // ==================== BULK INVALIDATION ====================

    /**
     * Invalidate all caches
     */
    @CacheEvict(value = {"productRatings", "sanPhamList", "activeSanPhamList", 
                         "searchResults", "popularProducts", "userSessions", 
                         "cartData", "categories", "systemConfig"}, allEntries = true)
    public void invalidateAllCaches() {
        try {
            log.info("Starting invalidation of all caches");
            
            Collection<String> cacheNames = cacheManager.getCacheNames();
            int invalidatedCount = 0;
            
            for (String cacheName : cacheNames) {
                try {
                    if (cacheManager.getCache(cacheName) != null) {
                        cacheManager.getCache(cacheName).clear();
                        invalidatedCount++;
                        log.debug("Cleared cache: {}", cacheName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to clear cache: {}", cacheName, e);
                }
            }
            
            updateInvalidationStats();
            log.info("✅ All caches invalidated successfully: {} caches cleared", invalidatedCount);
            
            // Trigger post-invalidation warming
            cacheWarmingService.warmAfterInvalidation();
            
        } catch (Exception e) {
            log.error("❌ Error during bulk cache invalidation", e);
            throw new RuntimeException("Bulk cache invalidation failed", e);
        }
    }

    /**
     * Invalidate product-related caches
     */
    @CacheEvict(value = {"sanPhamList", "activeSanPhamList", "productRatings", "popularProducts"}, allEntries = true)
    public void invalidateProductCaches() {
        try {
            log.info("Invalidating product-related caches");
            updateInvalidationStats();
            log.info("✅ Product caches invalidated successfully");
        } catch (Exception e) {
            log.error("❌ Error invalidating product caches", e);
            throw new RuntimeException("Product cache invalidation failed", e);
        }
    }

    /**
     * Invalidate user session caches
     */
    @CacheEvict(value = {"userSessions", "cartData"}, allEntries = true)
    public void invalidateUserCaches() {
        try {
            log.info("Invalidating user-related caches");
            updateInvalidationStats();
            log.info("✅ User caches invalidated successfully");
        } catch (Exception e) {
            log.error("❌ Error invalidating user caches", e);
            throw new RuntimeException("User cache invalidation failed", e);
        }
    }

    // ==================== PATTERN-BASED INVALIDATION ====================

    /**
     * Invalidate caches by pattern using Redis keys command
     * @param pattern Redis key pattern (e.g., "product:*")
     */
    public void invalidateByPattern(String pattern) {
        try {
            log.debug("Invalidating caches by pattern: {}", pattern);
            
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache entries matching pattern: {}", keys.size(), pattern);
                updateInvalidationStats();
            } else {
                log.debug("No cache entries found for pattern: {}", pattern);
            }
            
        } catch (Exception e) {
            log.error("Error invalidating caches by pattern: {}", pattern, e);
            throw new RuntimeException("Pattern-based invalidation failed", e);
        }
    }

    // ==================== SERVICE-SPECIFIC INVALIDATION ====================

    /**
     * Invalidate caches when product data changes
     * @param productId specific product ID (optional)
     */
    public void invalidateProductData(Long productId) {
        try {
            log.debug("Invalidating product data caches for product: {}", productId);
            
            // Invalidate product lists
            invalidateCache("sanPhamList");
            invalidateCache("activeSanPhamList");
            
            // Invalidate specific product rating if productId provided
            if (productId != null) {
                invalidateCacheEntry("productRatings", productId);
            }
            
            // Invalidate search results that might contain this product
            invalidateCache("searchResults");
            
            log.debug("✅ Product data caches invalidated for product: {}", productId);
            
        } catch (Exception e) {
            log.error("❌ Error invalidating product data caches", e);
            throw new RuntimeException("Product data invalidation failed", e);
        }
    }

    /**
     * Invalidate caches when review data changes
     * @param productId product ID for the review
     */
    public void invalidateReviewData(Long productId) {
        try {
            log.debug("Invalidating review data caches for product: {}", productId);
            
            // Invalidate product rating cache for this product
            if (productId != null) {
                invalidateCacheEntry("productRatings", productId);
            }
            
            // Invalidate popular products as ratings might affect popularity
            invalidateCache("popularProducts");
            
            log.debug("✅ Review data caches invalidated for product: {}", productId);
            
        } catch (Exception e) {
            log.error("❌ Error invalidating review data caches", e);
            throw new RuntimeException("Review data invalidation failed", e);
        }
    }

    // ==================== TTL-BASED EXPIRATION MONITORING ====================

    /**
     * Check cache TTL status for monitoring
     * @param cacheName cache name to check
     * @return TTL information
     */
    public CacheTtlInfo getCacheTtlInfo(String cacheName) {
        try {
            // For Redis-based caches, we can check TTL using Redis commands
            Set<String> keys = redisTemplate.keys(cacheName + ":*");
            int totalKeys = keys != null ? keys.size() : 0;
            
            return new CacheTtlInfo(cacheName, totalKeys, Instant.now());
            
        } catch (Exception e) {
            log.debug("Error checking TTL for cache: {}", cacheName, e);
            return new CacheTtlInfo(cacheName, 0, Instant.now());
        }
    }

    // ==================== STATISTICS AND MONITORING ====================

    /**
     * Get invalidation statistics
     */
    public CacheInvalidationStats getInvalidationStats() {
        return new CacheInvalidationStats(
            invalidationCount.get(),
            lastInvalidationTime
        );
    }

    /**
     * Update invalidation statistics
     */
    private void updateInvalidationStats() {
        invalidationCount.incrementAndGet();
        lastInvalidationTime = Instant.now();
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Cache TTL information class
     */
    public static class CacheTtlInfo {
        private final String cacheName;
        private final int keyCount;
        private final Instant checkTime;

        public CacheTtlInfo(String cacheName, int keyCount, Instant checkTime) {
            this.cacheName = cacheName;
            this.keyCount = keyCount;
            this.checkTime = checkTime;
        }

        public String getCacheName() { return cacheName; }
        public int getKeyCount() { return keyCount; }
        public Instant getCheckTime() { return checkTime; }
    }

    /**
     * Cache invalidation statistics class
     */
    public static class CacheInvalidationStats {
        private final int totalInvalidationCount;
        private final Instant lastInvalidationTime;

        public CacheInvalidationStats(int totalInvalidationCount, Instant lastInvalidationTime) {
            this.totalInvalidationCount = totalInvalidationCount;
            this.lastInvalidationTime = lastInvalidationTime;
        }

        public int getTotalInvalidationCount() { return totalInvalidationCount; }
        public Instant getLastInvalidationTime() { return lastInvalidationTime; }
    }
}
