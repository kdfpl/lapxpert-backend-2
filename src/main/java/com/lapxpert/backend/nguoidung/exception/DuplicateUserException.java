package com.lapxpert.backend.nguoidung.exception;

/**
 * Exception thrown when duplicate user data is detected
 * Provides specific error messaging for uniqueness constraint violations
 */
public class DuplicateUserException extends RuntimeException {
    
    /**
     * Constructor with duplicate message
     * @param message the duplicate error message
     */
    public DuplicateUserException(String message) {
        super(message);
    }
    
    /**
     * Static factory method for duplicate email
     * @param email the duplicate email
     * @return DuplicateUserException with email-specific message
     */
    public static DuplicateUserException email(String email) {
        return new DuplicateUserException("Email đã tồn tại: " + email);
    }
    
    /**
     * Static factory method for duplicate phone
     * @param phone the duplicate phone number
     * @return DuplicateUserException with phone-specific message
     */
    public static DuplicateUserException phone(String phone) {
        return new DuplicateUserException("Số điện thoại đã tồn tại: " + phone);
    }
    
    /**
     * Static factory method for duplicate CCCD
     * @param cccd the duplicate CCCD number
     * @return DuplicateUserException with CCCD-specific message
     */
    public static DuplicateUserException cccd(String cccd) {
        return new DuplicateUserException("CCCD đã tồn tại: " + cccd);
    }
    
    /**
     * Static factory method for both email and phone duplicates
     * @param email the duplicate email
     * @param phone the duplicate phone
     * @return DuplicateUserException with combined message
     */
    public static DuplicateUserException emailAndPhone(String email, String phone) {
        return new DuplicateUserException("Email và số điện thoại đã tồn tại: " + email + ", " + phone);
    }
}
