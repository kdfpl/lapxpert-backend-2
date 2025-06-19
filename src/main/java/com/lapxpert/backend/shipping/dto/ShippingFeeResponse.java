package com.lapxpert.backend.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shipping fee calculation response DTO
 * Contains calculated shipping fee and related information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeResponse {
    
    private boolean success;
    private String message;
    private String logId;
    
    // Shipping fee details
    private BigDecimal shippingFee;
    private BigDecimal insuranceFee;
    private BigDecimal totalFee;
    
    // Service information
    private String serviceName;
    private String serviceCode;
    private String estimatedDeliveryTime;
    
    // Provider information
    private String providerName;
    private LocalDateTime calculatedAt;
    
    // Error handling
    private String errorCode;
    private String errorMessage;
    
    /**
     * Create successful response
     */
    public static ShippingFeeResponse success(BigDecimal shippingFee, String providerName) {
        return ShippingFeeResponse.builder()
            .success(true)
            .shippingFee(shippingFee)
            .totalFee(shippingFee)
            .providerName(providerName)
            .calculatedAt(LocalDateTime.now())
            .message("Shipping fee calculated successfully")
            .build();
    }
    
    /**
     * Create successful response with detailed information
     */
    public static ShippingFeeResponse success(BigDecimal shippingFee, BigDecimal insuranceFee, 
                                            String serviceName, String providerName) {
        BigDecimal totalFee = shippingFee;
        if (insuranceFee != null) {
            totalFee = totalFee.add(insuranceFee);
        }
        
        return ShippingFeeResponse.builder()
            .success(true)
            .shippingFee(shippingFee)
            .insuranceFee(insuranceFee)
            .totalFee(totalFee)
            .serviceName(serviceName)
            .providerName(providerName)
            .calculatedAt(LocalDateTime.now())
            .message("Shipping fee calculated successfully")
            .build();
    }
    
    /**
     * Create error response
     */
    public static ShippingFeeResponse error(String errorMessage, String providerName) {
        return ShippingFeeResponse.builder()
            .success(false)
            .errorMessage(errorMessage)
            .providerName(providerName)
            .calculatedAt(LocalDateTime.now())
            .message("Failed to calculate shipping fee")
            .build();
    }
    
    /**
     * Create error response with error code
     */
    public static ShippingFeeResponse error(String errorCode, String errorMessage, String providerName) {
        return ShippingFeeResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .providerName(providerName)
            .calculatedAt(LocalDateTime.now())
            .message("Failed to calculate shipping fee")
            .build();
    }
}
