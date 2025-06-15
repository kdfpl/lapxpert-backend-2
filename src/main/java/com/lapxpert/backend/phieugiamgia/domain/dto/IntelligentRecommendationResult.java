package com.lapxpert.backend.phieugiamgia.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IntelligentRecommendationResult {
    private boolean hasRecommendations;
    private VoucherRecommendation primaryRecommendation;
    private List<VoucherRecommendation> alternativeRecommendations;
    private List<FutureVoucherSuggestion> futureVoucherSuggestions;
    private CustomerPurchaseProfile customerProfile;
    private String explanationMessage;

    public static IntelligentRecommendationResult noRecommendations(String message) {
        return IntelligentRecommendationResult.builder()
            .hasRecommendations(false)
            .explanationMessage(message)
            .alternativeRecommendations(List.of())
            .futureVoucherSuggestions(List.of())
            .build();
    }
}
