package com.lapxpert.backend.phieugiamgia.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class FutureVoucherSuggestion {
    private String voucherCode;
    private String description;
    private BigDecimal minimumOrderValue;
    private BigDecimal discountValue;
    private String discountType;
    private Instant availableFrom;
    private String suggestionReason;
    private boolean isPersonalized;
}
