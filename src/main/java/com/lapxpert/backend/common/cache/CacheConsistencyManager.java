package com.lapxpert.backend.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache Consistency Manager for LapXpert E-commerce System
 * 
 * Manages hybrid cache consistency model with strong consistency for critical data
 * (inventory, pricing, orders) and eventual consistency for non-critical data
 * (product descriptions, categories, user profiles).
 * 
 * Vietnamese Business Context:
 * - Dữ liệu quan trọng: Tồn kho, giá cả, đơn hàng, voucher
 * - Dữ liệu thông thường: Mô tả sản phẩm, danh mục, hồ sơ người dùng
 * 
 * Consistency Models:
 * - STRONG: Write-through pattern with immediate synchronization
 * - EVENTUAL: Cache-aside pattern with TTL-based expiration
 */
@Component
@Slf4j
public class CacheConsistencyManager {

    /**
     * Cache consistency levels
     */
    public enum ConsistencyLevel {
        STRONG,     // Critical data requiring immediate consistency
        EVENTUAL    // Non-critical data allowing eventual consistency
    }

    /**
     * Data criticality classification
     */
    public enum DataCriticality {
        CRITICAL,       // Inventory, pricing, orders, vouchers
        NON_CRITICAL    // Descriptions, categories, user profiles
    }

    // Critical cache patterns requiring strong consistency (using patterns for dynamic keys)
    private static final Set<String> CRITICAL_CACHE_PATTERNS = Set.of(
        // Inventory and stock management (dynamic keys like inventory:123)
        "inventory:*", "stockLevels:*", "serialNumbers:*", "availableQuantity:*",

        // Pricing and discount campaigns (dynamic keys like productPricing:456)
        "productPricing:*", "dotGiamGia:*", "activeCampaigns:*", "effectivePrices:*",

        // Order and payment processing (dynamic keys like orderProcessing:789)
        "orderProcessing:*", "paymentStatus:*", "orderInventory:*", "reservedItems:*",

        // Voucher management (dynamic keys like voucherUsage:101)
        "voucherUsage:*", "phieuGiamGia:*", "voucherValidation:*", "usageCount:*"
    );

    // Spring Cache names that are critical (from application.properties)
    private static final Set<String> CRITICAL_SPRING_CACHES = Set.of(
        // These are actual Spring Cache names that contain critical business data
        "hoaDonCache", "dotGiamGiaCache", "phieuGiamGiaCache"
    );

    // Non-critical cache names allowing eventual consistency
    private static final Set<String> NON_CRITICAL_CACHES = Set.of(
        // Product information
        "sanPhamList", "productRatings", "popularProducts", "categories",
        
        // User and session data
        "userSessions", "userProfiles", "preferences",
        
        // Search and analytics
        "searchResults", "systemConfig", "shippingFees"
    );

    // Cache consistency tracking
    private final ConcurrentHashMap<String, ConsistencyLevel> cacheConsistencyMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastConsistencyCheck = new ConcurrentHashMap<>();

    /**
     * Initialize cache consistency mappings on startup
     */
    @PostConstruct
    public void initializeConsistencyMappings() {
        log.info("Initializing cache consistency mappings for LapXpert system");

        // Map critical cache patterns to strong consistency
        CRITICAL_CACHE_PATTERNS.forEach(cachePattern -> {
            cacheConsistencyMap.put(cachePattern, ConsistencyLevel.STRONG);
            log.debug("Mapped cache pattern '{}' to STRONG consistency", cachePattern);
        });

        // Map critical Spring caches to strong consistency
        CRITICAL_SPRING_CACHES.forEach(cacheName -> {
            cacheConsistencyMap.put(cacheName, ConsistencyLevel.STRONG);
            log.debug("Mapped Spring cache '{}' to STRONG consistency", cacheName);
        });

        // Map non-critical caches to eventual consistency
        NON_CRITICAL_CACHES.forEach(cacheName -> {
            cacheConsistencyMap.put(cacheName, ConsistencyLevel.EVENTUAL);
            log.debug("Mapped cache '{}' to EVENTUAL consistency", cacheName);
        });

        log.info("Cache consistency mappings initialized: {} patterns + {} Spring caches = {} STRONG, {} EVENTUAL",
            CRITICAL_CACHE_PATTERNS.size(), CRITICAL_SPRING_CACHES.size(),
            CRITICAL_CACHE_PATTERNS.size() + CRITICAL_SPRING_CACHES.size(), NON_CRITICAL_CACHES.size());
    }

