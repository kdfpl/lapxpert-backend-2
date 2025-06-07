package com.lapxpert.backend.danhgia.application.controller;

import com.lapxpert.backend.danhgia.domain.service.ProductRatingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for cache management operations
 * Provides endpoints for monitoring and managing the product rating cache
 * Only accessible by admin users for maintenance purposes
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CacheManagementController {

    private final ProductRatingCacheService cacheService;

    /**
     * Get cache statistics for monitoring
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRatingCacheService.CacheStatistics> getCacheStats() {
        try {
            ProductRatingCacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clear all cache entries (for maintenance)
     */
    @PostMapping("/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearAllCache() {
        try {
            log.info("Admin requested to clear all cache entries");
            cacheService.clearAllCache();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All cache entries cleared successfully"
            ));
        } catch (Exception e) {
            log.error("Error clearing all cache", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to clear cache: " + e.getMessage()
                ));
        }
    }

    /**
     * Clear cache for a specific product
     */
    @PostMapping("/clear-product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearProductCache(@PathVariable Long productId) {
        try {
            log.info("Admin requested to clear cache for product {}", productId);
            cacheService.clearProductCache(productId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache cleared for product " + productId
            ));
        } catch (Exception e) {
            log.error("Error clearing cache for product {}", productId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to clear product cache: " + e.getMessage()
                ));
        }
    }

    /**
     * Warm up cache for popular products
     */
    @PostMapping("/warm-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> warmUpCache() {
        try {
            log.info("Admin requested cache warm-up");
            cacheService.warmUpCache();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache warm-up completed successfully"
            ));
        } catch (Exception e) {
            log.error("Error during cache warm-up", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "Cache warm-up failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Force refresh of product ratings cache
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> refreshCache() {
        try {
            log.info("Admin requested cache refresh");
            cacheService.refreshProductRatings();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache refresh completed successfully"
            ));
        } catch (Exception e) {
            log.error("Error during cache refresh", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "Cache refresh failed: " + e.getMessage()
                ));
        }
    }

    /**
     * Health check for cache service
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            ProductRatingCacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "totalCacheSize", stats.getTotalCacheSize(),
                "ratingCacheSize", stats.getRatingCacheSize(),
                "countCacheSize", stats.getCountCacheSize(),
                "lastUpdated", stats.getLastUpdated()
            ));
        } catch (Exception e) {
            log.error("Error checking cache health", e);
            return ResponseEntity.ok(Map.of(
                "status", "unhealthy",
                "error", e.getMessage()
            ));
        }
    }
}
