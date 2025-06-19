package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Eventual Consistency Cache for LapXpert E-commerce System
 * 
 * Implements cache-aside pattern with intelligent TTL management for non-critical data
 * (product descriptions, categories, user profiles). Prioritizes performance over
 * immediate consistency, allowing for eventual synchronization.
 * 
 * Vietnamese Business Context:
 * - Mô tả sản phẩm: Product descriptions with relaxed consistency
 * - Danh mục: Categories with longer TTL
 * - Hồ sơ người dùng: User profiles with eventual updates
 * - Kết quả tìm kiếm: Search results with performance priority
 * 
 * Features:
 * - Cache-aside pattern for optimal performance
 * - Intelligent TTL based on data access patterns
 * - Lazy loading with background refresh
 * - Graceful degradation on cache failures
 */
@Component
@Slf4j
public class EventualConsistencyCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheVersioningService versioningService;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    // TTL configurations for different data types
    private static final long SHORT_TTL_MINUTES = 15;   // Frequently changing data
    private static final long MEDIUM_TTL_MINUTES = 60;  // Moderately changing data
    private static final long LONG_TTL_MINUTES = 1440;  // Rarely changing data

    /**
     * Cache-aside read operation with lazy loading
     */
    public <T> T cacheAside(String cacheKey, Class<T> dataType, Supplier<T> databaseReader) {
        try {
            // Step 1: Try to get data from cache
            CacheVersioningService.VersionedCacheEntry<T> cachedEntry = 
                versioningService.getVersionedData(cacheKey, dataType);
            
            if (cachedEntry != null && cachedEntry.getData() != null) {
                log.debug("Cache hit for key '{}' with version {}", cacheKey, cachedEntry.getVersion());
                return cachedEntry.getData();
            }
            
            // Step 2: Cache miss - load from database
            log.debug("Cache miss for key '{}', loading from database", cacheKey);
            T data = databaseReader.get();
            
            if (data != null) {
                // Step 3: Store in cache with appropriate TTL
                storeWithIntelligentTTL(cacheKey, data);
                log.debug("Data loaded and cached for key '{}'", cacheKey);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("Cache-aside operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Graceful degradation - return data from database
            return databaseReader.get();
        }
    }

    /**
     * Lazy update with eventual consistency
     */
    public <T> void lazyUpdate(String cacheKey, T newData) {
        try {
            // For eventual consistency, we can update cache without immediate database sync
            storeWithIntelligentTTL(cacheKey, newData);
            log.debug("Lazy update completed for key '{}'", cacheKey);
            
        } catch (Exception e) {
            log.error("Lazy update failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Non-critical failure - log and continue
        }
    }

    /**
     * Background refresh for frequently accessed data
     */
    public <T> void backgroundRefresh(String cacheKey, Class<T> dataType, Supplier<T> databaseReader) {
        try {
            // Check if cache entry is approaching expiration
            CacheVersioningService.VersionedCacheEntry<T> cachedEntry = 
                versioningService.getVersionedData(cacheKey, dataType);
            
            if (shouldRefreshInBackground(cachedEntry)) {
                log.debug("Background refresh triggered for key '{}'", cacheKey);
                
                // Refresh data from database
                T freshData = databaseReader.get();
                if (freshData != null) {
                    storeWithIntelligentTTL(cacheKey, freshData);
                    log.debug("Background refresh completed for key '{}'", cacheKey);
                }
            }
            
        } catch (Exception e) {
            log.error("Background refresh failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Non-critical failure - continue operation
        }
    }

    /**
     * Invalidate cache entry (eventual consistency)
     */
    public void invalidateEventually(String cacheKey) {
        try {
            // For eventual consistency, we can invalidate immediately
            versioningService.invalidateVersionedEntry(cacheKey);
            log.debug("Cache entry invalidated for key '{}'", cacheKey);
            
        } catch (Exception e) {
            log.error("Cache invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Non-critical failure
        }
    }

    /**
     * Bulk cache operations for efficiency
     */
    public <T> void bulkStore(java.util.Map<String, T> dataMap) {
        try {
            dataMap.forEach((key, value) -> {
                try {
                    storeWithIntelligentTTL(key, value);
                } catch (Exception e) {
                    log.warn("Failed to store bulk data for key '{}': {}", key, e.getMessage());
                }
            });
            
            log.debug("Bulk store operation completed for {} items", dataMap.size());
            
        } catch (Exception e) {
            log.error("Bulk store operation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Warm up cache with frequently accessed data
     */
    public <T> void warmUpCache(String cacheKey, Supplier<T> dataLoader) {
        try {
            // Check if cache is empty or stale
            @SuppressWarnings("unchecked")
            CacheVersioningService.VersionedCacheEntry<T> cachedEntry =
                (CacheVersioningService.VersionedCacheEntry<T>) versioningService.getVersionedData(cacheKey, Object.class);
            
            if (cachedEntry == null || isStaleForWarmup(cachedEntry)) {
                log.debug("Warming up cache for key '{}'", cacheKey);
                
                T data = dataLoader.get();
                if (data != null) {
                    storeWithIntelligentTTL(cacheKey, data);
                    log.debug("Cache warmed up for key '{}'", cacheKey);
                }
            }
            
        } catch (Exception e) {
            log.error("Cache warmup failed for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Store data with intelligent TTL based on cache key patterns
     */
    private <T> void storeWithIntelligentTTL(String cacheKey, T data) {
        long ttlMinutes = determineTTL(cacheKey);
        versioningService.storeVersionedData(cacheKey, data, "EVENTUAL_CONSISTENCY");
        
        // Set additional TTL on the Redis key for automatic cleanup
        redisTemplate.expire(cacheKey, ttlMinutes, TimeUnit.MINUTES);
        
        log.debug("Stored data for key '{}' with TTL {} minutes", cacheKey, ttlMinutes);
    }

    /**
     * Determine appropriate TTL based on cache key patterns
     */
    private long determineTTL(String cacheKey) {
        // Short TTL for frequently changing data
        if (cacheKey.contains("search") || cacheKey.contains("cart") || cacheKey.contains("active")) {
            return SHORT_TTL_MINUTES;
        }
        
        // Long TTL for rarely changing data
        if (cacheKey.contains("categories") || cacheKey.contains("config") || cacheKey.contains("popular")) {
            return LONG_TTL_MINUTES;
        }
        
        // Medium TTL for moderately changing data (default)
        return MEDIUM_TTL_MINUTES;
    }

    /**
     * Check if cache entry should be refreshed in background
     */
    private <T> boolean shouldRefreshInBackground(CacheVersioningService.VersionedCacheEntry<T> entry) {
        if (entry == null || entry.getVersion() == null) {
            return true;
        }
        
        // Refresh if data is older than 75% of its expected TTL
        Instant refreshThreshold = Instant.now().minusSeconds(45 * 60); // 45 minutes
        return entry.getVersion().isBefore(refreshThreshold);
    }

    /**
     * Check if cache entry is stale enough to warrant warmup
     */
    private <T> boolean isStaleForWarmup(CacheVersioningService.VersionedCacheEntry<T> entry) {
        if (entry == null || entry.getVersion() == null) {
            return true;
        }
        
        // Consider stale if older than 2 hours
        Instant staleThreshold = Instant.now().minusSeconds(2 * 60 * 60);
        return entry.getVersion().isBefore(staleThreshold);
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats(String cacheKey) {
        try {
            CacheVersioningService.VersionedCacheEntry<?> entry = 
                versioningService.getVersionedData(cacheKey, Object.class);
            
            if (entry != null) {
                return new CacheStats(
                    cacheKey,
                    entry.getVersion(),
                    entry.getSource(),
                    true,
                    determineTTL(cacheKey)
                );
            } else {
                return new CacheStats(cacheKey, null, null, false, 0);
            }
            
        } catch (Exception e) {
            log.error("Failed to get cache stats for key '{}': {}", cacheKey, e.getMessage(), e);
            return new CacheStats(cacheKey, null, null, false, 0);
        }
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStats {
        public final String cacheKey;
        public final Instant version;
        public final String source;
        public final boolean exists;
        public final long ttlMinutes;

        public CacheStats(String cacheKey, Instant version, String source, boolean exists, long ttlMinutes) {
            this.cacheKey = cacheKey;
            this.version = version;
            this.source = source;
            this.exists = exists;
            this.ttlMinutes = ttlMinutes;
        }
    }
}