    /**
     * Determine consistency level for a cache
     */
    public ConsistencyLevel getConsistencyLevel(String cacheName) {
        return cacheConsistencyMap.getOrDefault(cacheName, ConsistencyLevel.EVENTUAL);
    }

    /**
     * Check if cache requires strong consistency
     */
    public boolean requiresStrongConsistency(String cacheName) {
        return getConsistencyLevel(cacheName) == ConsistencyLevel.STRONG ||
               isCriticalCachePattern(cacheName);
    }

    /**
     * Classify data criticality based on cache name or data type
     */
    public DataCriticality classifyDataCriticality(String cacheName) {
        if (isCriticalCache(cacheName)) {
            return DataCriticality.CRITICAL;
        }
        return DataCriticality.NON_CRITICAL;
    }

    /**
     * Check if cache name matches critical patterns or Spring cache names
     */
    public boolean isCriticalCache(String cacheName) {
        // Check exact Spring cache names
        if (CRITICAL_SPRING_CACHES.contains(cacheName)) {
            return true;
        }

        // Check cache patterns (e.g., inventory:123 matches inventory:*)
        return isCriticalCachePattern(cacheName);
    }

    /**
     * Check if cache name matches critical cache patterns
     */
    private boolean isCriticalCachePattern(String cacheName) {
        return CRITICAL_CACHE_PATTERNS.stream()
            .anyMatch(pattern -> {
                if (pattern.endsWith(":*")) {
                    String prefix = pattern.substring(0, pattern.length() - 1); // Remove *
                    return cacheName.startsWith(prefix);
                }
                return pattern.equals(cacheName);
            });
    }

    /**
     * Record consistency check timestamp
     */
    public void recordConsistencyCheck(String cacheName) {
        lastConsistencyCheck.put(cacheName, Instant.now());
    }

    /**
     * Get last consistency check time
     */
    public Instant getLastConsistencyCheck(String cacheName) {
        return lastConsistencyCheck.get(cacheName);
    }

    /**
     * Get all critical cache patterns and Spring cache names
     */
    public Set<String> getCriticalCaches() {
        Set<String> allCritical = new java.util.HashSet<>();
        allCritical.addAll(CRITICAL_CACHE_PATTERNS);
        allCritical.addAll(CRITICAL_SPRING_CACHES);
        return allCritical;
    }

    /**
     * Get critical cache patterns only
     */
    public Set<String> getCriticalCachePatterns() {
        return Set.copyOf(CRITICAL_CACHE_PATTERNS);
    }

    /**
     * Get critical Spring cache names only
     */
    public Set<String> getCriticalSpringCaches() {
        return Set.copyOf(CRITICAL_SPRING_CACHES);
    }

    /**
     * Get all non-critical cache names
     */
    public Set<String> getNonCriticalCaches() {
        return Set.copyOf(NON_CRITICAL_CACHES);
    }

    /**
     * Check if cache name matches Vietnamese business terminology
     */
    public boolean isVietnameseBusinessCache(String cacheName) {
        return cacheName.contains("sanPham") || 
               cacheName.contains("hoaDon") || 
               cacheName.contains("phieuGiamGia") || 
               cacheName.contains("dotGiamGia") ||
               cacheName.contains("nguoiDung");
    }
}
