package com.lapxpert.backend.dotgiamgia.domain.exception;

/**
 * Exception thrown when a discount campaign is not found
 * Provides specific error messaging for campaign-related operations
 */
public class CampaignNotFoundException extends RuntimeException {
    
    /**
     * Constructor with campaign ID
     * @param id the campaign ID that was not found
     */
    public CampaignNotFoundException(Long id) {
        super("Không tìm thấy đợt giảm giá với ID: " + id);
    }
    
    /**
     * Constructor with campaign code
     * @param code the campaign code that was not found
     */
    public CampaignNotFoundException(String code) {
        super("Không tìm thấy đợt giảm giá với mã: " + code);
    }
    
    /**
     * Constructor with custom message
     * @param message custom error message
     */
    public CampaignNotFoundException(String message, boolean isCustomMessage) {
        super(message);
    }
}
