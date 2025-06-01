package com.lapxpert.backend.nguoidung.domain.exception;

/**
 * Exception thrown when a user is not found
 * Provides specific error messaging for user-related operations
 */
public class UserNotFoundException extends RuntimeException {
    
    /**
     * Constructor with user ID
     * @param id the user ID that was not found
     */
    public UserNotFoundException(Long id) {
        super("Không tìm thấy người dùng với ID: " + id);
    }
    
    /**
     * Constructor with email
     * @param email the email that was not found
     */
    public UserNotFoundException(String email) {
        super("Không tìm thấy người dùng với email: " + email);
    }
    
    /**
     * Constructor with custom message
     * @param message custom error message
     */
    public UserNotFoundException(String message, boolean isCustomMessage) {
        super(message);
    }
    
    /**
     * Static factory method for customer not found
     * @param id customer ID
     * @return UserNotFoundException with customer-specific message
     */
    public static UserNotFoundException customer(Long id) {
        return new UserNotFoundException("Không tìm thấy khách hàng với ID: " + id);
    }
    
    /**
     * Static factory method for staff not found
     * @param id staff ID
     * @return UserNotFoundException with staff-specific message
     */
    public static UserNotFoundException staff(Long id) {
        return new UserNotFoundException("Không tìm thấy nhân viên với ID: " + id);
    }
}
