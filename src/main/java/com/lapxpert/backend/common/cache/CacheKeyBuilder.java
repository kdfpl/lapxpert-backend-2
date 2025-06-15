package com.lapxpert.backend.common.cache;

/**
 * Centralized cache key builder for consistent key naming across LapXpert application
 * Provides standardized key patterns for the standalone Redis setup
 * 
 * Key Pattern: module:entity:id or module:entity:type:id
 * Examples:
 * - product:rating:123
 * - user:session:456
 * - search:results:laptop
 */
public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
        // Utility class - prevent instantiation
    }

    // ==================== KEY DELIMITERS ====================

    /**
     * Standard delimiter for cache keys
     */
    private static final String DELIMITER = ":";

    /**
     * Wildcard pattern for key matching
     */
    private static final String WILDCARD = "*";

    // ==================== MODULE PREFIXES ====================

    /**
     * Product module keys
     */
    public static final class Product {
        private static final String MODULE = "product";

        public static String rating(Long productId) {
            return buildKey(MODULE, "rating", productId);
        }

        public static String reviewCount(Long productId) {
            return buildKey(MODULE, "review-count", productId);
        }

        public static String popularList() {
            return buildKey(MODULE, "popular");
        }

        public static String list() {
            return buildKey(MODULE, "list");
        }

        public static String activeList() {
            return buildKey(MODULE, "active-list");
        }

        public static String ratingPattern() {
            return buildPattern(MODULE, "rating");
        }

        public static String reviewCountPattern() {
            return buildPattern(MODULE, "review-count");
        }
    }

    /**
     * User module keys
     */
    public static final class User {
        private static final String MODULE = "user";

        public static String session(Long userId) {
            return buildKey(MODULE, "session", userId);
        }

        public static String profile(Long userId) {
            return buildKey(MODULE, "profile", userId);
        }

        public static String preferences(Long userId) {
            return buildKey(MODULE, "preferences", userId);
        }

        public static String sessionPattern() {
            return buildPattern(MODULE, "session");
        }
    }

    /**
     * Cart module keys
     */
    public static final class Cart {
        private static final String MODULE = "cart";

        public static String data(Long userId) {
            return buildKey(MODULE, "data", userId);
        }

        public static String items(Long cartId) {
            return buildKey(MODULE, "items", cartId);
        }

        public static String total(Long cartId) {
            return buildKey(MODULE, "total", cartId);
        }

        public static String dataPattern() {
            return buildPattern(MODULE, "data");
        }
    }

    /**
     * Search module keys
     */
    public static final class Search {
        private static final String MODULE = "search";

        public static String results(String query) {
            return buildKey(MODULE, "results", sanitizeSearchQuery(query));
        }

        public static String filters(String category) {
            return buildKey(MODULE, "filters", category);
        }

        public static String suggestions(String prefix) {
            return buildKey(MODULE, "suggestions", prefix);
        }

        public static String resultsPattern() {
            return buildPattern(MODULE, "results");
        }
    }

    /**
     * Category module keys
     */
    public static final class Category {
        private static final String MODULE = "category";

        public static String list() {
            return buildKey(MODULE, "list");
        }

        public static String tree() {
            return buildKey(MODULE, "tree");
        }

        public static String products(Long categoryId) {
            return buildKey(MODULE, "products", categoryId);
        }

        public static String listPattern() {
            return buildPattern(MODULE, "list");
        }
    }

    /**
     * System module keys
     */
    public static final class System {
        private static final String MODULE = "system";

        public static String config(String configKey) {
            return buildKey(MODULE, "config", configKey);
        }

        public static String health() {
            return buildKey(MODULE, "health");
        }

        public static String stats(String statsType) {
            return buildKey(MODULE, "stats", statsType);
        }

        public static String configPattern() {
            return buildPattern(MODULE, "config");
        }
    }



    // ==================== KEY BUILDING METHODS ====================

    /**
     * Build cache key with module:entity:id pattern
     */
    private static String buildKey(String module, String entity, Object id) {
        return module + DELIMITER + entity + DELIMITER + String.valueOf(id);
    }

    /**
     * Build cache key with module:entity pattern
     */
    private static String buildKey(String module, String entity) {
        return module + DELIMITER + entity;
    }

    /**
     * Build pattern for key matching (with wildcard)
     */
    private static String buildPattern(String module, String entity) {
        return module + DELIMITER + entity + DELIMITER + WILDCARD;
    }

    /**
     * Build custom key with variable parts
     */
    public static String buildCustomKey(String... parts) {
        return String.join(DELIMITER, parts);
    }

    /**
     * Build pattern for custom key matching
     */
    public static String buildCustomPattern(String... parts) {
        String[] partsWithWildcard = new String[parts.length + 1];
        for (int i = 0; i < parts.length; i++) {
            partsWithWildcard[i] = parts[i];
        }
        partsWithWildcard[parts.length] = WILDCARD;
        return String.join(DELIMITER, partsWithWildcard);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Sanitize search query for use in cache keys
     */
    private static String sanitizeSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "empty";
        }
        
        // Remove special characters and limit length
        String sanitized = query.trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-");
        
        // Limit length to prevent very long keys
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.isEmpty() ? "empty" : sanitized;
    }

    /**
     * Extract ID from cache key
     */
    public static String extractId(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length > 2 ? parts[parts.length - 1] : null;
    }

    /**
     * Extract module from cache key
     */
    public static String extractModule(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Extract entity from cache key
     */
    public static String extractEntity(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Validate cache key format
     */
    public static boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = key.split(DELIMITER);
        return parts.length >= 2 && 
               parts[0] != null && !parts[0].trim().isEmpty() &&
               parts[1] != null && !parts[1].trim().isEmpty();
    }

    /**
     * Get key length for memory optimization
     */
    public static int getKeyLength(String key) {
        return key != null ? key.length() : 0;
    }
}
