package com.lapxpert.backend.phieugiamgia.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CustomerPurchaseProfile {
    private Long customerId;
    private BigDecimal averageOrderValue;
    private Map<String, Integer> categoryPreferences;
    private List<String> frequentlyUsedVoucherTypes;
    private BigDecimal totalSavingsFromVouchers;
    private int orderFrequency;
    private String loyaltyLevel;

    public static CustomerPurchaseProfile defaultProfile() {
        return CustomerPurchaseProfile.builder()
            .customerId(null)
            .averageOrderValue(BigDecimal.valueOf(500000)) // Default 500K VND
            .categoryPreferences(Map.of())
            .frequentlyUsedVoucherTypes(List.of())
            .totalSavingsFromVouchers(BigDecimal.ZERO)
            .orderFrequency(0)
            .loyaltyLevel("NEW")
            .build();
    }
}
