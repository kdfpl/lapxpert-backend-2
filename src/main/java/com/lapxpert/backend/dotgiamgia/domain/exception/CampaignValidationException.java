package com.lapxpert.backend.dotgiamgia.domain.exception;

/**
 * Exception thrown when campaign validation fails
 * Provides specific error messaging for campaign business rule violations
 */
public class CampaignValidationException extends RuntimeException {
    
    /**
     * Constructor with validation message
     * @param message the validation error message
     */
    public CampaignValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructor with validation message and cause
     * @param message the validation error message
     * @param cause the underlying cause
     */
    public CampaignValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
