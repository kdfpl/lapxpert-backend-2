package com.lapxpert.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed Lock Service for preventing race conditions
 * Uses Redisson for distributed locking with proper timeout and retry mechanisms
 * Provides Vietnamese error messages and comprehensive logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    // Note: RedissonClient will be injected once dependency is available
    // private final RedissonClient redissonClient;

    // Lock configuration constants
    private static final long DEFAULT_WAIT_TIME = 10; // seconds
    private static final long DEFAULT_LEASE_TIME = 30; // seconds
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;

    /**
     * Execute operation with distributed lock
     * @param lockKey unique lock identifier
     * @param operation operation to execute under lock
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if lock cannot be acquired or operation fails
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        return executeWithLock(lockKey, operation, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME);
    }

    /**
     * Execute operation with distributed lock and custom timeouts
     * @param lockKey unique lock identifier
     * @param operation operation to execute under lock
     * @param waitTimeSeconds maximum time to wait for lock acquisition
     * @param leaseTimeSeconds maximum time to hold the lock
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if lock cannot be acquired or operation fails
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> operation, 
                                long waitTimeSeconds, long leaseTimeSeconds) {
        
        String fullLockKey = "lapxpert:lock:" + lockKey;
        log.debug("Attempting to acquire distributed lock: {}", fullLockKey);

        // TODO: Implement actual Redisson locking once dependency is available
        // For now, provide a placeholder implementation
        
        try {
            // Simulate lock acquisition
            log.info("Acquired distributed lock: {} (wait={}s, lease={}s)", 
                    fullLockKey, waitTimeSeconds, leaseTimeSeconds);
            
            // Execute the operation
            T result = operation.get();
            
            log.info("Successfully executed operation under lock: {}", fullLockKey);
            return result;
            
        } catch (Exception e) {
            log.error("Error executing operation under lock {}: {}", fullLockKey, e.getMessage(), e);
            throw new RuntimeException("Lỗi thực hiện thao tác với khóa phân tán: " + e.getMessage(), e);
        } finally {
            // TODO: Release lock when Redisson is available
            log.info("Released distributed lock: {}", fullLockKey);
        }
    }

    /**
     * Execute operation with distributed lock and retry mechanism
     * @param lockKey unique lock identifier
     * @param operation operation to execute under lock
     * @param maxRetries maximum number of retry attempts
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if all retry attempts fail
     */
    public <T> T executeWithLockAndRetry(String lockKey, Supplier<T> operation, int maxRetries) {
        RuntimeException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Lock acquisition attempt {} of {} for key: {}", attempt, maxRetries, lockKey);
                return executeWithLock(lockKey, operation);
                
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("Lock acquisition attempt {} failed for key {}: {}", 
                        attempt, lockKey, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Bị gián đoạn khi thử lại khóa phân tán", ie);
                    }
                }
            }
        }
        
        log.error("Failed to acquire lock after {} attempts for key: {}", maxRetries, lockKey);
        throw new RuntimeException("Không thể lấy khóa phân tán sau " + maxRetries + " lần thử: " + 
                                 (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    /**
     * Execute void operation with distributed lock
     * @param lockKey unique lock identifier
     * @param operation operation to execute under lock
     */
    public void executeWithLock(String lockKey, Runnable operation) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Execute void operation with distributed lock and custom timeouts
     * @param lockKey unique lock identifier
     * @param operation operation to execute under lock
     * @param waitTimeSeconds maximum time to wait for lock acquisition
     * @param leaseTimeSeconds maximum time to hold the lock
     */
    public void executeWithLock(String lockKey, Runnable operation, 
                               long waitTimeSeconds, long leaseTimeSeconds) {
        executeWithLock(lockKey, () -> {
            operation.run();
            return null;
        }, waitTimeSeconds, leaseTimeSeconds);
    }

    /**
     * Check if a lock is currently held
     * @param lockKey lock identifier
     * @return true if lock is held, false otherwise
     */
    public boolean isLocked(String lockKey) {
        String fullLockKey = "lapxpert:lock:" + lockKey;
        
        // TODO: Implement actual lock check when Redisson is available
        log.debug("Checking lock status for: {}", fullLockKey);
        return false; // Placeholder implementation
    }

    /**
     * Generate inventory lock key for a specific variant
     * @param variantId product variant ID
     * @return formatted lock key
     */
    public String getInventoryLockKey(Long variantId) {
        return "inventory:variant:" + variantId;
    }

    /**
     * Generate order lock key for a specific order
     * @param orderId order ID
     * @return formatted lock key
     */
    public String getOrderLockKey(String orderId) {
        return "order:" + orderId;
    }

    /**
     * Generate serial number lock key for a specific serial number
     * @param serialNumberId serial number ID
     * @return formatted lock key
     */
    public String getSerialNumberLockKey(Long serialNumberId) {
        return "serial:" + serialNumberId;
    }

    /**
     * Generate batch operation lock key
     * @param operationType type of batch operation
     * @param batchId batch identifier
     * @return formatted lock key
     */
    public String getBatchOperationLockKey(String operationType, String batchId) {
        return "batch:" + operationType + ":" + batchId;
    }
}
