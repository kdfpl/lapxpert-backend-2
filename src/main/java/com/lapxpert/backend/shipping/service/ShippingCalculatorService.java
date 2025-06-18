package com.lapxpert.backend.shipping.service;

import com.lapxpert.backend.shipping.dto.ShippingRequest;
import com.lapxpert.backend.shipping.dto.ShippingFeeResponse;
import org.springframework.cache.annotation.Cacheable;

/**
 * Abstract base class for shipping fee calculation services
 * Provides common interface and caching functionality for different shipping providers
 */
public abstract class ShippingCalculatorService {
    
    /**
     * Calculate shipping fee with caching
     * Cache key is based on request hashCode which includes essential shipping parameters
     * Uses medium TTL (30 minutes) as configured in Redis
     */
    @Cacheable(value = "shippingFees", key = "#request.hashCode()")
    public abstract ShippingFeeResponse calculateShippingFee(ShippingRequest request);
    
    /**
     * Get the name of the shipping provider
     */
    public abstract String getProviderName();
    
    /**
     * Check if this shipping provider is available/enabled
     */
    public abstract boolean isAvailable();
    
    /**
     * Get the priority of this shipping provider
     * Lower numbers have higher priority
     * Used when multiple providers are available
     */
    public int getPriority() {
        return 100; // Default priority
    }
    
    /**
     * Validate shipping request before processing
     * Subclasses can override for provider-specific validation
     */
    protected boolean validateRequest(ShippingRequest request) {
        return request != null && request.isValid();
    }
    
    /**
     * Get validation error message for invalid requests
     */
    protected String getValidationError(ShippingRequest request) {
        if (request == null) {
            return "Shipping request cannot be null";
        }
        return request.getValidationError();
    }
    
    /**
     * Create fallback response when calculation fails
     * Subclasses can override for provider-specific fallback logic
     */
    protected ShippingFeeResponse createFallbackResponse(String errorMessage) {
        return ShippingFeeResponse.error(
            "CALCULATION_FAILED", 
            errorMessage + " - Manual shipping fee entry required", 
            getProviderName()
        );
    }
}
