package com.lapxpert.backend.danhgia.domain.service;

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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    /**
     * Cache key prefixes for different data types
     */
    private static final String PRODUCT_RATING_KEY = "product:rating:";
    private static final String REVIEW_COUNT_KEY = "product:review-count:";
    private static final String POPULAR_PRODUCTS_KEY = "popular:products";
    private static final String CACHE_STATS_KEY = "cache:stats:ratings";

    /**
     * Cache TTL configurations
     */
    private static final Duration RATING_TTL = Duration.ofHours(2);
    private static final Duration COUNT_TTL = Duration.ofMinutes(30);
    private static final Duration POPULAR_PRODUCTS_TTL = Duration.ofHours(6);

    /**
     * Performance thresholds
     */
    private static final int POPULAR_PRODUCT_THRESHOLD = 10; // Minimum reviews to be considered popular
    private static final int BATCH_SIZE = 50; // Products to process in each batch

    // ==================== CACHE OPERATIONS ====================

    /**
     * Get cached product rating or calculate if not cached
     * @param productId product ID
     * @return product rating DTO
     */
    @Cacheable(value = "productRatings", key = "#productId")
    public ProductRatingDto getProductRating(Long productId) {
        log.debug("Getting product rating for product {}", productId);

        // Try to get from Redis first
        String cacheKey = PRODUCT_RATING_KEY + productId;
        ProductRatingDto cachedRating = (ProductRatingDto) redisTemplate.opsForValue().get(cacheKey);

        if (cachedRating != null) {
            log.debug("Found cached rating for product {}", productId);
            return cachedRating;
        }

        // Calculate and cache
        ProductRatingDto rating = calculateProductRating(productId);
        cacheProductRating(productId, rating);

        log.debug("Calculated and cached rating for product {}", productId);
        return rating;
    }

    /**
     * Invalidate product rating cache
     * @param productId product ID to invalidate
     */
    @CacheEvict(value = "productRatings", key = "#productId")
    public void invalidateProductRating(Long productId) {
        String cacheKey = PRODUCT_RATING_KEY + productId;
        redisTemplate.delete(cacheKey);

        // Also invalidate review count cache
        String countKey = REVIEW_COUNT_KEY + productId;
        redisTemplate.delete(countKey);

        log.debug("Invalidated rating cache for product {}", productId);

        // Update cache statistics
        updateCacheStats("invalidation", productId);
    }

    /**
     * Invalidate all product rating caches
     */
    @CacheEvict(value = "productRatings", allEntries = true)
    public void invalidateAllProductRatings() {
        // Clear Redis cache patterns
        Set<String> ratingKeys = redisTemplate.keys(PRODUCT_RATING_KEY + "*");
        Set<String> countKeys = redisTemplate.keys(REVIEW_COUNT_KEY + "*");

        if (ratingKeys != null && !ratingKeys.isEmpty()) {
            redisTemplate.delete(ratingKeys);
        }
        if (countKeys != null && !countKeys.isEmpty()) {
            redisTemplate.delete(countKeys);
        }

        log.info("Invalidated all product rating caches");
    }

    /**
     * Warm up cache for popular products
     */
    public void warmUpCache() {
        log.info("Starting cache warm-up for popular products");

        List<Long> popularProducts = getPopularProductIds();
        int warmedCount = 0;

        for (Long productId : popularProducts) {
            try {
                getProductRating(productId);
                warmedCount++;
            } catch (Exception e) {
                log.warn("Failed to warm cache for product {}", productId, e);
            }
        }

        log.info("Cache warm-up completed: {} products cached", warmedCount);
    }

    // ==================== SCHEDULED TASKS ====================

    /**
     * Refresh product ratings cache every 15 minutes
     * Focuses on popular products and recently updated products
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @Transactional(readOnly = true)
    public void refreshProductRatings() {
        log.debug("Starting scheduled product ratings refresh");

        try {
            List<Long> productsToRefresh = getProductsToRefresh();
            int refreshedCount = 0;

            // Process in batches to avoid overwhelming the system
            for (int i = 0; i < productsToRefresh.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, productsToRefresh.size());
                List<Long> batch = productsToRefresh.subList(i, endIndex);

                refreshedCount += refreshBatch(batch);

                // Small delay between batches
                if (endIndex < productsToRefresh.size()) {
                    Thread.sleep(100);
                }
            }

            log.info("Scheduled refresh completed: {} products refreshed", refreshedCount);
            updateCacheStats("scheduled_refresh", refreshedCount);

        } catch (Exception e) {
            log.error("Error during scheduled product ratings refresh", e);
        }
    }

    /**
     * Clean up expired cache entries every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredCache() {
        log.debug("Starting cache cleanup");

        try {
            // This is handled automatically by Redis TTL, but we can add custom cleanup logic here
            // For example, removing entries that haven't been accessed recently

            Set<String> allKeys = redisTemplate.keys(PRODUCT_RATING_KEY + "*");
            int cleanedCount = 0;

            if (allKeys != null) {
                for (String key : allKeys) {
                    // Check if key is about to expire or hasn't been accessed recently
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl < 300) { // Less than 5 minutes TTL remaining
                        // Let it expire naturally, but we could refresh popular ones here
                        String productIdStr = key.replace(PRODUCT_RATING_KEY, "");
                        try {
                            Long productId = Long.parseLong(productIdStr);
                            if (isPopularProduct(productId)) {
                                refreshProductRating(productId);
                                cleanedCount++;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid product ID in cache key: {}", key);
                        }
                    }
                }
            }

            log.debug("Cache cleanup completed: {} entries refreshed", cleanedCount);

        } catch (Exception e) {
            log.error("Error during cache cleanup", e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Cache product rating in Redis with TTL
     */
    private void cacheProductRating(Long productId, ProductRatingDto rating) {
        String key = PRODUCT_RATING_KEY + productId;
        redisTemplate.opsForValue().set(key, rating, RATING_TTL);

        // Also cache review count separately for quick access
        String countKey = REVIEW_COUNT_KEY + productId;
        redisTemplate.opsForValue().set(countKey, rating.getTotalApprovedReviews(), COUNT_TTL);
    }

    /**
     * Get list of popular product IDs for cache prioritization
     */
    private List<Long> getPopularProductIds() {
        // Check if we have cached popular products list
        try {
            @SuppressWarnings("unchecked")
            List<Long> cached = (List<Long>) redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);

            if (cached != null && !cached.isEmpty()) {
                log.debug("Retrieved {} popular products from cache", cached.size());
                return cached;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve popular products from cache, will recalculate: {}", e.getMessage());
            // Clear the corrupted cache entry
            try {
                redisTemplate.delete(POPULAR_PRODUCTS_KEY);
            } catch (Exception deleteEx) {
                log.debug("Failed to delete corrupted cache entry", deleteEx);
            }
        }

        // Calculate popular products (products with many reviews)
        List<Object[]> popularData = danhGiaRepository.calculateBatchRatings(
            getAllActiveProductIds(), TrangThaiDanhGia.DA_DUYET);

        List<Long> popularProducts = popularData.stream()
            .filter(row -> ((Long) row[2]) >= POPULAR_PRODUCT_THRESHOLD) // Filter by review count
            .map(row -> (Long) row[0]) // Extract product ID
            .limit(100) // Top 100 popular products
            .toList();

        // Cache the popular products list
        try {
            redisTemplate.opsForValue().set(POPULAR_PRODUCTS_KEY, popularProducts, POPULAR_PRODUCTS_TTL);
            log.debug("Cached {} popular products", popularProducts.size());
        } catch (Exception e) {
            log.warn("Failed to cache popular products: {}", e.getMessage());
        }

        return popularProducts;
    }

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

    /**
     * Get products that need cache refresh
     */
    private List<Long> getProductsToRefresh() {
        // Combine popular products and recently reviewed products
        List<Long> popularProducts = getPopularProductIds();

        // Add products with recent reviews (last 24 hours)
        Instant yesterday = Instant.now().minusSeconds(24 * 60 * 60);
        List<Long> recentlyReviewed = danhGiaRepository.findRecentReviewsByCustomer(null, yesterday)
            .stream()
            .map(review -> review.getSanPham().getId())
            .distinct()
            .toList();

        // Combine popular products and recently reviewed products
        return java.util.stream.Stream.concat(
                popularProducts.stream().distinct().limit(150), // Popular products
                recentlyReviewed.stream().limit(50) // Recently reviewed products
            )
            .distinct()
            .limit(200) // Final limit to prevent overload
            .toList();
    }

    /**
     * Refresh a batch of products
     */
    private int refreshBatch(List<Long> productIds) {
        int refreshedCount = 0;

        for (Long productId : productIds) {
            try {
                refreshProductRating(productId);
                refreshedCount++;
            } catch (Exception e) {
                log.warn("Failed to refresh rating for product {}", productId, e);
            }
        }

        return refreshedCount;
    }

    /**
     * Refresh rating for a single product
     */
    private void refreshProductRating(Long productId) {
        ProductRatingDto rating = calculateProductRating(productId);
        cacheProductRating(productId, rating);
    }

    /**
     * Check if product is popular (has many reviews)
     */
    private boolean isPopularProduct(Long productId) {
        Long reviewCount = danhGiaRepository.countReviewsByProduct(productId, TrangThaiDanhGia.DA_DUYET);
        return reviewCount >= POPULAR_PRODUCT_THRESHOLD;
    }

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



    /**
     * Update cache statistics for monitoring
     */
    private void updateCacheStats(String operation, Object value) {
        try {
            String statsKey = CACHE_STATS_KEY + ":" + operation;
            redisTemplate.opsForValue().increment(statsKey);
            redisTemplate.expire(statsKey, Duration.ofDays(7)); // Keep stats for a week
        } catch (Exception e) {
            log.debug("Failed to update cache stats", e);
        }
    }

    // ==================== CACHE MONITORING ====================

    /**
     * Get cache statistics for monitoring
     */
    public CacheStatistics getCacheStatistics() {
        try {
            Set<String> ratingKeys = redisTemplate.keys(PRODUCT_RATING_KEY + "*");
            Set<String> countKeys = redisTemplate.keys(REVIEW_COUNT_KEY + "*");

            int ratingCacheSize = ratingKeys != null ? ratingKeys.size() : 0;
            int countCacheSize = countKeys != null ? countKeys.size() : 0;

            return new CacheStatistics(ratingCacheSize, countCacheSize, Instant.now());

        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return new CacheStatistics(0, 0, Instant.now());
        }
    }

    /**
     * Clear all rating cache entries (for maintenance or troubleshooting)
     */
    public void clearAllCache() {
        try {
            log.info("Clearing all product rating cache entries");

            // Clear product rating cache
            Set<String> ratingKeys = redisTemplate.keys(PRODUCT_RATING_KEY + "*");
            if (ratingKeys != null && !ratingKeys.isEmpty()) {
                redisTemplate.delete(ratingKeys);
                log.info("Cleared {} product rating cache entries", ratingKeys.size());
            }

            // Clear review count cache
            Set<String> countKeys = redisTemplate.keys(REVIEW_COUNT_KEY + "*");
            if (countKeys != null && !countKeys.isEmpty()) {
                redisTemplate.delete(countKeys);
                log.info("Cleared {} review count cache entries", countKeys.size());
            }

            // Clear popular products cache
            redisTemplate.delete(POPULAR_PRODUCTS_KEY);
            log.info("Cleared popular products cache");

            // Clear cache stats
            Set<String> statsKeys = redisTemplate.keys(CACHE_STATS_KEY + "*");
            if (statsKeys != null && !statsKeys.isEmpty()) {
                redisTemplate.delete(statsKeys);
                log.info("Cleared {} cache statistics entries", statsKeys.size());
            }

        } catch (Exception e) {
            log.error("Error clearing cache", e);
        }
    }

    /**
     * Clear cache for a specific product
     */
    public void clearProductCache(Long productId) {
        try {
            String ratingKey = PRODUCT_RATING_KEY + productId;
            String countKey = REVIEW_COUNT_KEY + productId;

            redisTemplate.delete(ratingKey);
            redisTemplate.delete(countKey);

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
