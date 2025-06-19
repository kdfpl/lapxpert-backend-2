package com.lapxpert.backend.phieugiamgia.service;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.service.VietnamTimeZoneService;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.service.NguoiDungService;
import com.lapxpert.backend.phieugiamgia.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.dto.IntelligentRecommendationResult;
import com.lapxpert.backend.phieugiamgia.dto.VoucherRecommendation;
import com.lapxpert.backend.phieugiamgia.dto.VoucherSuggestionDto;
import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.repository.PhieuGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced voucher monitoring service with real-time validation and notifications.
 * Extends existing PhieuGiamGiaService patterns with WebSocket integration.
 * 
 * Features:
 * - Real-time voucher expiration monitoring (every 10 minutes)
 * - WebSocket notifications for expired/new vouchers
 * - Alternative voucher recommendations
 * - Integration with existing intelligent recommendation system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherMonitoringService {
    
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaService phieuGiamGiaService;
    private final VietnamTimeZoneService vietnamTimeZoneService;
    // private final SimpMessagingTemplate messagingTemplate;
    private final NguoiDungService nguoiDungService;
    private final VoucherSuggestionEngine voucherSuggestionEngine;
    
    // Track last monitoring state to detect changes
    private final Map<Long, TrangThaiCampaign> lastKnownVoucherStates = new ConcurrentHashMap<>();
    
    /**
     * Scheduled voucher monitoring job - runs every 10 minutes
     * Monitors voucher status changes and sends real-time notifications
     */
    @Scheduled(cron = "0 */10 * * * ?") // Every 10 minutes
    @Transactional(readOnly = true)
    public void monitorVoucherChanges() {
        try {
            log.debug("Starting voucher monitoring cycle");
            
            // Use Vietnam timezone for business logic consistency
            Instant currentTime = vietnamTimeZoneService.getCurrentVietnamTime().toInstant();
            
            // Step 1: Find and notify expired vouchers
            List<PhieuGiamGia> recentlyExpiredVouchers = findRecentlyExpiredVouchers(currentTime);
            recentlyExpiredVouchers.forEach(this::notifyVoucherExpiration);
            
            // Step 2: Find and notify newly activated vouchers
            List<PhieuGiamGia> newlyActivatedVouchers = findNewlyActivatedVouchers(currentTime);
            newlyActivatedVouchers.forEach(this::notifyNewVoucher);
            
            // Step 3: Update tracking state for next cycle
            updateVoucherStateTracking();

            // Step 4: Check for better voucher alternatives (new feature)
            checkForBetterVoucherAlternatives();

            log.info("Voucher monitoring completed - Expired: {}, New: {}",
                    recentlyExpiredVouchers.size(), newlyActivatedVouchers.size());
                    
        } catch (Exception e) {
            log.error("Error during voucher monitoring cycle", e);
        }
    }
    
    /**
     * Find vouchers that have recently expired (changed from DA_DIEN_RA to KET_THUC)
     */
    private List<PhieuGiamGia> findRecentlyExpiredVouchers(Instant currentTime) {
        // Find vouchers that should be expired but still marked as active
        List<PhieuGiamGia> expiredVouchers = phieuGiamGiaRepository.findExpiredCampaigns(
            currentTime, TrangThaiCampaign.KET_THUC);
        
        // Filter to only include vouchers that were previously active
        return expiredVouchers.stream()
            .filter(voucher -> {
                TrangThaiCampaign lastState = lastKnownVoucherStates.get(voucher.getId());
                return lastState == TrangThaiCampaign.DA_DIEN_RA;
            })
            .toList();
    }
    
    /**
     * Find vouchers that have recently been activated (changed from CHUA_DIEN_RA to DA_DIEN_RA)
     */
    private List<PhieuGiamGia> findNewlyActivatedVouchers(Instant currentTime) {
        // Find vouchers that should be active now
        List<PhieuGiamGia> activeVouchers = phieuGiamGiaRepository.findByTrangThai(TrangThaiCampaign.DA_DIEN_RA);
        
        // Filter to only include vouchers that were previously not started
        return activeVouchers.stream()
            .filter(voucher -> {
                TrangThaiCampaign lastState = lastKnownVoucherStates.get(voucher.getId());
                return lastState == TrangThaiCampaign.CHUA_DIEN_RA || lastState == null;
            })
            .toList();
    }
    
    /**
     * Update internal tracking state for all vouchers
     */
    private void updateVoucherStateTracking() {
        List<PhieuGiamGia> allVouchers = phieuGiamGiaRepository.findAll();
        lastKnownVoucherStates.clear();
        
        for (PhieuGiamGia voucher : allVouchers) {
            lastKnownVoucherStates.put(voucher.getId(), voucher.getTrangThai());
        }
        
        log.debug("Updated voucher state tracking for {} vouchers", allVouchers.size());
    }
    
    /**
     * Send WebSocket notification for expired voucher
     */
    private void notifyVoucherExpiration(PhieuGiamGia voucher) {
        try {
            VoucherExpirationNotification notification = VoucherExpirationNotification.builder()
                .voucherId(voucher.getId())
                .voucherCode(voucher.getMaPhieuGiamGia())
                .voucherDescription(voucher.getMoTa())
                .discountValue(voucher.getGiaTriGiam())
                .discountType(voucher.getLoaiGiamGia().name())
                .expirationTime(formatVietnamTime(voucher.getNgayKetThuc()))
                .message("Phiếu giảm giá '" + voucher.getMaPhieuGiamGia() + "' đã hết hạn")
                .timestamp(getCurrentVietnamTimeString())
                .build();
            
            // Broadcast to all connected clients (WebSocket functionality to be enabled later)
            // messagingTemplate.convertAndSend("/topic/phieu-giam-gia/expired", notification);

            // Log the notification for now (will be replaced with WebSocket broadcast)
            log.warn("🚨 VOUCHER EXPIRED: {} - {}", voucher.getMaPhieuGiamGia(), notification.getMessage());

            // Also send alternative recommendations
            sendAlternativeVoucherRecommendations(voucher);

            log.info("Sent expiration notification for voucher: {}", voucher.getMaPhieuGiamGia());
            
        } catch (Exception e) {
            log.error("Failed to send expiration notification for voucher: {}", voucher.getMaPhieuGiamGia(), e);
        }
    }
    
    /**
     * Send WebSocket notification for newly activated voucher
     */
    private void notifyNewVoucher(PhieuGiamGia voucher) {
        try {
            NewVoucherNotification notification = NewVoucherNotification.builder()
                .voucherId(voucher.getId())
                .voucherCode(voucher.getMaPhieuGiamGia())
                .voucherDescription(voucher.getMoTa())
                .discountValue(voucher.getGiaTriGiam())
                .discountType(voucher.getLoaiGiamGia().name())
                .minimumOrderValue(voucher.getGiaTriDonHangToiThieu())
                .remainingQuantity(voucher.getSoLuongBanDau() - voucher.getSoLuongDaDung())
                .expirationTime(formatVietnamTime(voucher.getNgayKetThuc()))
                .message("Phiếu giảm giá mới '" + voucher.getMaPhieuGiamGia() + "' đã có hiệu lực")
                .timestamp(getCurrentVietnamTimeString())
                .build();
            
            // Broadcast to all connected clients (WebSocket functionality to be enabled later)
            // messagingTemplate.convertAndSend("/topic/phieu-giam-gia/new", notification);

            // Log the notification for now (will be replaced with WebSocket broadcast)
            log.info("🎉 NEW VOUCHER ACTIVATED: {} - {}", voucher.getMaPhieuGiamGia(), notification.getMessage());

            log.info("Sent new voucher notification for: {}", voucher.getMaPhieuGiamGia());
            
        } catch (Exception e) {
            log.error("Failed to send new voucher notification for: {}", voucher.getMaPhieuGiamGia(), e);
        }
    }
    
    /**
     * Send alternative voucher recommendations when a voucher expires
     */
    private void sendAlternativeVoucherRecommendations(PhieuGiamGia expiredVoucher) {
        try {
            // Use existing intelligent recommendation system to find alternatives
            // We'll use a sample order total based on the expired voucher's minimum requirement
            BigDecimal sampleOrderTotal = expiredVoucher.getGiaTriDonHangToiThieu() != null 
                ? expiredVoucher.getGiaTriDonHangToiThieu() 
                : BigDecimal.valueOf(1000000); // Default 1M VND
            
            IntelligentRecommendationResult recommendations =
                phieuGiamGiaService.getIntelligentVoucherRecommendations(null, sampleOrderTotal, null);
            
            if (recommendations.isHasRecommendations()) {
                AlternativeVoucherRecommendation alternativeRecommendation = AlternativeVoucherRecommendation.builder()
                    .expiredVoucherId(expiredVoucher.getId())
                    .expiredVoucherCode(expiredVoucher.getMaPhieuGiamGia())
                    .primaryAlternative(mapToAlternativeVoucher(recommendations.getPrimaryRecommendation()))
                    .additionalAlternatives(recommendations.getAlternativeRecommendations().stream()
                        .map(this::mapToAlternativeVoucher)
                        .toList())
                    .message("Tìm thấy " + (1 + recommendations.getAlternativeRecommendations().size()) + 
                            " phiếu giảm giá thay thế cho '" + expiredVoucher.getMaPhieuGiamGia() + "'")
                    .timestamp(getCurrentVietnamTimeString())
                    .build();
                
                // messagingTemplate.convertAndSend("/topic/phieu-giam-gia/alternatives", alternativeRecommendation);

                // Log the alternative recommendations for now (will be replaced with WebSocket broadcast)
                log.info("💡 ALTERNATIVE VOUCHERS for {}: Primary: {}, Additional: {}",
                    expiredVoucher.getMaPhieuGiamGia(),
                    alternativeRecommendation.getPrimaryAlternative() != null ?
                        alternativeRecommendation.getPrimaryAlternative().getVoucherCode() : "None",
                    alternativeRecommendation.getAdditionalAlternatives().size());

                log.info("Sent alternative recommendations for expired voucher: {}", expiredVoucher.getMaPhieuGiamGia());
            }
            
        } catch (Exception e) {
            log.error("Failed to send alternative recommendations for voucher: {}", expiredVoucher.getMaPhieuGiamGia(), e);
        }
    }

    /**
     * Check for better voucher alternatives for active orders
     * This method runs as part of the scheduled monitoring cycle
     */
    private void checkForBetterVoucherAlternatives() {
        try {
            log.debug("Checking for better voucher alternatives");

            // In a real implementation, this would check active orders/carts
            // For now, we'll implement a general better voucher detection
            // that can be called by the order management system

            // This is a placeholder for the scheduled check
            // The actual detection will be triggered by order events

            log.debug("Better voucher alternatives check completed");

        } catch (Exception e) {
            log.error("Error during better voucher alternatives check", e);
        }
    }

    /**
     * Detect better vouchers for a specific order context
     * Called by order management system when vouchers are applied
     */
    @Transactional(readOnly = true)
    public List<VoucherSuggestionDto> detectBetterVouchers(
            List<String> currentVoucherCodes,
            Long customerId,
            BigDecimal orderTotal) {

        try {
            log.debug("Detecting better vouchers for customer {} with order total {}", customerId, orderTotal);

            List<VoucherSuggestionDto> suggestions = voucherSuggestionEngine.detectBetterVouchers(
                currentVoucherCodes, customerId, orderTotal);

            if (!suggestions.isEmpty()) {
                log.info("Found {} better voucher suggestions for customer {}", suggestions.size(), customerId);

                // Send suggestions via WebSocket (to be enabled later)
                sendVoucherSuggestions(suggestions, customerId);
            }

            return suggestions;

        } catch (Exception e) {
            log.error("Error detecting better vouchers for customer {}", customerId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Calculate voucher value for comparison
     */
    public BigDecimal calculateVoucherValue(String voucherCode, BigDecimal orderTotal) {
        try {
            Optional<PhieuGiamGia> voucherOpt = phieuGiamGiaRepository.findByMaPhieuGiamGia(voucherCode);
            if (voucherOpt.isEmpty()) {
                return BigDecimal.ZERO;
            }

            return voucherSuggestionEngine.calculateVoucherValue(voucherOpt.get(), orderTotal);

        } catch (Exception e) {
            log.error("Error calculating voucher value for code: {}", voucherCode, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Send voucher suggestions via WebSocket
     */
    private void sendVoucherSuggestions(List<VoucherSuggestionDto> suggestions, Long customerId) {
        try {
            // Add timestamp to suggestions
            Instant now = Instant.now();
            suggestions.forEach(suggestion -> suggestion.setTimestamp(now));

            VoucherSuggestionsNotification notification = VoucherSuggestionsNotification.builder()
                .customerId(customerId)
                .suggestions(suggestions)
                .totalSuggestions(suggestions.size())
                .maxSavings(suggestions.stream()
                    .map(VoucherSuggestionDto::getSavingsAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO))
                .message("Tìm thấy voucher tốt hơn cho đơn hàng của bạn")
                .timestamp(getCurrentVietnamTimeString())
                .build();

            // Broadcast to voucher suggestions topic (WebSocket functionality to be enabled later)
            // messagingTemplate.convertAndSend("/topic/phieu-giam-gia/suggestions", notification);

            // Log the suggestions for now
            log.info("🎯 VOUCHER SUGGESTIONS: Found {} better alternatives for customer {}",
                suggestions.size(), customerId);

            for (VoucherSuggestionDto suggestion : suggestions) {
                log.info("  - {}: {} (Tiết kiệm: {})",
                    suggestion.getVoucherCode(),
                    suggestion.getMessage(),
                    formatCurrency(suggestion.getSavingsAmount()));
            }

        } catch (Exception e) {
            log.error("Failed to send voucher suggestions for customer: {}", customerId, e);
        }
    }
    
    /**
     * Map VoucherRecommendation to AlternativeVoucherInfo
     */
    private AlternativeVoucherInfo mapToAlternativeVoucher(VoucherRecommendation recommendation) {
        if (recommendation == null) return null;
        
        PhieuGiamGiaDto voucher = recommendation.getVoucher();
        return AlternativeVoucherInfo.builder()
            .voucherId(voucher.getId())
            .voucherCode(voucher.getMaPhieuGiamGia())
            .voucherDescription(voucher.getMoTa())
            .discountValue(voucher.getGiaTriGiam())
            .discountType(voucher.getLoaiGiamGia().name())
            .minimumOrderValue(voucher.getGiaTriDonHangToiThieu())
            .remainingQuantity(voucher.getSoLuongBanDau() - voucher.getSoLuongDaDung())
            .expirationTime(formatVietnamTime(voucher.getNgayKetThuc()))
            .recommendationReason(recommendation.getRecommendationReason())
            .effectivenessScore(recommendation.getEffectivenessScore())
            .build();
    }
    
    /**
     * Format Instant to Vietnam timezone string
     */
    private String formatVietnamTime(Instant instant) {
        return instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    
    /**
     * Get current Vietnam time as formatted string
     */
    private String getCurrentVietnamTimeString() {
        return vietnamTimeZoneService.getCurrentVietnamTime()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /**
     * Real-time voucher validation for active orders
     * Can be called by order processing to validate vouchers in real-time
     */
    @Transactional(readOnly = true)
    public VoucherValidationNotification validateVoucherRealTime(String voucherCode, Long customerId, BigDecimal orderTotal) {
        try {
            // Use existing validation logic from PhieuGiamGiaService
            NguoiDung customer = null;
            if (customerId != null) {
                customer = nguoiDungService.findByIdOptional(customerId).orElse(null);
            }

            PhieuGiamGiaService.VoucherValidationResult validationResult =
                phieuGiamGiaService.validateVoucher(voucherCode, customer, orderTotal);

            VoucherValidationNotification notification = VoucherValidationNotification.builder()
                .voucherCode(voucherCode)
                .customerId(customerId)
                .orderTotal(orderTotal)
                .isValid(validationResult.isValid())
                .discountAmount(validationResult.getDiscountAmount())
                .errorMessage(validationResult.getErrorMessage())
                .timestamp(getCurrentVietnamTimeString())
                .build();

            // Send real-time validation result via WebSocket (to be enabled later)
            // if (customerId != null) {
            //     messagingTemplate.convertAndSendToUser(
            //         customerId.toString(),
            //         "/queue/voucher-validation",
            //         notification
            //     );
            // }

            return notification;

        } catch (Exception e) {
            log.error("Error during real-time voucher validation for code: {}", voucherCode, e);
            return VoucherValidationNotification.builder()
                .voucherCode(voucherCode)
                .customerId(customerId)
                .orderTotal(orderTotal)
                .isValid(false)
                .errorMessage("Lỗi hệ thống khi kiểm tra phiếu giảm giá")
                .timestamp(getCurrentVietnamTimeString())
                .build();
        }
    }

    // ==================== WebSocket Message DTOs ====================

    /**
     * DTO for voucher expiration notifications
     */
    @lombok.Builder
    @lombok.Data
    public static class VoucherExpirationNotification {
        private Long voucherId;
        private String voucherCode;
        private String voucherDescription;
        private BigDecimal discountValue;
        private String discountType;
        private String expirationTime;
        private String message;
        private String timestamp;
    }

    /**
     * DTO for new voucher notifications
     */
    @lombok.Builder
    @lombok.Data
    public static class NewVoucherNotification {
        private Long voucherId;
        private String voucherCode;
        private String voucherDescription;
        private BigDecimal discountValue;
        private String discountType;
        private BigDecimal minimumOrderValue;
        private Integer remainingQuantity;
        private String expirationTime;
        private String message;
        private String timestamp;
    }

    /**
     * DTO for alternative voucher recommendations
     */
    @lombok.Builder
    @lombok.Data
    public static class AlternativeVoucherRecommendation {
        private Long expiredVoucherId;
        private String expiredVoucherCode;
        private AlternativeVoucherInfo primaryAlternative;
        private List<AlternativeVoucherInfo> additionalAlternatives;
        private String message;
        private String timestamp;
    }

    /**
     * DTO for individual alternative voucher information
     */
    @lombok.Builder
    @lombok.Data
    public static class AlternativeVoucherInfo {
        private Long voucherId;
        private String voucherCode;
        private String voucherDescription;
        private BigDecimal discountValue;
        private String discountType;
        private BigDecimal minimumOrderValue;
        private Integer remainingQuantity;
        private String expirationTime;
        private String recommendationReason;
        private Double effectivenessScore;
    }

    /**
     * DTO for real-time voucher validation notifications
     */
    @lombok.Builder
    @lombok.Data
    public static class VoucherValidationNotification {
        private String voucherCode;
        private Long customerId;
        private BigDecimal orderTotal;
        private Boolean isValid;
        private BigDecimal discountAmount;
        private String errorMessage;
        private String timestamp;
    }

    /**
     * DTO for voucher suggestions notifications
     */
    @lombok.Builder
    @lombok.Data
    public static class VoucherSuggestionsNotification {
        private Long customerId;
        private List<VoucherSuggestionDto> suggestions;
        private Integer totalSuggestions;
        private BigDecimal maxSavings;
        private String message;
        private String timestamp;
    }

    /**
     * Format currency for display
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,.0f ₫", amount);
    }
}
