package com.lapxpert.backend.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for consistent response format across all endpoints.
 * Provides success/error status, data payload, message, and timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * Indicates if the operation was successful
     */
    private boolean success;
    
    /**
     * Response data payload
     */
    private T data;
    
    /**
     * Human-readable message describing the result
     */
    private String message;
    
    /**
     * Timestamp when the response was created
     */
    private Instant timestamp;
    
    /**
     * Error code for failed operations
     */
    private String errorCode;
    
    /**
     * Additional metadata or context information
     */
    private Object metadata;

    // Static factory methods for common response patterns

    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Operation completed successfully", Instant.now(), null, null);
    }

    /**
     * Create successful response with data and custom message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), null, null);
    }

    /**
     * Create successful response with data, message, and metadata
     */
    public static <T> ApiResponse<T> success(T data, String message, Object metadata) {
        return new ApiResponse<>(true, data, message, Instant.now(), null, metadata);
    }

    /**
     * Create error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), null, null);
    }

    /**
     * Create error response with message and error code
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, Instant.now(), errorCode, null);
    }

    /**
     * Create error response with message, error code, and metadata
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, Object metadata) {
        return new ApiResponse<>(false, null, message, Instant.now(), errorCode, metadata);
    }

    /**
     * Create response for validation errors
     */
    public static <T> ApiResponse<T> validationError(String message, Object validationErrors) {
        return new ApiResponse<>(false, null, message, Instant.now(), "VALIDATION_ERROR", validationErrors);
    }

    /**
     * Create response for not found errors
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "NOT_FOUND", null);
    }

    /**
     * Create response for unauthorized access
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "UNAUTHORIZED", null);
    }

    /**
     * Create response for forbidden access
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "FORBIDDEN", null);
    }

    /**
     * Create response for internal server errors
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "INTERNAL_ERROR", null);
    }

    /**
     * Create response for bad requests
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "BAD_REQUEST", null);
    }

    /**
     * Create response for conflict errors
     */
    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), "CONFLICT", null);
    }

    // Utility methods

    /**
     * Check if the response indicates success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the response indicates an error
     */
    public boolean isError() {
        return !success;
    }

    /**
     * Get data if successful, otherwise return null
     */
    public T getDataOrNull() {
        return success ? data : null;
    }

    /**
     * Get data if successful, otherwise return default value
     */
    public T getDataOrDefault(T defaultValue) {
        return success ? data : defaultValue;
    }

    /**
     * Add metadata to the response
     */
    public ApiResponse<T> withMetadata(Object metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Add error code to the response
     */
    public ApiResponse<T> withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Create a copy of this response with different data type
     */
    public <U> ApiResponse<U> map(U newData) {
        return new ApiResponse<>(this.success, newData, this.message, this.timestamp, this.errorCode, this.metadata);
    }

    /**
     * Create a copy of this response with different data and message
     */
    public <U> ApiResponse<U> map(U newData, String newMessage) {
        return new ApiResponse<>(this.success, newData, newMessage, this.timestamp, this.errorCode, this.metadata);
    }

    // Builder pattern support

    /**
     * Builder for creating custom ApiResponse instances
     */
    public static class Builder<T> {
        private boolean success;
        private T data;
        private String message;
        private Instant timestamp = Instant.now();
        private String errorCode;
        private Object metadata;

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder<T> errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder<T> metadata(Object metadata) {
            this.metadata = metadata;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(success, data, message, timestamp, errorCode, metadata);
        }
    }

    /**
     * Create a new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
