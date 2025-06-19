package com.lapxpert.backend.phieugiamgia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for voucher suggestion data.
 * Contains information about better voucher alternatives with savings calculations.
 * Used by VoucherSuggestionEngine for intelligent voucher recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSuggestionDto implements Serializable {
    
    private Long voucherId;
    private String voucherCode;
    private String voucherDescription;
    private String discountType; // PHAN_TRAM or SO_TIEN_CO_DINH
    private BigDecimal discountValue;
    private BigDecimal minimumOrderValue;
    private BigDecimal savingsAmount;
    private BigDecimal orderTotal;
    private String suggestionType; // BETTER_SINGLE, BETTER_COMBINATION, etc.
    private Integer remainingUsage;
    private Instant expirationTime;
    private Integer priority;
    private String message;
    private Instant timestamp;
    
    // Additional metadata for frontend display
    private String formattedSavingsAmount;
    private String formattedDiscountValue;
    private String formattedMinimumOrderValue;
    private String expirationTimeVietnam;
    private Boolean isExpiringSoon;
    private Boolean isLimitedQuantity;
}
