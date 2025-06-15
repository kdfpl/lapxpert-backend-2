package com.lapxpert.backend.common.cache;

import java.time.Duration;

/**
 * Constants for cache TTL values used across the application
 * Provides centralized TTL configuration for consistent caching behavior
 */
public final class CacheTtlConstants {

    private CacheTtlConstants() {
        // Utility class - prevent instantiation
    }

    // ==================== TTL DURATIONS ====================

    /**
     * High volatility data - changes frequently
     */
    public static final Duration HIGH_VOLATILITY_TTL = Duration.ofMinutes(15);

    /**
     * Medium volatility data - moderate change frequency
     */
    public static final Duration MEDIUM_VOLATILITY_TTL = Duration.ofMinutes(30);

    /**
     * Low volatility data - changes infrequently
     */
    public static final Duration LOW_VOLATILITY_TTL = Duration.ofHours(2);

    /**
     * Static data - rarely changes
     */
    public static final Duration STATIC_DATA_TTL = Duration.ofHours(24);

    // ==================== SPECIFIC DATA TYPE TTL ====================

    /**
     * Product-related TTL values
     */
    public static final class Product {
        public static final Duration RATINGS = Duration.ofHours(2);
        public static final Duration LIST = Duration.ofMinutes(30);
        public static final Duration ACTIVE_LIST = Duration.ofMinutes(15);
        public static final Duration POPULAR = Duration.ofHours(6);
    }

    /**
     * User session TTL values
     */
    public static final class Session {
        public static final Duration USER_SESSION = Duration.ofHours(1);
        public static final Duration CART_DATA = Duration.ofMinutes(30);
    }

    /**
     * Search and computed data TTL values
     */
    public static final class Search {
        public static final Duration RESULTS = Duration.ofMinutes(15);
        public static final Duration FILTERS = Duration.ofMinutes(30);
    }

    /**
     * Reference data TTL values
     */
    public static final class Reference {
        public static final Duration CATEGORIES = Duration.ofHours(24);
        public static final Duration SYSTEM_CONFIG = Duration.ofHours(24);
        public static final Duration BRANDS = Duration.ofHours(12);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get TTL based on data volatility level
     * @param volatility the volatility level
     * @return appropriate TTL duration
     */
    public static Duration getTtlByVolatility(DataVolatility volatility) {
        return switch (volatility) {
            case HIGH -> HIGH_VOLATILITY_TTL;
            case MEDIUM -> MEDIUM_VOLATILITY_TTL;
            case LOW -> LOW_VOLATILITY_TTL;
            case STATIC -> STATIC_DATA_TTL;
        };
    }

    /**
     * Data volatility levels
     */
    public enum DataVolatility {
        HIGH,    // Changes frequently (every few minutes)
        MEDIUM,  // Changes moderately (every 30 minutes to 1 hour)
        LOW,     // Changes infrequently (every few hours)
        STATIC   // Rarely changes (daily or less)
    }
}
