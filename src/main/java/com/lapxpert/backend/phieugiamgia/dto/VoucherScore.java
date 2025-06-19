package com.lapxpert.backend.phieugiamgia.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VoucherScore {
    private PhieuGiamGiaDto voucher;
    private BigDecimal discountAmount;
    private double effectivenessScore;
    private String explanation;
}
