package com.lapxpert.backend.common.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache Versioning Service for LapXpert E-commerce System
 * 
 * Implements cache versioning with conflict resolution for hybrid consistency model.
 * Uses Instant timestamps for version tracking (consistent with WebSocket migration).
 * 
 * Vietnamese Business Context:
 * - Phiên bản cache: Version tracking for data consistency
 * - Giải quyết xung đột: Conflict resolution strategies
 * - Đồng bộ dữ liệu: Data synchronization across instances
 * 
 * Conflict Resolution Strategies:
 * - LAST_WRITE_WINS: Latest timestamp prevails (default for non-critical data)
 * - OPTIMISTIC_LOCKING: Version-based conflict detection (for critical data)
 * - MANUAL_RESOLUTION: Application-defined conflict handling
 */
@Service
@Slf4j
public class CacheVersioningService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    /**
     * Conflict resolution strategies
     */
    public enum ConflictResolutionStrategy {
        LAST_WRITE_WINS,    // Latest timestamp wins (eventual consistency)
        OPTIMISTIC_LOCKING, // Version-based conflict detection (strong consistency)
        MANUAL_RESOLUTION   // Application-defined resolution
    }

    /**
     * Versioned cache entry wrapper
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VersionedCacheEntry<T> {
        private T data;
        private Instant version;
        private String source;
        private ConflictResolutionStrategy strategy;
        private String checksum;

        public VersionedCacheEntry(T data, Instant version, String source) {
            this.data = data;
            this.version = version;
            this.source = source;
            this.strategy = ConflictResolutionStrategy.LAST_WRITE_WINS;
            this.checksum = generateChecksum(data);
        }

        private String generateChecksum(T data) {
            if (data == null) return null;
            return String.valueOf(data.hashCode());
        }

        public boolean isNewerThan(VersionedCacheEntry<T> other) {
            if (other == null || other.version == null) return true;
            if (this.version == null) return false;
            return this.version.isAfter(other.version);
        }

        public boolean hasConflict(VersionedCacheEntry<T> other) {
            if (other == null) return false;
            return this.version != null && other.version != null && 
                   !this.version.equals(other.version) &&
                   !this.checksum.equals(other.checksum);
        }
    }

    /**
     * Create versioned cache entry with current timestamp
     */
    public <T> VersionedCacheEntry<T> createVersionedEntry(T data, String source) {
        return new VersionedCacheEntry<>(data, Instant.now(), source);
    }

    /**
     * Create versioned cache entry with specific strategy
     */
    public <T> VersionedCacheEntry<T> createVersionedEntry(T data, String source, 
                                                          ConflictResolutionStrategy strategy) {
        VersionedCacheEntry<T> entry = createVersionedEntry(data, source);
        entry.setStrategy(strategy);
        return entry;
    }

    /**
     * Store versioned data in cache
     */
    public <T> void storeVersionedData(String cacheKey, T data, String source) {
        try {
            ConflictResolutionStrategy strategy = determineStrategy(cacheKey);
            VersionedCacheEntry<T> entry = createVersionedEntry(data, source, strategy);
            
            // Store with appropriate TTL based on consistency level
            long ttlMinutes = consistencyManager.requiresStrongConsistency(cacheKey) ? 30 : 60;
            redisTemplate.opsForValue().set(cacheKey, entry, ttlMinutes, TimeUnit.MINUTES);
            
            log.debug("Stored versioned data for key '{}' with version {} and strategy {}", 
                cacheKey, entry.getVersion(), strategy);
                
        } catch (Exception e) {
            log.error("Failed to store versioned data for key '{}': {}", cacheKey, e.getMessage(), e);
            throw new RuntimeException("Cache versioning storage failed", e);
        }
    }

    /**
     * Retrieve versioned data from cache
     */
    @SuppressWarnings("unchecked")
    public <T> VersionedCacheEntry<T> getVersionedData(String cacheKey, Class<T> dataType) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof VersionedCacheEntry) {
                return (VersionedCacheEntry<T>) cached;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve versioned data for key '{}': {}", cacheKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update versioned data with conflict resolution
     */
    @SuppressWarnings("unchecked")
    public <T> boolean updateVersionedData(String cacheKey, T newData, String source) {
        try {
            VersionedCacheEntry<T> existing = getVersionedData(cacheKey, (Class<T>) newData.getClass());
            VersionedCacheEntry<T> newEntry = createVersionedEntry(newData, source);
            
            if (existing == null) {
                // No existing data, store new entry
                storeVersionedData(cacheKey, newData, source);
                return true;
            }
            
            // Apply conflict resolution strategy
            ConflictResolutionStrategy strategy = existing.getStrategy();
            boolean shouldUpdate = resolveConflict(existing, newEntry, strategy);
            
            if (shouldUpdate) {
                storeVersionedData(cacheKey, newData, source);
                log.debug("Updated versioned data for key '{}' using strategy {}", cacheKey, strategy);
                return true;
            } else {
                log.debug("Conflict resolution prevented update for key '{}' using strategy {}", cacheKey, strategy);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to update versioned data for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Determine conflict resolution strategy based on cache criticality
     */
    private ConflictResolutionStrategy determineStrategy(String cacheKey) {
        return consistencyManager.requiresStrongConsistency(cacheKey) 
            ? ConflictResolutionStrategy.OPTIMISTIC_LOCKING 
            : ConflictResolutionStrategy.LAST_WRITE_WINS;
    }

    /**
     * Resolve conflicts between cache entries
     */
    private <T> boolean resolveConflict(VersionedCacheEntry<T> existing, 
                                       VersionedCacheEntry<T> newEntry, 
                                       ConflictResolutionStrategy strategy) {
        switch (strategy) {
            case LAST_WRITE_WINS:
                return newEntry.isNewerThan(existing);

            case OPTIMISTIC_LOCKING:
                if (existing.hasConflict(newEntry)) {
                    log.warn("Optimistic locking conflict detected - rejecting update");
                    return false;
                }
                return newEntry.isNewerThan(existing);

            case MANUAL_RESOLUTION:
                // Application should handle this case
                log.info("Manual conflict resolution required");
                return false;

            default:
                return newEntry.isNewerThan(existing);
        }
    }

    /**
     * Check if cache entry is stale based on version
     */
    @SuppressWarnings("unchecked")
    public <T> boolean isStale(String cacheKey, Instant referenceVersion) {
        VersionedCacheEntry<T> entry = (VersionedCacheEntry<T>) getVersionedData(cacheKey, Object.class);
        if (entry == null || entry.getVersion() == null) return true;
        return entry.getVersion().isBefore(referenceVersion);
    }

    /**
     * Get cache entry version
     */
    public Instant getCacheVersion(String cacheKey) {
        VersionedCacheEntry<?> entry = getVersionedData(cacheKey, Object.class);
        return entry != null ? entry.getVersion() : null;
    }

    /**
     * Invalidate versioned cache entry
     */
    public void invalidateVersionedEntry(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
            // Increment version to track invalidation
            incrementCacheVersion(cacheKey);
            log.debug("Invalidated versioned cache entry for key '{}'", cacheKey);
        } catch (Exception e) {
            log.error("Failed to invalidate versioned cache entry for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Get current cache version for a key
     * Vietnamese Business Context: Lấy phiên bản cache hiện tại
     */
    public String getCurrentCacheVersion(String cacheKey) {
        try {
            String versionKey = cacheKey + ":version";
            String version = (String) redisTemplate.opsForValue().get(versionKey);
            return version != null ? version : "1.0.0";
        } catch (Exception e) {
            log.warn("Failed to get cache version for key '{}': {}", cacheKey, e.getMessage());
            return "1.0.0";
        }
    }

    /**
     * Increment cache version for invalidation tracking
     * Vietnamese Business Context: Tăng phiên bản cache để theo dõi vô hiệu hóa
     */
    public String incrementCacheVersion(String cacheKey) {
        try {
            String versionKey = cacheKey + ":version";
            String currentVersion = getCurrentCacheVersion(cacheKey);

            // Parse and increment version (simple semantic versioning)
            String[] parts = currentVersion.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // Increment patch version for cache invalidation
            patch++;
            String newVersion = String.format("%d.%d.%d", major, minor, patch);

            // Store new version with TTL
            redisTemplate.opsForValue().set(versionKey, newVersion, 24, TimeUnit.HOURS);

            log.debug("Incremented cache version for key '{}' from {} to {}", cacheKey, currentVersion, newVersion);
            return newVersion;

        } catch (Exception e) {
            log.error("Failed to increment cache version for key '{}': {}", cacheKey, e.getMessage(), e);
            return getCurrentCacheVersion(cacheKey);
        }
    }

    /**
     * Get cache invalidation metadata for WebSocket coordination
     * Vietnamese Business Context: Lấy metadata vô hiệu hóa cache cho điều phối WebSocket
     */
    public Map<String, Object> getCacheInvalidationMetadata(String cacheKey) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            metadata.put("cacheKey", cacheKey);
            metadata.put("version", getCurrentCacheVersion(cacheKey));
            metadata.put("timestamp", Instant.now());
            metadata.put("requiresRefresh", true);

            // Determine cache scope based on key pattern
            String scope = determineCacheScope(cacheKey);
            metadata.put("scope", scope);

            // Add Vietnamese business context
            metadata.put("lyDoVoHieuHoa", "Đồng bộ dữ liệu thời gian thực");

            return metadata;
        } catch (Exception e) {
            log.error("Failed to get cache invalidation metadata for key '{}': {}", cacheKey, e.getMessage(), e);
            metadata.put("error", e.getMessage());
            return metadata;
        }
    }

    /**
     * Determine cache scope for frontend coordination
     */
    private String determineCacheScope(String cacheKey) {
        if (cacheKey.contains("productData") || cacheKey.contains("sanPham")) {
            return "PRODUCT_DATA";
        } else if (cacheKey.contains("inventory") || cacheKey.contains("tonKho")) {
            return "INVENTORY_DATA";
        } else if (cacheKey.contains("voucher") || cacheKey.contains("phieuGiamGia") || cacheKey.contains("dotGiamGia")) {
            return "VOUCHER_DATA";
        } else if (cacheKey.contains("hoaDon") || cacheKey.contains("order")) {
            return "ORDER_DATA";
        } else if (cacheKey.contains("pricing") || cacheKey.contains("gia")) {
            return "PRICING_DATA";
        } else {
            return "GENERAL_DATA";
        }
    }
}
