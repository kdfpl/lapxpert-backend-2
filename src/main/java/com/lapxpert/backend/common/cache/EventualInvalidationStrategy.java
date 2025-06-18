package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Eventual Cache Invalidation Strategy for LapXpert E-commerce System
 * 
 * Implements asynchronous cache invalidation for non-critical data
 * (product descriptions, categories, user profiles). Prioritizes performance
 * over immediate consistency, allowing for eventual synchronization.
 * 
 * Vietnamese Business Context:
 * - Vô hiệu hóa cuối cùng: Eventual invalidation for non-critical data
 * - Mô tả sản phẩm: Product descriptions with relaxed consistency
 * - Danh mục: Categories with eventual updates
 * - Hồ sơ người dùng: User profiles with performance priority
 */
@Component
@Slf4j
public class EventualInvalidationStrategy implements CacheInvalidationStrategy {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    @Autowired
    private CacheVersioningService versioningService;

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.EVENTUAL;
    }

    @Override
    public boolean invalidate(String cacheKey) {
        if (!isApplicable(cacheKey)) {
            log.debug("Eventual invalidation not applicable for key '{}'", cacheKey);
            return false;
        }

        try {
            // Asynchronous invalidation for better performance
            invalidateAsync(cacheKey);
            log.debug("Eventual invalidation initiated for key '{}'", cacheKey);
            return true;

        } catch (Exception e) {
            log.error("Eventual invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int invalidateMultiple(Set<String> cacheKeys) {
        // Filter keys applicable for eventual invalidation
        Set<String> applicableKeys = cacheKeys.stream()
            .filter(this::isApplicable)
            .collect(java.util.stream.Collectors.toSet());

        if (applicableKeys.isEmpty()) {
            log.debug("No keys applicable for eventual invalidation");
            return 0;
        }

        log.debug("Starting eventual invalidation for {} keys", applicableKeys.size());

        // Process keys asynchronously in batches for efficiency
        invalidateMultipleAsync(applicableKeys);

        // Return count immediately (eventual consistency)
        return applicableKeys.size();
    }

    @Override
    public int invalidateByPattern(String pattern) {
        try {
            log.debug("Starting eventual pattern-based invalidation for pattern '{}'", pattern);

            // Get keys matching the pattern
            Set<String> matchingKeys = redisTemplate.keys(pattern);
            if (matchingKeys == null || matchingKeys.isEmpty()) {
                log.debug("No keys found matching pattern '{}'", pattern);
                return 0;
            }

            // Filter for non-critical keys only
            Set<String> nonCriticalKeys = matchingKeys.stream()
                .filter(this::isApplicable)
                .collect(java.util.stream.Collectors.toSet());

            if (nonCriticalKeys.isEmpty()) {
                log.debug("No non-critical keys found matching pattern '{}'", pattern);
                return 0;
            }

            // Invalidate non-critical keys eventually
            return invalidateMultiple(nonCriticalKeys);

        } catch (Exception e) {
            log.error("Eventual pattern-based invalidation failed for pattern '{}': {}", pattern, e.getMessage(), e);
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
        // Eventual invalidation - very low latency but eventual consistency
        return 10; // milliseconds
    }

    @Override
    public boolean isApplicable(String cacheKey) {
        // Apply eventual invalidation to non-critical caches
        return !consistencyManager.requiresStrongConsistency(cacheKey) &&
               isNonCriticalBusinessData(cacheKey);
    }

    /**
     * Asynchronous invalidation for single cache key
     */
    @Async
    public CompletableFuture<Void> invalidateAsync(String cacheKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Executing async invalidation for key '{}'", cacheKey);

                // Step 1: Remove versioned cache entry
                versioningService.invalidateVersionedEntry(cacheKey);

                // Step 2: Remove any additional Redis keys
                Boolean deleted = redisTemplate.delete(cacheKey);

                log.debug("Async invalidation completed for key '{}', deleted: {}", cacheKey, deleted);

            } catch (Exception e) {
                log.error("Async invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            }
        });
    }

    /**
     * Asynchronous invalidation for multiple cache keys
     */
    @Async
    public CompletableFuture<Integer> invalidateMultipleAsync(Set<String> cacheKeys) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger successCount = new AtomicInteger(0);

            try {
                log.debug("Executing async bulk invalidation for {} keys", cacheKeys.size());

                // Process keys in parallel for better performance
                cacheKeys.parallelStream().forEach(cacheKey -> {
                    try {
                        // Remove versioned cache entry
                        versioningService.invalidateVersionedEntry(cacheKey);

                        // Remove Redis key
                        Boolean deleted = redisTemplate.delete(cacheKey);
                        if (Boolean.TRUE.equals(deleted)) {
                            successCount.incrementAndGet();
                        }

                    } catch (Exception e) {
                        log.warn("Failed to invalidate key '{}' in bulk operation: {}", cacheKey, e.getMessage());
                    }
                });

                int successful = successCount.get();
                log.debug("Async bulk invalidation completed: {}/{} keys successfully invalidated", 
                    successful, cacheKeys.size());

                return successful;

            } catch (Exception e) {
                log.error("Async bulk invalidation failed: {}", e.getMessage(), e);
                return 0;
            }
        });
    }

    /**
     * Check if cache key represents non-critical business data
     */
    private boolean isNonCriticalBusinessData(String cacheKey) {
        return cacheKey.contains("sanPhamList") ||
               cacheKey.contains("categories") ||
               cacheKey.contains("productRatings") ||
               cacheKey.contains("popularProducts") ||
               cacheKey.contains("searchResults") ||
               cacheKey.contains("userSessions") ||
               cacheKey.contains("userProfiles") ||
               cacheKey.contains("systemConfig") ||
               cacheKey.contains("shippingFees") ||
               cacheKey.contains("preferences");
    }

    /**
     * Lazy invalidation with TTL update
     */
    public void lazyInvalidate(String cacheKey, long ttlSeconds) {
        try {
            if (isApplicable(cacheKey)) {
                // Instead of immediate deletion, reduce TTL for eventual expiration
                redisTemplate.expire(cacheKey, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
                log.debug("Lazy invalidation applied to key '{}' with TTL {} seconds", cacheKey, ttlSeconds);
            }
        } catch (Exception e) {
            log.error("Lazy invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Batch invalidation with rate limiting
     */
    public void batchInvalidateWithRateLimit(Set<String> cacheKeys, int batchSize, long delayMs) {
        if (cacheKeys.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                log.debug("Starting rate-limited batch invalidation for {} keys", cacheKeys.size());

                java.util.List<String> keyList = new java.util.ArrayList<>(cacheKeys);
                for (int i = 0; i < keyList.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, keyList.size());
                    java.util.List<String> batch = keyList.subList(i, endIndex);

                    // Process batch
                    batch.forEach(key -> {
                        if (isApplicable(key)) {
                            try {
                                versioningService.invalidateVersionedEntry(key);
                                redisTemplate.delete(key);
                            } catch (Exception e) {
                                log.warn("Failed to invalidate key '{}' in rate-limited batch: {}", key, e.getMessage());
                            }
                        }
                    });

                    // Rate limiting delay
                    if (endIndex < keyList.size() && delayMs > 0) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                log.debug("Rate-limited batch invalidation completed");

            } catch (Exception e) {
                log.error("Rate-limited batch invalidation failed: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Schedule invalidation for future execution
     */
    public void scheduleInvalidation(String cacheKey, long delaySeconds) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delaySeconds * 1000);
                if (isApplicable(cacheKey)) {
                    invalidateAsync(cacheKey);
                    log.debug("Scheduled invalidation executed for key '{}' after {} seconds", cacheKey, delaySeconds);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Scheduled invalidation interrupted for key '{}'", cacheKey);
            } catch (Exception e) {
                log.error("Scheduled invalidation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            }
        });
    }
}
