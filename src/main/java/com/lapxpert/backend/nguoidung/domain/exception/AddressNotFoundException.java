package com.lapxpert.backend.nguoidung.domain.exception;

/**
 * Exception thrown when an address is not found
 * Provides specific error messaging for address-related operations
 */
public class AddressNotFoundException extends RuntimeException {
    
    /**
     * Constructor with address ID
     * @param id the address ID that was not found
     */
    public AddressNotFoundException(Long id) {
        super("Không tìm thấy địa chỉ với ID: " + id);
    }
    
    /**
     * Constructor with user ID for address lookup
     * @param userId the user ID
     * @param isForUser indicates this is for user address lookup
     */
    public AddressNotFoundException(Long userId, boolean isForUser) {
        super("Không tìm thấy địa chỉ cho người dùng với ID: " + userId);
    }
    
    /**
     * Constructor with custom message
     * @param message custom error message
     */
    public AddressNotFoundException(String message, boolean isCustomMessage) {
        super(message);
    }
    
    /**
     * Static factory method for default address not found
     * @param userId the user ID
     * @return AddressNotFoundException with default address message
     */
    public static AddressNotFoundException defaultAddress(Long userId) {
        return new AddressNotFoundException("Không tìm thấy địa chỉ mặc định cho người dùng với ID: " + userId, true);
    }
}
