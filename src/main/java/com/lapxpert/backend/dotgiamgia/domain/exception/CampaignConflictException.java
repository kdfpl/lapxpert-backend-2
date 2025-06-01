package com.lapxpert.backend.dotgiamgia.domain.exception;

/**
 * Exception thrown when campaign conflicts are detected
 * Provides specific error messaging for campaign overlap scenarios
 */
public class CampaignConflictException extends RuntimeException {
    
    /**
     * Constructor with conflict message
     * @param message the conflict error message
     */
    public CampaignConflictException(String message) {
        super(message);
    }
    
    /**
     * Constructor for duplicate campaign code
     * @param code the duplicate campaign code
     */
    public static CampaignConflictException duplicateCode(String code) {
        return new CampaignConflictException("Mã đợt giảm giá đã tồn tại: " + code);
    }
    
    /**
     * Constructor for overlapping campaigns
     * @param campaignId the conflicting campaign ID
     */
    public static CampaignConflictException overlappingCampaign(Long campaignId) {
        return new CampaignConflictException("Đợt giảm giá có xung đột với đợt giảm giá khác (ID: " + campaignId + ")");
    }
}
