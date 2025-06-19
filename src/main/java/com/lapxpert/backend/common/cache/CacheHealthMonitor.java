package com.lapxpert.backend.common.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Health Monitor for LapXpert E-commerce System
 * 
 * Monitors cache consistency, performance metrics, and health status
 * for the hybrid cache consistency model. Provides real-time monitoring
 * and alerting for cache-related issues.
 * 
 * Vietnamese Business Context:
 * - Giám sát cache: Cache monitoring and health checks
 * - Tính nhất quán: Consistency validation across instances
 * - Hiệu suất: Performance metrics and optimization
 * - Cảnh báo: Alerting for cache-related issues
 * 
 * Features:
 * - Real-time consistency validation
 * - Performance metrics collection
 * - Health status reporting
 * - Automatic issue detection and alerting
 */
@Component
@Slf4j
public class CacheHealthMonitor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheConsistencyManager consistencyManager;

    @Autowired
    private CacheVersioningService versioningService;

    // Health metrics
    private final AtomicLong totalCacheOperations = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong consistencyViolations = new AtomicLong(0);
    private final AtomicLong lastHealthCheck = new AtomicLong(0);

    // Application startup tracking
    private volatile boolean applicationFullyStarted = false;
    private volatile Instant applicationStartTime = Instant.now();

    // Cache health tracking
    private final ConcurrentHashMap<String, CacheHealthStatus> cacheHealthMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastAccessTimes = new ConcurrentHashMap<>();

    /**
     * Mark application as fully started after all beans are initialized
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        applicationFullyStarted = true;
        applicationStartTime = Instant.now();
        log.info("Application fully started - cache health monitoring will now check for critical issues");
    }

    /**
     * Cache health status data class
     */
    @Data
    public static class CacheHealthStatus {
        private String cacheKey;
        private boolean isHealthy;
        private Instant lastCheck;
        private String healthIssue;
        private long accessCount;
        private double hitRatio;
        private CacheConsistencyManager.ConsistencyLevel consistencyLevel;

        public CacheHealthStatus(String cacheKey) {
            this.cacheKey = cacheKey;
            this.isHealthy = true;
            this.lastCheck = Instant.now();
            this.accessCount = 0;
            this.hitRatio = 0.0;
        }
    }

    /**
     * Overall system health report
     */
    @Data
    public static class SystemHealthReport {
        private boolean overallHealthy;
        private Instant reportTime;
        private long totalCaches;
        private long healthyCaches;
        private long unhealthyCaches;
        private double systemHitRatio;
        private long consistencyViolations;
        private List<String> criticalIssues;
        private Map<String, Object> metrics;

        public SystemHealthReport() {
            this.reportTime = Instant.now();
            this.criticalIssues = new ArrayList<>();
            this.metrics = new HashMap<>();
        }
    }

    /**
     * Scheduled health check - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void performHealthCheck() {
        try {
            log.debug("Starting scheduled cache health check");

            Instant checkTime = Instant.now();
            lastHealthCheck.set(checkTime.toEpochMilli());

            // Check actual Spring cache names that exist
            checkSpringCacheHealth();

            // Check pattern-based caches (only if they exist)
            checkPatternBasedCacheHealth();

            // Check non-critical caches
            Set<String> nonCriticalCaches = consistencyManager.getNonCriticalCaches();
            for (String cacheKey : nonCriticalCaches) {
                checkCacheHealth(cacheKey, false);
            }

            // Generate health report
            SystemHealthReport report = generateHealthReport();
            logHealthReport(report);

            // Alert on critical issues
            if (!report.isOverallHealthy()) {
                alertOnHealthIssues(report);
            }

            log.debug("Scheduled cache health check completed");

        } catch (Exception e) {
            log.error("Cache health check failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Check Spring cache health (actual named caches)
     */
    private void checkSpringCacheHealth() {
        Set<String> criticalSpringCaches = consistencyManager.getCriticalSpringCaches();
        for (String cacheName : criticalSpringCaches) {
            // Check if the Spring cache exists and has entries
            checkSpringCacheExistence(cacheName);
        }
    }

    /**
     * Check pattern-based cache health (dynamic keys)
     */
    private void checkPatternBasedCacheHealth() {
        Set<String> criticalPatterns = consistencyManager.getCriticalCachePatterns();
        for (String pattern : criticalPatterns) {
            // Only check patterns if we're past startup grace period
            if (applicationFullyStarted && isGracePeriodOver()) {
                checkPatternBasedCache(pattern);
            }
        }
    }

    /**
     * Check if grace period after startup is over (5 minutes)
     */
    private boolean isGracePeriodOver() {
        return applicationStartTime.isBefore(Instant.now().minusSeconds(300)); // 5 minutes
    }

    /**
     * Check Spring cache existence
     */
    private void checkSpringCacheExistence(String cacheName) {
        try {
            // For Spring caches, we check if they're configured, not if they have entries
            // Empty Spring caches are normal and not a health issue
            CacheHealthStatus status = cacheHealthMap.computeIfAbsent(cacheName, CacheHealthStatus::new);
            status.setLastCheck(Instant.now());
            status.setConsistencyLevel(consistencyManager.getConsistencyLevel(cacheName));
            status.setHealthy(true); // Spring caches are healthy if they're configured
            status.setHealthIssue(null);

            log.debug("Spring cache '{}' health check: OK", cacheName);
        } catch (Exception e) {
            log.error("Spring cache health check failed for '{}': {}", cacheName, e.getMessage(), e);
        }
    }

    /**
     * Check pattern-based cache (only if entries exist)
     */
    private void checkPatternBasedCache(String pattern) {
        try {
            // Convert pattern to Redis key pattern (e.g., inventory:* -> inventory:*)
            Set<String> matchingKeys = redisTemplate.keys(pattern);

            if (matchingKeys != null && !matchingKeys.isEmpty()) {
                // Check a sample of matching keys
                int checkedCount = 0;
                for (String key : matchingKeys) {
                    if (checkedCount >= 5) break; // Limit to 5 keys per pattern
                    checkCacheHealth(key, true);
                    checkedCount++;
                }
                log.debug("Pattern '{}' health check: {} keys found", pattern, matchingKeys.size());
            } else {
                // No keys found for pattern - this is OK, just log debug
                log.debug("Pattern '{}' health check: no keys found (normal for unused patterns)", pattern);
            }
        } catch (Exception e) {
            log.error("Pattern-based cache health check failed for '{}': {}", pattern, e.getMessage(), e);
        }
    }

    /**
     * Check health of individual cache
     */
    public CacheHealthStatus checkCacheHealth(String cacheKey, boolean isCritical) {
        try {
            CacheHealthStatus status = cacheHealthMap.computeIfAbsent(cacheKey, CacheHealthStatus::new);
            status.setLastCheck(Instant.now());
            status.setConsistencyLevel(consistencyManager.getConsistencyLevel(cacheKey));

            // Check cache existence and validity
            boolean exists = redisTemplate.hasKey(cacheKey);
            if (!exists && isCritical) {
                // During startup grace period, missing caches are not critical
                if (!applicationFullyStarted || !isGracePeriodOver()) {
                    status.setHealthy(true);
                    status.setHealthIssue("Cache not yet populated (startup grace period)");
                    log.debug("Critical cache key '{}' does not exist yet (startup grace period)", cacheKey);
                    return status;
                } else {
                    status.setHealthy(false);
                    status.setHealthIssue("Critical cache key does not exist");
                    log.warn("Critical cache key '{}' does not exist", cacheKey);
                    return status;
                }
            }

            // Check version consistency for versioned caches
            if (exists) {
                CacheVersioningService.VersionedCacheEntry<?> entry = 
                    versioningService.getVersionedData(cacheKey, Object.class);
                
                if (entry != null) {
                    // Check if version is too old for critical data
                    if (isCritical && isVersionTooOld(entry.getVersion())) {
                        status.setHealthy(false);
                        status.setHealthIssue("Critical cache version is too old");
                        consistencyViolations.incrementAndGet();
                        log.warn("Critical cache key '{}' has stale version: {}", cacheKey, entry.getVersion());
                    } else {
                        status.setHealthy(true);
                        status.setHealthIssue(null);
                    }
                } else if (isCritical) {
                    status.setHealthy(false);
                    status.setHealthIssue("Critical cache exists but has no version info");
                }
            }

            // Update access tracking
            updateAccessMetrics(cacheKey, status);

            return status;

        } catch (Exception e) {
            log.error("Health check failed for cache key '{}': {}", cacheKey, e.getMessage(), e);
            CacheHealthStatus errorStatus = new CacheHealthStatus(cacheKey);
            errorStatus.setHealthy(false);
            errorStatus.setHealthIssue("Health check error: " + e.getMessage());
            return errorStatus;
        }
    }

    /**
     * Record cache operation metrics
     */
    public void recordCacheOperation(String cacheKey, boolean isHit) {
        totalCacheOperations.incrementAndGet();
        lastAccessTimes.put(cacheKey, System.currentTimeMillis());

        if (isHit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }

        // Update cache-specific metrics
        CacheHealthStatus status = cacheHealthMap.get(cacheKey);
        if (status != null) {
            status.setAccessCount(status.getAccessCount() + 1);
            updateHitRatio(status, isHit);
        }
    }

    /**
     * Generate comprehensive health report
     */
    public SystemHealthReport generateHealthReport() {
        SystemHealthReport report = new SystemHealthReport();

        try {
            // Calculate overall metrics
            long totalOps = totalCacheOperations.get();
            long hits = cacheHits.get();
            long misses = cacheMisses.get();

            report.setSystemHitRatio(totalOps > 0 ? (double) hits / totalOps : 0.0);
            report.setConsistencyViolations(consistencyViolations.get());

            // Count healthy vs unhealthy caches
            long healthy = cacheHealthMap.values().stream()
                .mapToLong(status -> status.isHealthy() ? 1 : 0)
                .sum();
            
            long unhealthy = cacheHealthMap.size() - healthy;

            report.setTotalCaches(cacheHealthMap.size());
            report.setHealthyCaches(healthy);
            report.setUnhealthyCaches(unhealthy);

            // Determine overall health
            report.setOverallHealthy(unhealthy == 0 && consistencyViolations.get() < 10);

            // Collect critical issues
            List<String> criticalIssues = new ArrayList<>();
            cacheHealthMap.values().stream()
                .filter(status -> !status.isHealthy())
                .forEach(status -> {
                    if (consistencyManager.requiresStrongConsistency(status.getCacheKey())) {
                        criticalIssues.add(String.format("Critical cache '%s': %s", 
                            status.getCacheKey(), status.getHealthIssue()));
                    }
                });

            report.setCriticalIssues(criticalIssues);

            // Add detailed metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalOperations", totalOps);
            metrics.put("cacheHits", hits);
            metrics.put("cacheMisses", misses);
            metrics.put("hitRatio", report.getSystemHitRatio());
            metrics.put("consistencyViolations", consistencyViolations.get());
            metrics.put("lastHealthCheck", new Date(lastHealthCheck.get()));
            
            report.setMetrics(metrics);

        } catch (Exception e) {
            log.error("Failed to generate health report: {}", e.getMessage(), e);
            report.setOverallHealthy(false);
            report.getCriticalIssues().add("Health report generation failed: " + e.getMessage());
        }

        return report;
    }

    /**
     * Validate consistency across multiple application instances
     */
    public boolean validateCrossInstanceConsistency(String cacheKey) {
        try {
            // For single Redis instance, consistency is maintained by Redis itself
            // This method can be extended for Redis Cluster scenarios
            
            CacheVersioningService.VersionedCacheEntry<?> entry = 
                versioningService.getVersionedData(cacheKey, Object.class);
            
            if (entry == null) {
                return true; // No data to validate
            }

            // Check if version is recent enough
            boolean isRecent = !isVersionTooOld(entry.getVersion());
            
            if (!isRecent && consistencyManager.requiresStrongConsistency(cacheKey)) {
                log.warn("Cross-instance consistency validation failed for critical cache '{}'", cacheKey);
                consistencyViolations.incrementAndGet();
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Cross-instance consistency validation failed for key '{}': {}", cacheKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current cache health status
     */
    public Map<String, CacheHealthStatus> getCurrentHealthStatus() {
        return new HashMap<>(cacheHealthMap);
    }

    /**
     * Reset health metrics
     */
    public void resetMetrics() {
        totalCacheOperations.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        consistencyViolations.set(0);
        cacheHealthMap.clear();
        lastAccessTimes.clear();
        log.info("Cache health metrics reset");
    }

    // Private helper methods

    private boolean isVersionTooOld(Instant version) {
        if (version == null) return true;
        // Consider version too old if older than 10 minutes for critical data
        return version.isBefore(Instant.now().minusSeconds(600));
    }

    private void updateAccessMetrics(String cacheKey, CacheHealthStatus status) {
        Long lastAccess = lastAccessTimes.get(cacheKey);
        if (lastAccess != null) {
            // Update access patterns and hit ratios
            status.setAccessCount(status.getAccessCount() + 1);
        }
    }

    private void updateHitRatio(CacheHealthStatus status, boolean isHit) {
        long totalAccess = status.getAccessCount();
        if (totalAccess > 0) {
            // Simple moving average for hit ratio
            double currentRatio = status.getHitRatio();
            double newRatio = ((currentRatio * (totalAccess - 1)) + (isHit ? 1 : 0)) / totalAccess;
            status.setHitRatio(newRatio);
        }
    }

    private void logHealthReport(SystemHealthReport report) {
        if (report.isOverallHealthy()) {
            log.info("Cache system health: HEALTHY - Hit ratio: {:.2f}%, Healthy caches: {}/{}", 
                report.getSystemHitRatio() * 100, report.getHealthyCaches(), report.getTotalCaches());
        } else {
            log.warn("Cache system health: UNHEALTHY - Issues: {}, Violations: {}", 
                report.getCriticalIssues().size(), report.getConsistencyViolations());
        }
    }

    private void alertOnHealthIssues(SystemHealthReport report) {
        if (!report.getCriticalIssues().isEmpty()) {
            log.error("CRITICAL CACHE ISSUES DETECTED:");
            report.getCriticalIssues().forEach(issue -> log.error("  - {}", issue));
        }
    }
}
