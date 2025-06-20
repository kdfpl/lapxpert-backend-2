package com.lapxpert.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Optimistic Locking Service for handling version conflicts
 * Provides retry mechanisms for optimistic locking failures
 * Uses Vietnamese error messages and comprehensive logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockingService {

    // Retry configuration constants
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 100;
    private static final double RETRY_MULTIPLIER = 2.0;

    /**
     * Execute operation with optimistic locking retry mechanism
     * @param operation operation to execute
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if all retry attempts fail
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES);
    }

    /**
     * Execute operation with optimistic locking retry mechanism and constraint violation handling
     * @param operation operation to execute
     * @param entityName entity name for error messages
     * @param entityId entity ID for error messages
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if all retry attempts fail or constraint violation occurs
     */
    public <T> T executeWithRetryAndConstraintHandling(Supplier<T> operation, String entityName, Object entityId) {
        return executeWithRetryAndConstraintHandling(operation, entityName, entityId, DEFAULT_MAX_RETRIES);
    }

    /**
     * Execute operation with optimistic locking retry mechanism, constraint violation handling, and custom retry count
     * @param operation operation to execute
     * @param entityName entity name for error messages
     * @param entityId entity ID for error messages
     * @param maxRetries maximum number of retry attempts
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if all retry attempts fail or constraint violation occurs
     */
    public <T> T executeWithRetryAndConstraintHandling(Supplier<T> operation, String entityName, Object entityId, int maxRetries) {
        try {
            return executeWithRetry(operation, maxRetries);
        } catch (DataIntegrityViolationException e) {
            String errorMessage = formatConstraintViolationError(e, entityName, entityId);
            log.error("Constraint violation for {} {}: {}", entityName, entityId, errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Execute operation with optimistic locking retry mechanism and custom retry count
     * @param operation operation to execute
     * @param maxRetries maximum number of retry attempts
     * @param <T> return type
     * @return operation result
     * @throws RuntimeException if all retry attempts fail
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries) {
        OptimisticLockingFailureException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Optimistic locking attempt {} of {}", attempt, maxRetries);
                T result = operation.get();
                
                if (attempt > 1) {
                    log.info("Optimistic locking succeeded on attempt {} of {}", attempt, maxRetries);
                }
                
                return result;
                
            } catch (OptimisticLockingFailureException e) {
                lastException = e;
                log.warn("Optimistic locking failure on attempt {} of {}: {}", 
                        attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    try {
                        log.debug("Waiting {}ms before retry attempt {}", delayMs, attempt + 1);
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Bị gián đoạn khi thử lại optimistic locking", ie);
                    }
                } else {
                    log.error("Optimistic locking failed after {} attempts", maxRetries);
                }
            }
        }
        
        throw new RuntimeException("Xung đột dữ liệu: Không thể cập nhật sau " + maxRetries + 
                                 " lần thử. Vui lòng thử lại sau.", lastException);
    }

    /**
     * Execute void operation with optimistic locking retry mechanism
     * @param operation operation to execute
     */
    public void executeWithRetry(Runnable operation) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Execute void operation with optimistic locking retry mechanism and custom retry count
     * @param operation operation to execute
     * @param maxRetries maximum number of retry attempts
     */
    public void executeWithRetry(Runnable operation, int maxRetries) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxRetries);
    }

    /**
     * Calculate retry delay with exponential backoff
     * @param attemptNumber current attempt number (1-based)
     * @return delay in milliseconds
     */
    private long calculateRetryDelay(int attemptNumber) {
        return (long) (BASE_RETRY_DELAY_MS * Math.pow(RETRY_MULTIPLIER, attemptNumber - 1));
    }

    /**
     * Handle optimistic locking exception with Vietnamese error message
     * @param e the optimistic locking exception
     * @param entityName name of the entity that failed to update
     * @param entityId ID of the entity that failed to update
     * @return formatted error message
     */
    public String formatOptimisticLockingError(OptimisticLockingFailureException e, 
                                             String entityName, Object entityId) {
        log.error("Optimistic locking failure for {} with ID {}: {}", 
                entityName, entityId, e.getMessage());
        
        return String.format("Dữ liệu %s (ID: %s) đã được cập nhật bởi người dùng khác. " +
                           "Vui lòng tải lại trang và thử lại.", entityName, entityId);
    }

    /**
     * Check if exception is an optimistic locking failure
     * @param throwable exception to check
     * @return true if it's an optimistic locking failure
     */
    public boolean isOptimisticLockingFailure(Throwable throwable) {
        if (throwable instanceof OptimisticLockingFailureException) {
            return true;
        }
        
        // Check if the cause is an optimistic locking failure
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause instanceof OptimisticLockingFailureException) {
                return true;
            }
            cause = cause.getCause();
        }
        
        return false;
    }

    /**
     * Create a retry-aware operation wrapper
     * @param operation original operation
     * @param entityName entity name for error messages
     * @param entityId entity ID for error messages
     * @param <T> return type
     * @return wrapped operation with retry logic
     */
    public <T> Supplier<T> createRetryWrapper(Supplier<T> operation, String entityName, Object entityId) {
        return () -> {
            try {
                return executeWithRetry(operation);
            } catch (RuntimeException e) {
                if (isOptimisticLockingFailure(e)) {
                    throw new RuntimeException(formatOptimisticLockingError(
                            (OptimisticLockingFailureException) e, entityName, entityId), e);
                }
                throw e;
            }
        };
    }

    /**
     * Create a retry-aware void operation wrapper
     * @param operation original operation
     * @param entityName entity name for error messages
     * @param entityId entity ID for error messages
     * @return wrapped operation with retry logic
     */
    public Runnable createRetryWrapper(Runnable operation, String entityName, Object entityId) {
        return () -> {
            try {
                executeWithRetry(operation);
            } catch (RuntimeException e) {
                if (isOptimisticLockingFailure(e)) {
                    throw new RuntimeException(formatOptimisticLockingError(
                            (OptimisticLockingFailureException) e, entityName, entityId), e);
                }
                throw e;
            }
        };
    }

    /**
     * Log optimistic locking statistics
     * @param entityName entity name
     * @param successfulAttempts number of successful attempts
     * @param failedAttempts number of failed attempts
     */
    public void logOptimisticLockingStats(String entityName, int successfulAttempts, int failedAttempts) {
        if (failedAttempts > 0) {
            log.info("Optimistic locking stats for {}: {} successful, {} failed attempts",
                    entityName, successfulAttempts, failedAttempts);
        }
    }

    /**
     * Format constraint violation error message in Vietnamese
     * @param e constraint violation exception
     * @param entityName entity name for error message
     * @param entityId entity ID for error message
     * @return formatted error message
     */
    private String formatConstraintViolationError(DataIntegrityViolationException e, String entityName, Object entityId) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }

        // Check for specific constraint violations and provide Vietnamese error messages
        if (message.contains("uk_serial_number_order_allocation")) {
            return "Lỗi phân bổ serial number: Không thể phân bổ cùng một serial number cho nhiều đơn hàng";
        } else if (message.contains("chk_serial_number_allocation_consistency")) {
            return "Lỗi tính nhất quán phân bổ serial number: Trạng thái và thông tin đặt trước không khớp";
        } else if (message.contains("uk_serial_number_value")) {
            return "Lỗi trùng lặp serial number: Serial number đã tồn tại trong hệ thống";
        } else if (message.contains("unique") || message.contains("duplicate")) {
            return String.format("Lỗi ràng buộc duy nhất cho %s ID %s: Dữ liệu đã tồn tại", entityName, entityId);
        } else if (message.contains("foreign key") || message.contains("violates")) {
            return String.format("Lỗi ràng buộc dữ liệu cho %s ID %s: Vi phạm quy tắc tham chiếu", entityName, entityId);
        } else {
            return String.format("Lỗi ràng buộc dữ liệu cho %s ID %s: %s", entityName, entityId, message);
        }
    }
}
