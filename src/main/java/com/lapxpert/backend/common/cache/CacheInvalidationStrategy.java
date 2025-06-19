package com.lapxpert.backend.common.cache;

import java.util.Set;

/**
 * Cache Invalidation Strategy Interface for LapXpert E-commerce System
 * 
 * Defines different invalidation strategies based on data criticality and
 * business requirements. Supports both immediate and eventual invalidation
 * patterns for optimal performance and consistency.
 * 
 * Vietnamese Business Context:
 * - Vô hiệu hóa cache: Cache invalidation strategies
 * - Dữ liệu quan trọng: Critical data requiring immediate invalidation
 * - Dữ liệu thông thường: Non-critical data with eventual invalidation
 * 
 * Strategy Types:
 * - IMMEDIATE: Synchronous invalidation for critical data
 * - EVENTUAL: Asynchronous invalidation for non-critical data
 * - PATTERN_BASED: Invalidation based on key patterns
 * - EVENT_DRIVEN: Invalidation triggered by business events
 */
public interface CacheInvalidationStrategy {

    /**
     * Invalidation strategy types
     */
    enum StrategyType {
        IMMEDIATE,      // Synchronous invalidation for critical data
        EVENTUAL,       // Asynchronous invalidation for non-critical data
        PATTERN_BASED,  // Invalidation based on key patterns
        EVENT_DRIVEN    // Invalidation triggered by business events
    }

    /**
     * Get the strategy type
     */
    StrategyType getStrategyType();

    /**
     * Invalidate a single cache entry
     * 
     * @param cacheKey The cache key to invalidate
     * @return true if invalidation was successful
     */
    boolean invalidate(String cacheKey);

    /**
     * Invalidate multiple cache entries
     * 
     * @param cacheKeys Set of cache keys to invalidate
     * @return number of successfully invalidated entries
     */
    int invalidateMultiple(Set<String> cacheKeys);

    /**
     * Invalidate cache entries matching a pattern
     * 
     * @param pattern The pattern to match (e.g., "product:*")
     * @return number of invalidated entries
     */
    int invalidateByPattern(String pattern);

    /**
     * Check if the strategy supports pattern-based invalidation
     */
    boolean supportsPatternInvalidation();

    /**
     * Check if the strategy supports bulk operations
     */
    boolean supportsBulkOperations();

    /**
     * Get estimated invalidation time in milliseconds
     */
    long getEstimatedInvalidationTime();

    /**
     * Validate if the strategy is applicable for the given cache key
     */
    boolean isApplicable(String cacheKey);
}
