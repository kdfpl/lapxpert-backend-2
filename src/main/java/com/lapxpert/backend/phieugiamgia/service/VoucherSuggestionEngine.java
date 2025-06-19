package com.lapxpert.backend.phieugiamgia.service;

import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.dto.VoucherSuggestionDto;
import com.lapxpert.backend.phieugiamgia.repository.PhieuGiamGiaRepository;
import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent voucher suggestion engine for LapXpert.
 * Provides smart voucher recommendations based on order value, customer eligibility,
 * and savings potential. Integrates with existing voucher validation system.
 * 
 * Features:
 * - Calculates accurate voucher value for comparison
 * - Detects better voucher alternatives
 * - Ranks suggestions by savings potential
 * - Supports both percentage and fixed amount vouchers
 * - Maintains Vietnamese business terminology
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherSuggestionEngine {
    
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaService phieuGiamGiaService;
    
    /**
     * Calculate the actual discount value for a voucher given an order total
     */
    public BigDecimal calculateVoucherValue(PhieuGiamGia voucher, BigDecimal orderTotal) {
        if (voucher == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Check minimum order value requirement
        if (voucher.getGiaTriDonHangToiThieu() != null && 
            orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
            return BigDecimal.ZERO;
        }
        
        if (voucher.getLoaiGiamGia() == LoaiGiamGia.PHAN_TRAM) {
            // Percentage discount: (orderTotal * percentage) / 100
            return orderTotal.multiply(voucher.getGiaTriGiam())
                           .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            // Fixed amount discount
            return voucher.getGiaTriGiam().min(orderTotal); // Cannot exceed order total
        }
    }
    
    /**
     * Detect better voucher alternatives for current vouchers
     */
    @Transactional(readOnly = true)
    public List<VoucherSuggestionDto> detectBetterVouchers(
            List<String> currentVoucherCodes, 
            Long customerId, 
            BigDecimal orderTotal) {
        
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }
        
        log.debug("Detecting better vouchers for customer {} with order total {}", customerId, orderTotal);
        
        // Get all eligible vouchers for this customer and order
        List<PhieuGiamGia> eligibleVouchers = getEligibleVouchers(customerId, orderTotal);
        
        // Calculate current total discount
        BigDecimal currentTotalDiscount = calculateCurrentTotalDiscount(currentVoucherCodes, orderTotal);
        
        // Find better alternatives
        List<VoucherSuggestionDto> suggestions = new ArrayList<>();
        
        // Single voucher alternatives
        for (PhieuGiamGia voucher : eligibleVouchers) {
            if (currentVoucherCodes.contains(voucher.getMaPhieuGiamGia())) {
                continue; // Skip vouchers already in use
            }
            
            BigDecimal voucherValue = calculateVoucherValue(voucher, orderTotal);
            if (voucherValue.compareTo(currentTotalDiscount) > 0) {
                suggestions.add(createVoucherSuggestion(voucher, voucherValue, orderTotal, "BETTER_SINGLE"));
            }
        }
        
        // Sort by savings potential (highest first)
        suggestions.sort((a, b) -> b.getSavingsAmount().compareTo(a.getSavingsAmount()));
        
        // Limit to top 5 suggestions to avoid overwhelming users
        return suggestions.stream().limit(5).collect(Collectors.toList());
    }
    
    /**
     * Get all vouchers eligible for a customer and order total
     */
    private List<PhieuGiamGia> getEligibleVouchers(Long customerId, BigDecimal orderTotal) {
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);
        
        return activeVouchers.stream()
            .filter(voucher -> {
                // Check usage limits
                if (voucher.getSoLuongDaDung() >= voucher.getSoLuongBanDau()) {
                    return false;
                }
                
                // Check minimum order value
                if (voucher.getGiaTriDonHangToiThieu() != null &&
                    orderTotal.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
                    return false;
                }
                
                // Check customer eligibility
                if (customerId != null && !voucher.isCustomerEligible(customerId)) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate total discount from current vouchers
     */
    private BigDecimal calculateCurrentTotalDiscount(List<String> voucherCodes, BigDecimal orderTotal) {
        if (voucherCodes == null || voucherCodes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        for (String code : voucherCodes) {
            Optional<PhieuGiamGia> voucherOpt = phieuGiamGiaRepository.findByMaPhieuGiamGia(code);
            if (voucherOpt.isPresent()) {
                BigDecimal discount = calculateVoucherValue(voucherOpt.get(), orderTotal);
                totalDiscount = totalDiscount.add(discount);
            }
        }
        
        return totalDiscount;
    }
    
    /**
     * Create a voucher suggestion DTO
     */
    private VoucherSuggestionDto createVoucherSuggestion(
            PhieuGiamGia voucher, 
            BigDecimal savingsAmount, 
            BigDecimal orderTotal,
            String suggestionType) {
        
        return VoucherSuggestionDto.builder()
            .voucherId(voucher.getId())
            .voucherCode(voucher.getMaPhieuGiamGia())
            .voucherDescription(voucher.getMoTa())
            .discountType(voucher.getLoaiGiamGia().name())
            .discountValue(voucher.getGiaTriGiam())
            .minimumOrderValue(voucher.getGiaTriDonHangToiThieu())
            .savingsAmount(savingsAmount)
            .orderTotal(orderTotal)
            .suggestionType(suggestionType)
            .remainingUsage(voucher.getRemainingUsage())
            .expirationTime(voucher.getNgayKetThuc())
            .priority(calculatePriority(voucher, savingsAmount, orderTotal))
            .message(generateSuggestionMessage(voucher, savingsAmount))
            .build();
    }
    
    /**
     * Calculate suggestion priority (higher is better)
     */
    private int calculatePriority(PhieuGiamGia voucher, BigDecimal savingsAmount, BigDecimal orderTotal) {
        int priority = 0;
        
        // Higher savings = higher priority
        BigDecimal savingsPercentage = savingsAmount.divide(orderTotal, 4, RoundingMode.HALF_UP)
                                                   .multiply(new BigDecimal("100"));
        priority += savingsPercentage.intValue();
        
        // Vouchers expiring soon get higher priority
        long daysUntilExpiry = java.time.Duration.between(
            java.time.Instant.now(), 
            voucher.getNgayKetThuc()
        ).toDays();
        
        if (daysUntilExpiry <= 7) {
            priority += 20; // Expiring within a week
        } else if (daysUntilExpiry <= 30) {
            priority += 10; // Expiring within a month
        }
        
        // Limited usage vouchers get slight priority boost
        if (voucher.getRemainingUsage() <= 10) {
            priority += 5;
        }
        
        return priority;
    }
    
    /**
     * Generate user-friendly suggestion message
     */
    private String generateSuggestionMessage(PhieuGiamGia voucher, BigDecimal savingsAmount) {
        if (voucher.getLoaiGiamGia() == LoaiGiamGia.PHAN_TRAM) {
            return String.format("Tiết kiệm %,.0f ₫ với voucher giảm %s%%", 
                savingsAmount, voucher.getGiaTriGiam());
        } else {
            return String.format("Tiết kiệm %,.0f ₫ với voucher giảm %,.0f ₫", 
                savingsAmount, voucher.getGiaTriGiam());
        }
    }
}
