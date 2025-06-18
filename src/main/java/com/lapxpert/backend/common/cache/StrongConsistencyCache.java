package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Strong Consistency Cache for LapXpert E-commerce System
 * 
 * Implements write-through pattern with optimistic locking for critical data
 * (inventory, pricing, orders, vouchers). Ensures immediate synchronization
 * between cache and database to prevent data inconsistencies.
 * 
 * Vietnamese Business Context:
 * - Tồn kho: Inventory management with strong consistency
 * - Giá cả: Pricing data requiring immediate updates
 * - Đơn hàng: Order processing with transaction safety
 * - Voucher: Voucher usage tracking with conflict prevention
 * 
 * Features:
 * - Write-through pattern for immediate consistency
 * - Optimistic locking with Redis WATCH/MULTI/EXEC
 * - Distributed locking with Redisson for critical operations
 * - Automatic conflict detection and resolution
 */
@Component
@Slf4j
public class StrongConsistencyCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheVersioningService versioningService;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    // Lock timeout for critical operations
    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final long LOCK_WAIT_SECONDS = 5;

    /**
     * Write-through cache operation with strong consistency
     * Synchronously updates both cache and database
     */
    @Transactional
    public <T> T writeThrough(String cacheKey, T data, Supplier<T> databaseWriter) {
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Acquire distributed lock for critical operation
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    log.debug("Acquired lock for write-through operation on key '{}'", cacheKey);
                    
                    // Step 1: Write to database first (write-through pattern)
                    T result = databaseWriter.get();
                    if (result == null) {
                        throw new RuntimeException("Database write operation returned null");
                    }
                    
                    // Step 2: Update cache with versioned entry
                    versioningService.storeVersionedData(cacheKey, result, "WRITE_THROUGH");
                    
                    // Step 3: Record consistency check
                    consistencyManager.recordConsistencyCheck(cacheKey);
                    
                    log.debug("Write-through operation completed successfully for key '{}'", cacheKey);
                    return result;
                    
                } finally {
                    lock.unlock();
                    log.debug("Released lock for write-through operation on key '{}'", cacheKey);
                }
            } else {
                log.warn("Failed to acquire lock for write-through operation on key '{}' within {} seconds", 
                    cacheKey, LOCK_WAIT_SECONDS);
                throw new RuntimeException("Unable to acquire lock for critical cache operation");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Write-through operation interrupted for key '{}'", cacheKey, e);
            throw new RuntimeException("Write-through operation interrupted", e);
        } catch (Exception e) {
            log.error("Write-through operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            throw new RuntimeException("Write-through operation failed", e);
        }
    }

    /**
     * Read-through cache operation with consistency validation
     */
    public <T> T readThrough(String cacheKey, Class<T> dataType, Supplier<T> databaseReader) {
        try {
            // Step 1: Try to get versioned data from cache
            CacheVersioningService.VersionedCacheEntry<T> cachedEntry = 
                versioningService.getVersionedData(cacheKey, dataType);
            
            if (cachedEntry != null && cachedEntry.getData() != null) {
                // Step 2: Validate cache freshness for critical data
                if (isCacheFresh(cachedEntry)) {
                    log.debug("Cache hit for key '{}' with version {}", cacheKey, cachedEntry.getVersion());
                    return cachedEntry.getData();
                } else {
                    log.debug("Cache entry stale for key '{}', refreshing from database", cacheKey);
                }
            }
            
            // Step 3: Cache miss or stale data - read from database
            T data = databaseReader.get();
            if (data != null) {
                // Step 4: Update cache with fresh data
                versioningService.storeVersionedData(cacheKey, data, "READ_THROUGH");
                log.debug("Cache refreshed for key '{}' from database", cacheKey);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("Read-through operation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            // Fallback to database on cache failure
            return databaseReader.get();
        }
    }

    /**
     * Optimistic update with conflict detection
     */
    public <T> boolean optimisticUpdate(String cacheKey, T newData, Supplier<T> databaseUpdater) {
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // Step 1: Get current version
                    Instant currentVersion = versioningService.getCacheVersion(cacheKey);
                    
                    // Step 2: Update database
                    T result = databaseUpdater.get();
                    if (result == null) {
                        return false;
                    }
                    
                    // Step 3: Check for conflicts and update cache
                    boolean updated = versioningService.updateVersionedData(cacheKey, result, "OPTIMISTIC_UPDATE");
                    
                    if (updated) {
                        consistencyManager.recordConsistencyCheck(cacheKey);
                        log.debug("Optimistic update successful for key '{}'", cacheKey);
                    } else {
                        log.warn("Optimistic update failed due to conflict for key '{}'", cacheKey);
                    }
                    
                    return updated;
                    
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for optimistic update on key '{}'", cacheKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Optimistic update interrupted for key '{}'", cacheKey, e);
            return false;
        } catch (Exception e) {
            log.error("Optimistic update failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Invalidate cache entry with strong consistency guarantee
     */
    @Transactional
    public void invalidateWithConsistency(String cacheKey, Runnable databaseInvalidator) {
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // Step 1: Invalidate database state if needed
                    if (databaseInvalidator != null) {
                        databaseInvalidator.run();
                    }
                    
                    // Step 2: Remove from cache
                    versioningService.invalidateVersionedEntry(cacheKey);
                    
                    // Step 3: Record consistency check
                    consistencyManager.recordConsistencyCheck(cacheKey);
                    
                    log.debug("Strong consistency invalidation completed for key '{}'", cacheKey);
                    
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for invalidation on key '{}'", cacheKey);
                throw new RuntimeException("Unable to acquire lock for cache invalidation");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Cache invalidation interrupted for key '{}'", cacheKey, e);
            throw new RuntimeException("Cache invalidation interrupted", e);
        } catch (Exception e) {
            log.error("Cache invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            throw new RuntimeException("Cache invalidation failed", e);
        }
    }

    /**
     * Check if cached entry is fresh enough for strong consistency
     */
    private <T> boolean isCacheFresh(CacheVersioningService.VersionedCacheEntry<T> entry) {
        if (entry == null || entry.getVersion() == null) {
            return false;
        }
        
        // For strong consistency, data should be very fresh (within 5 minutes)
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        return entry.getVersion().isAfter(fiveMinutesAgo);
    }

    /**
     * Validate cache consistency across multiple keys
     */
    public boolean validateConsistency(String... cacheKeys) {
        boolean allConsistent = true;
        
        for (String cacheKey : cacheKeys) {
            try {
                Instant lastCheck = consistencyManager.getLastConsistencyCheck(cacheKey);
                if (lastCheck == null || lastCheck.isBefore(Instant.now().minusSeconds(300))) {
                    log.warn("Cache consistency validation failed for key '{}' - last check: {}", cacheKey, lastCheck);
                    allConsistent = false;
                }
            } catch (Exception e) {
                log.error("Error validating consistency for key '{}': {}", cacheKey, e.getMessage(), e);
                allConsistent = false;
            }
        }
        
        return allConsistent;
    }

    /**
     * Force consistency check and refresh if needed
     */
    public <T> void forceConsistencyCheck(String cacheKey, Class<T> dataType, Supplier<T> databaseReader) {
        try {
            log.debug("Forcing consistency check for key '{}'", cacheKey);
            
            // Get current cached version
            CacheVersioningService.VersionedCacheEntry<T> cachedEntry = 
                versioningService.getVersionedData(cacheKey, dataType);
            
            // Always refresh from database for strong consistency
            T freshData = databaseReader.get();
            if (freshData != null) {
                versioningService.storeVersionedData(cacheKey, freshData, "CONSISTENCY_CHECK");
                consistencyManager.recordConsistencyCheck(cacheKey);
                log.debug("Consistency check completed and cache refreshed for key '{}'", cacheKey);
            }
            
        } catch (Exception e) {
            log.error("Force consistency check failed for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }
}
