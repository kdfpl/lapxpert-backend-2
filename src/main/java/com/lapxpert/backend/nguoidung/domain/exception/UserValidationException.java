package com.lapxpert.backend.nguoidung.domain.exception;

/**
 * Exception thrown when user validation fails
 * Provides specific error messaging for user business rule violations
 */
public class UserValidationException extends RuntimeException {
    
    /**
     * Constructor with validation message
     * @param message the validation error message
     */
    public UserValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructor with validation message and cause
     * @param message the validation error message
     * @param cause the underlying cause
     */
    public UserValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Static factory method for invalid name format
     * @param name the invalid name
     * @return UserValidationException with name-specific message
     */
    public static UserValidationException invalidName(String name) {
        return new UserValidationException("Họ tên không hợp lệ: " + name + ". Họ tên phải có ít nhất 2 phần.");
    }
    
    /**
     * Static factory method for invalid role
     * @param role the invalid role
     * @return UserValidationException with role-specific message
     */
    public static UserValidationException invalidRole(String role) {
        return new UserValidationException("Vai trò không hợp lệ: " + role);
    }
}
