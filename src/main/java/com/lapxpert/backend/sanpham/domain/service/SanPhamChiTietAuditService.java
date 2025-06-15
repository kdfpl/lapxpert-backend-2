package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.domain.entity.SanPhamChiTietAuditHistory;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.event.PriceChangeEvent;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietAuditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Audit service for SanPhamChiTiet extending existing audit patterns.
 * Follows the same structure as HoaDonAuditService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SanPhamChiTietAuditService {

    private final SanPhamChiTietAuditHistoryRepository auditHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Log price change with real-time notification
     */
    @Transactional
    public void logPriceChange(Long variantId, BigDecimal oldPrice, BigDecimal newPrice,
                             BigDecimal oldPromotionalPrice, BigDecimal newPromotionalPrice,
                             String nguoiThucHien, String lyDo, SanPhamChiTiet variant) {
        try {
            // Create and publish price change event for real-time notifications
            PriceChangeEvent event = PriceChangeEvent.builder()
                    .variantId(variantId)
                    .sku(variant.getSku())
                    .productName(variant.getSanPham() != null ? variant.getSanPham().getTenSanPham() : "Unknown Product")
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .oldPromotionalPrice(oldPromotionalPrice)
                    .newPromotionalPrice(newPromotionalPrice)
                    .nguoiThucHien(nguoiThucHien)
                    .lyDoThayDoi(lyDo)
                    .timestamp(Instant.now())
                    .build();

            // Publish event for real-time processing
            eventPublisher.publishEvent(event);

            log.info("Published price change event for variant {}: {} -> {}", 
                variantId, event.getEffectiveOldPrice(), event.getEffectiveNewPrice());

        } catch (Exception e) {
            log.error("Failed to log price change for variant {}: {}", variantId, e.getMessage(), e);
        }
    }

    /**
     * Log status change
     */
    @Transactional
    public void logStatusChange(Long variantId, Boolean oldStatus, Boolean newStatus,
                              String nguoiThucHien, String lyDo) {
        try {
            SanPhamChiTietAuditHistory auditEntry = SanPhamChiTietAuditHistory.statusChangeEntry(
                variantId, oldStatus.toString(), newStatus.toString(), nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);

            log.info("Logged status change for variant {}: {} -> {} by {}", 
                variantId, oldStatus, newStatus, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log status change for variant {}: {}", variantId, e.getMessage(), e);
        }
    }

    /**
     * Log variant creation
     */
    @Transactional
    public void logVariantCreation(SanPhamChiTiet variant, String nguoiThucHien, String lyDo) {
        try {
            String newValues = createDetailedAuditValues(variant);
            SanPhamChiTietAuditHistory auditEntry = SanPhamChiTietAuditHistory.createEntry(
                variant.getId(), newValues, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);

            log.info("Logged variant creation for variant {} by {}", variant.getId(), nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log variant creation for variant {}: {}", variant.getId(), e.getMessage(), e);
        }
    }

    /**
     * Log variant update
     */
    @Transactional
    public void logVariantUpdate(Long variantId, String oldValues, String newValues,
                               String nguoiThucHien, String lyDo) {
        try {
            SanPhamChiTietAuditHistory auditEntry = SanPhamChiTietAuditHistory.updateEntry(
                variantId, oldValues, newValues, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);

            log.info("Logged variant update for variant {} by {}", variantId, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log variant update for variant {}: {}", variantId, e.getMessage(), e);
        }
    }

    /**
     * Get comprehensive audit history for a variant
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTietAuditHistory> getComprehensiveAuditHistory(Long variantId) {
        return auditHistoryRepository.findBySanPhamChiTietIdOrderByThoiGianThayDoiDesc(variantId);
    }

    /**
     * Get audit history by action type
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTietAuditHistory> getAuditHistoryByAction(Long variantId, String action) {
        return auditHistoryRepository.findBySanPhamChiTietIdAndHanhDongOrderByThoiGianThayDoiDesc(variantId, action);
    }

    /**
     * Get price change history for a variant
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTietAuditHistory> getPriceChangeHistory(Long variantId) {
        return auditHistoryRepository.findBySanPhamChiTietIdAndHanhDongInOrderByThoiGianThayDoiDesc(
            variantId, List.of("PRICE_CHANGE", "PROMOTIONAL_PRICE_CHANGE"));
    }

    /**
     * Create detailed audit values JSON for variant
     */
    private String createDetailedAuditValues(SanPhamChiTiet variant) {
        return String.format(
            "{\"sku\":\"%s\",\"giaBan\":\"%s\",\"giaKhuyenMai\":\"%s\",\"trangThai\":\"%s\"}",
            variant.getSku(),
            variant.getGiaBan(),
            variant.getGiaKhuyenMai(),
            variant.getTrangThai()
        );
    }
}
