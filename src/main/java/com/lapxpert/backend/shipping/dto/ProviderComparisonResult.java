package com.lapxpert.backend.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider comparison result DTO
 * Contains comparison results between multiple shipping providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderComparisonResult {
    
    // Comparison metadata
    private LocalDateTime comparedAt;
    private String comparisonId;
    private boolean hasValidOptions;
    
    // Selected provider
    private ShippingFeeResponse selectedProvider;
    private String selectionReason;
    
    // All provider results
    private List<ProviderResult> allProviders;
    
    // Comparison criteria used
    private ComparisonCriteria criteria;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderResult {
        private String providerName;
        private ShippingFeeResponse response;
        private boolean isAvailable;
        private boolean isSuccessful;
        private BigDecimal totalScore;
        private BigDecimal costScore;
        private BigDecimal reliabilityScore;
        private BigDecimal speedScore;
        private String failureReason;
        private long responseTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonCriteria {
        // Weights for scoring (should sum to 1.0)
        @Builder.Default
        private BigDecimal costWeight = new BigDecimal("0.5"); // 50% weight on cost
        
        @Builder.Default
        private BigDecimal reliabilityWeight = new BigDecimal("0.3"); // 30% weight on reliability
        
        @Builder.Default
        private BigDecimal speedWeight = new BigDecimal("0.2"); // 20% weight on delivery speed
        
        // Provider preferences
        @Builder.Default
        private String preferredProvider = "GHN"; // Default preference
        
        @Builder.Default
        private String fallbackProvider = "GHTK"; // Fallback preference
        
        // Thresholds
        @Builder.Default
        private BigDecimal maxAcceptableFee = new BigDecimal("500000"); // 500k VND max
        
        @Builder.Default
        private long maxResponseTimeMs = 10000L; // 10 seconds max response time
        
        @Builder.Default
        private BigDecimal minReliabilityScore = new BigDecimal("0.7"); // 70% min reliability
    }
    
    /**
     * Check if comparison has any valid shipping options
     */
    public boolean hasValidShippingOptions() {
        return hasValidOptions && selectedProvider != null && selectedProvider.isSuccess();
    }
    
    /**
     * Get the best shipping fee from selected provider
     */
    public BigDecimal getBestShippingFee() {
        if (selectedProvider != null && selectedProvider.getTotalFee() != null) {
            return selectedProvider.getTotalFee();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get the selected provider name
     */
    public String getSelectedProviderName() {
        if (selectedProvider != null) {
            return selectedProvider.getProviderName();
        }
        return "MANUAL";
    }
    
    /**
     * Create a failed comparison result
     */
    public static ProviderComparisonResult failed(String reason) {
        return ProviderComparisonResult.builder()
            .comparedAt(LocalDateTime.now())
            .hasValidOptions(false)
            .selectionReason(reason)
            .build();
    }
    
    /**
     * Create a successful comparison result
     */
    public static ProviderComparisonResult success(ShippingFeeResponse selectedProvider, 
                                                 List<ProviderResult> allProviders,
                                                 String selectionReason,
                                                 ComparisonCriteria criteria) {
        return ProviderComparisonResult.builder()
            .comparedAt(LocalDateTime.now())
            .comparisonId("CMP-" + System.currentTimeMillis())
            .hasValidOptions(true)
            .selectedProvider(selectedProvider)
            .selectionReason(selectionReason)
            .allProviders(allProviders)
            .criteria(criteria)
            .build();
    }
}
