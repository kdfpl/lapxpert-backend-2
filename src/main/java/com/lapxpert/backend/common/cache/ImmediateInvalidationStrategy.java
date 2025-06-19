package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immediate Cache Invalidation Strategy for LapXpert E-commerce System
 * 
 * Implements synchronous cache invalidation for critical data requiring
 * immediate consistency (inventory, pricing, orders, vouchers).
 * Uses distributed locking to ensure atomic invalidation operations.
 * 
 * Vietnamese Business Context:
 * - Vô hiệu hóa ngay lập tức: Immediate invalidation for critical data
 * - Tồn kho: Inventory data requiring immediate consistency
 * - Giá cả: Pricing data with real-time requirements
 * - Đơn hàng: Order data with transaction safety
 */
@Component
@Slf4j
public class ImmediateInvalidationStrategy implements CacheInvalidationStrategy {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    @Autowired
    private CacheVersioningService versioningService;

    private static final long LOCK_TIMEOUT_SECONDS = 5;
    private static final long LOCK_WAIT_SECONDS = 3;

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.IMMEDIATE;
    }

    @Override
    public boolean invalidate(String cacheKey) {
        if (!isApplicable(cacheKey)) {
            log.debug("Immediate invalidation not applicable for key '{}'", cacheKey);
            return false;
        }

        String lockKey = "invalidation_lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(LOCK_WAIT_SECONDS, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    log.debug("Acquired lock for immediate invalidation of key '{}'", cacheKey);

                    // Step 1: Remove versioned cache entry
                    versioningService.invalidateVersionedEntry(cacheKey);

                    // Step 2: Remove any additional Redis keys
                    Boolean deleted = redisTemplate.delete(cacheKey);

                    // Step 3: Record consistency check
                    consistencyManager.recordConsistencyCheck(cacheKey);

                    log.debug("Immediate invalidation completed for key '{}', deleted: {}", cacheKey, deleted);
                    return Boolean.TRUE.equals(deleted);

                } finally {
                    lock.unlock();
                    log.debug("Released lock for immediate invalidation of key '{}'", cacheKey);
                }
            } else {
                log.warn("Failed to acquire lock for immediate invalidation of key '{}' within {} seconds", 
                    cacheKey, LOCK_WAIT_SECONDS);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Immediate invalidation interrupted for key '{}'", cacheKey, e);
            return false;
        } catch (Exception e) {
            log.error("Immediate invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int invalidateMultiple(Set<String> cacheKeys) {
        AtomicInteger successCount = new AtomicInteger(0);

        // Filter keys applicable for immediate invalidation
        Set<String> applicableKeys = cacheKeys.stream()
            .filter(this::isApplicable)
            .collect(java.util.stream.Collectors.toSet());

        if (applicableKeys.isEmpty()) {
            log.debug("No keys applicable for immediate invalidation");
            return 0;
        }

        log.debug("Starting immediate invalidation for {} keys", applicableKeys.size());

        // Process each key with individual locking for safety
        applicableKeys.parallelStream().forEach(cacheKey -> {
            if (invalidate(cacheKey)) {
                successCount.incrementAndGet();
            }
        });

        int successful = successCount.get();
        log.debug("Immediate invalidation completed: {}/{} keys successfully invalidated", 
            successful, applicableKeys.size());

        return successful;
    }

    @Override
    public int invalidateByPattern(String pattern) {
        try {
            log.debug("Starting immediate pattern-based invalidation for pattern '{}'", pattern);

            // Get keys matching the pattern
            Set<String> matchingKeys = redisTemplate.keys(pattern);
            if (matchingKeys == null || matchingKeys.isEmpty()) {
                log.debug("No keys found matching pattern '{}'", pattern);
                return 0;
            }

            // Filter for critical keys only
            Set<String> criticalKeys = matchingKeys.stream()
                .filter(this::isApplicable)
                .collect(java.util.stream.Collectors.toSet());

            if (criticalKeys.isEmpty()) {
                log.debug("No critical keys found matching pattern '{}'", pattern);
                return 0;
            }

            // Invalidate critical keys immediately
            return invalidateMultiple(criticalKeys);

        } catch (Exception e) {
            log.error("Immediate pattern-based invalidation failed for pattern '{}': {}", pattern, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean supportsPatternInvalidation() {
        return true;
    }

    @Override
    public boolean supportsBulkOperations() {
        return true;
    }

    @Override
    public long getEstimatedInvalidationTime() {
        // Immediate invalidation with locking - higher latency but guaranteed consistency
        return 100; // milliseconds
    }

    @Override
    public boolean isApplicable(String cacheKey) {
        // Only apply immediate invalidation to critical caches
        return consistencyManager.requiresStrongConsistency(cacheKey) ||
               isCriticalBusinessData(cacheKey);
    }

    /**
     * Check if cache key represents critical business data
     */
    private boolean isCriticalBusinessData(String cacheKey) {
        return cacheKey.contains("inventory") ||
               cacheKey.contains("stock") ||
               cacheKey.contains("pricing") ||
               cacheKey.contains("order") ||
               cacheKey.contains("voucher") ||
               cacheKey.contains("payment") ||
               cacheKey.contains("dotGiamGia") ||
               cacheKey.contains("phieuGiamGia") ||
               cacheKey.contains("hoaDon");
    }

    /**
     * Validate cache consistency after invalidation
     */
    public boolean validateInvalidation(String cacheKey) {
        try {
            // Check if cache entry is actually removed
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            boolean isInvalidated = (cached == null);

            if (isInvalidated) {
                log.debug("Invalidation validation successful for key '{}'", cacheKey);
            } else {
                log.warn("Invalidation validation failed for key '{}' - entry still exists", cacheKey);
            }

            return isInvalidated;

        } catch (Exception e) {
            log.error("Invalidation validation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Force immediate invalidation with retry mechanism
     */
    public boolean forceInvalidate(String cacheKey, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (invalidate(cacheKey)) {
                if (validateInvalidation(cacheKey)) {
                    log.debug("Force invalidation successful for key '{}' on attempt {}", cacheKey, attempt);
                    return true;
                }
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(50 * attempt); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("Force invalidation failed for key '{}' after {} attempts", cacheKey, maxRetries);
        return false;
    }
}
