package com.lapxpert.backend.phieugiamgia.domain.dto;

import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VoucherRecommendation {
    private PhieuGiamGiaDto voucher;
    private BigDecimal discountAmount;
    private double effectivenessScore;
    private String recommendationReason;
    private String categoryMatch;
    private boolean isPersonalized;
    private String urgencyMessage;
}
