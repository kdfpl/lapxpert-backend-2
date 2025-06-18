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
            log.debug("Invalidated versioned cache entry for key '{}'", cacheKey);
        } catch (Exception e) {
            log.error("Failed to invalidate versioned cache entry for key '{}': {}", cacheKey, e.getMessage(), e);
        }
    }
}
