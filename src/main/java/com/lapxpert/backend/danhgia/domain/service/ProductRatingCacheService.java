package com.lapxpert.backend.danhgia.domain.service;

import com.lapxpert.backend.common.cache.CacheKeyBuilder;
import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.application.dto.ProductRatingDto;
import com.lapxpert.backend.danhgia.application.dto.RatingDistributionDto;
import com.lapxpert.backend.danhgia.domain.repository.DanhGiaRepository;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;



import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing product rating cache with Redis
 * Provides high-performance rating retrieval and scheduled cache refresh
 * Implements comprehensive caching strategy for review aggregations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRatingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DanhGiaRepository danhGiaRepository;
    private final SanPhamRepository sanPhamRepository;

    // ==================== CACHE CONFIGURATION ====================

    // Cache keys are now managed by CacheKeyBuilder for consistency

    // Cache TTL configurations are now handled by Spring Cache Manager

    // Removed performance thresholds - simplified for basic caching

    // ==================== CACHE OPERATIONS ====================

    /**
     * Get cached product rating or calculate if not cached
     * Uses Spring Cache abstraction for simplicity
     * @param productId product ID
     * @return product rating DTO
     */
    @Cacheable(value = "productRatings", key = "#productId")
    public ProductRatingDto getProductRating(Long productId) {
        log.debug("Getting product rating for product {}", productId);
        long startTime = System.currentTimeMillis();

        try {
            // Calculate rating (Spring Cache handles caching automatically)
            ProductRatingDto rating = calculateProductRating(productId);

            // Log operation timing
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Calculated rating for product {} in {}ms", productId, duration);
            return rating;

        } catch (Exception e) {
            // Log failed operation
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Failed to get rating for product {} in {}ms", productId, duration, e);
            throw e;
        }
    }

    /**
     * Invalidate product rating cache
     * @param productId product ID to invalidate
     */
    @CacheEvict(value = "productRatings", key = "#productId")
    public void invalidateProductRating(Long productId) {
        log.debug("Invalidated rating cache for product {}", productId);
    }

    /**
     * Invalidate all product rating caches
     */
    @CacheEvict(value = "productRatings", allEntries = true)
    public void invalidateAllProductRatings() {
        log.info("Invalidated all product rating caches");
    }

    /**
     * Simple cache warm-up for startup
     */
    public void warmUpCache() {
        log.info("Starting basic cache warm-up");

        // Simple warm-up: just get a few active products to populate cache
        List<Long> activeProducts = getAllActiveProductIds().stream()
            .limit(10) // Only warm up first 10 products
            .toList();

        int warmedCount = 0;
        for (Long productId : activeProducts) {
            try {
                getProductRating(productId);
                warmedCount++;
            } catch (Exception e) {
                log.warn("Failed to warm cache for product {}", productId, e);
            }
        }

        log.info("Basic cache warm-up completed: {} products cached", warmedCount);
    }

    // ==================== SCHEDULED TASKS ====================

    /**
     * Simple cache cleanup - let Redis TTL handle expiration automatically
     * This is just for logging and monitoring
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void logCacheStatus() {
        try {
            Set<String> ratingKeys = redisTemplate.keys(CacheKeyBuilder.Product.ratingPattern());
            int cacheSize = ratingKeys != null ? ratingKeys.size() : 0;
            log.debug("Current cache size: {} product ratings", cacheSize);
        } catch (Exception e) {
            log.debug("Error checking cache status", e);
        }
    }

    // ==================== HELPER METHODS ====================

    // Removed complex popular products tracking - simplified for basic caching

    /**
     * Get all active product IDs
     */
    private List<Long> getAllActiveProductIds() {
        // This should be implemented in SanPhamRepository
        // For now, return a basic implementation
        return sanPhamRepository.findAll().stream()
            .filter(product -> product.getTrangThai() != null && product.getTrangThai())
            .map(product -> product.getId())
            .toList();
    }

    // Removed complex refresh methods - Spring Cache handles this automatically

    // Removed popular product checking - simplified for basic caching

    /**
     * Calculate product rating statistics directly (without circular dependency)
     * @param sanPhamId product ID
     * @return product rating summary
     */
    private ProductRatingDto calculateProductRating(Long sanPhamId) {
        log.debug("Calculating rating for product {}", sanPhamId);

        // Get basic statistics
        Optional<Double> averageRating = danhGiaRepository.calculateAverageRating(sanPhamId, TrangThaiDanhGia.DA_DUYET);
        Long totalReviews = danhGiaRepository.countReviewsByProduct(sanPhamId, TrangThaiDanhGia.DA_DUYET);

        // Get rating distribution
        RatingDistributionDto distribution = calculateRatingDistribution(sanPhamId);

        // Note: Recent reviews are not included in cache to avoid circular dependency

        return ProductRatingDto.builder()
            .sanPhamId(sanPhamId)
            .averageRating(averageRating.orElse(0.0))
            .totalApprovedReviews(totalReviews.intValue())
            .distribution(distribution)
            .recentReviews(List.of()) // Empty list to avoid circular dependency
            .lastUpdated(Instant.now())
            .verifiedPurchaseCount(totalReviews.intValue()) // All reviews are verified purchases
            .build();
    }

    /**
     * Calculate rating distribution for a product
     */
    private RatingDistributionDto calculateRatingDistribution(Long sanPhamId) {
        List<Object[]> distribution = danhGiaRepository.getRatingDistribution(sanPhamId, TrangThaiDanhGia.DA_DUYET);

        RatingDistributionDto result = new RatingDistributionDto();

        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];

            switch (rating) {
                case 1 -> result.setOneStar(count.intValue());
                case 2 -> result.setTwoStar(count.intValue());
                case 3 -> result.setThreeStar(count.intValue());
                case 4 -> result.setFourStar(count.intValue());
                case 5 -> result.setFiveStar(count.intValue());
            }
        }

        result.calculatePercentages();
        return result;
    }



    // Removed complex cache statistics - simplified for basic caching

    // ==================== CACHE MONITORING ====================

    /**
     * Simple cache statistics for monitoring
     */
    public CacheStatistics getCacheStatistics() {
        try {
            Set<String> ratingKeys = redisTemplate.keys(CacheKeyBuilder.Product.ratingPattern());
            int ratingCacheSize = ratingKeys != null ? ratingKeys.size() : 0;

            return new CacheStatistics(ratingCacheSize, 0, Instant.now());

        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return new CacheStatistics(0, 0, Instant.now());
        }
    }

    /**
     * Clear all rating cache entries (for maintenance or troubleshooting)
     * Simplified to use Spring Cache eviction
     */
    public void clearAllCache() {
        try {
            log.info("Clearing all product rating cache entries");

            // Use Spring Cache eviction for simplicity
            invalidateAllProductRatings();

            log.info("Cache clearing completed");

        } catch (Exception e) {
            log.error("Error clearing cache", e);
        }
    }

    /**
     * Clear cache for a specific product
     * Simplified to use Spring Cache eviction
     */
    public void clearProductCache(Long productId) {
        try {
            // Use Spring Cache eviction for simplicity
            invalidateProductRating(productId);
            log.debug("Cleared cache for product {}", productId);
        } catch (Exception e) {
            log.warn("Failed to clear cache for product {}: {}", productId, e.getMessage());
        }
    }

    /**
     * Cache statistics class
     */
    public static class CacheStatistics {
        private final int ratingCacheSize;
        private final int countCacheSize;
        private final Instant lastUpdated;

        public CacheStatistics(int ratingCacheSize, int countCacheSize, Instant lastUpdated) {
            this.ratingCacheSize = ratingCacheSize;
            this.countCacheSize = countCacheSize;
            this.lastUpdated = lastUpdated;
        }

        public int getRatingCacheSize() { return ratingCacheSize; }
        public int getCountCacheSize() { return countCacheSize; }
        public Instant getLastUpdated() { return lastUpdated; }
        public int getTotalCacheSize() { return ratingCacheSize + countCacheSize; }
    }
}
