package com.lapxpert.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consistent error handling utilities with Vietnamese error messages.
 * Provides standardized error response formatting and exception handling
 * across all LapXpert business services.
 */
@Slf4j
public class ExceptionHandlingUtils {

    /**
     * Create business exception with Vietnamese message
     */
    public static RuntimeException createBusinessException(String message, Throwable cause) {
        log.error("Business exception: {}", message, cause);
        return new BusinessException(message, cause);
    }

    /**
     * Create business exception with Vietnamese message (no cause)
     */
    public static RuntimeException createBusinessException(String message) {
        log.error("Business exception: {}", message);
        return new BusinessException(message);
    }

    /**
     * Create not found exception with Vietnamese message
     */
    public static EntityNotFoundException createNotFoundException(String message) {
        log.warn("Entity not found: {}", message);
        return new EntityNotFoundException(message);
    }

    /**
     * Create validation exception with Vietnamese message
     */
    public static IllegalArgumentException createValidationException(String message) {
        log.warn("Validation error: {}", message);
        return new IllegalArgumentException(message);
    }

    /**
     * Create validation exception with multiple error messages
     */
    public static IllegalArgumentException createValidationException(List<String> messages) {
        String combinedMessage = String.join("; ", messages);
        log.warn("Multiple validation errors: {}", combinedMessage);
        return new IllegalArgumentException(combinedMessage);
    }

    /**
     * Create standardized error response
     */
    public static ResponseEntity<Map<String, Object>> createErrorResponse(
            String message, String errorCode, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Create validation error response with field details
     */
    public static ResponseEntity<Map<String, Object>> createValidationErrorResponse(
            String message, Map<String, String> fieldErrors) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "VALIDATION_ERROR");
        errorResponse.put("message", message);
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Create not found error response
     */
    public static ResponseEntity<Map<String, Object>> createNotFoundResponse(String message) {
        return createErrorResponse(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * Create business error response
     */
    public static ResponseEntity<Map<String, Object>> createBusinessErrorResponse(String message) {
        return createErrorResponse(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST);
    }

    /**
     * Create internal server error response
     */
    public static ResponseEntity<Map<String, Object>> createInternalErrorResponse(String message) {
        return createErrorResponse(
            message != null ? message : "Đã xảy ra lỗi hệ thống", 
            "INTERNAL_ERROR", 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Create conflict error response (for duplicate data)
     */
    public static ResponseEntity<Map<String, Object>> createConflictResponse(String message) {
        return createErrorResponse(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    /**
     * Create unauthorized error response
     */
    public static ResponseEntity<Map<String, Object>> createUnauthorizedResponse(String message) {
        return createErrorResponse(
            message != null ? message : "Không có quyền truy cập", 
            "UNAUTHORIZED", 
            HttpStatus.UNAUTHORIZED
        );
    }

    /**
     * Create forbidden error response
     */
    public static ResponseEntity<Map<String, Object>> createForbiddenResponse(String message) {
        return createErrorResponse(
            message != null ? message : "Không có quyền thực hiện thao tác này", 
            "FORBIDDEN", 
            HttpStatus.FORBIDDEN
        );
    }

    /**
     * Handle and convert exception to appropriate response
     */
    public static ResponseEntity<Map<String, Object>> handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return createValidationErrorResponse(e.getMessage(), null);
        } else if (e instanceof EntityNotFoundException) {
            return createNotFoundResponse(e.getMessage());
        } else if (e instanceof BusinessException) {
            return createBusinessErrorResponse(e.getMessage());
        } else {
            log.error("Unexpected error", e);
            return createInternalErrorResponse("Đã xảy ra lỗi hệ thống không mong muốn");
        }
    }

    /**
     * Get Vietnamese error message for common HTTP status codes
     */
    public static String getVietnameseErrorMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Không có quyền truy cập";
            case FORBIDDEN -> "Không có quyền thực hiện thao tác này";
            case NOT_FOUND -> "Không tìm thấy dữ liệu";
            case CONFLICT -> "Dữ liệu đã tồn tại";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            default -> "Đã xảy ra lỗi";
        };
    }

    /**
     * Custom business exception class
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }

        public BusinessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Aggregate multiple validation errors
     */
    public static class ValidationErrorAggregator {
        private final Map<String, String> fieldErrors = new HashMap<>();
        private final StringBuilder generalErrors = new StringBuilder();

        public void addFieldError(String field, String message) {
            fieldErrors.put(field, message);
        }

        public void addGeneralError(String message) {
            if (generalErrors.length() > 0) {
                generalErrors.append("; ");
            }
            generalErrors.append(message);
        }

        public boolean hasErrors() {
            return !fieldErrors.isEmpty() || generalErrors.length() > 0;
        }

        public ResponseEntity<Map<String, Object>> createErrorResponse() {
            if (!hasErrors()) {
                return null;
            }

            String message = generalErrors.length() > 0 ? 
                generalErrors.toString() : 
                "Dữ liệu đầu vào không hợp lệ";

            return createValidationErrorResponse(message, fieldErrors);
        }

        public IllegalArgumentException createException() {
            if (!hasErrors()) {
                return null;
            }

            String message = generalErrors.length() > 0 ? 
                generalErrors.toString() : 
                "Dữ liệu đầu vào không hợp lệ";

            return new IllegalArgumentException(message);
        }
    }

    /**
     * Create error response with additional context
     */
    public static ResponseEntity<Map<String, Object>> createErrorResponseWithContext(
            String message, String errorCode, HttpStatus status, Map<String, Object> context) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        
        if (context != null && !context.isEmpty()) {
            errorResponse.put("context", context);
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Log and create business exception with context
     */
    public static RuntimeException createBusinessExceptionWithContext(
            String message, Map<String, Object> context, Throwable cause) {
        log.error("Business exception with context: {} - Context: {}", message, context, cause);
        return new BusinessException(message, cause);
    }
}
