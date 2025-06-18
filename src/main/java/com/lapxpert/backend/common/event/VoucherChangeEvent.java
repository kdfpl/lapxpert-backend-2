package com.lapxpert.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


/**
 * Event published when voucher status or details change.
 * Used for real-time notifications and cache invalidation.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherChangeEvent {
    
    /**
     * Voucher ID (PhieuGiamGia or DotGiamGia)
     */
    private Long voucherId;
    
    /**
     * Voucher code for identification
     */
    private String maVoucher;
    
    /**
     * Voucher name for display
     */
    private String tenVoucher;
    
    /**
     * Voucher type (PHIEU_GIAM_GIA, DOT_GIAM_GIA)
     */
    private String loaiVoucher;
    
    /**
     * Old status before change
     */
    private String trangThaiCu;
    
    /**
     * New status after change
     */
    private String trangThaiMoi;
    
    /**
     * Old discount value
     */
    private BigDecimal giaTriGiamCu;
    
    /**
     * New discount value
     */
    private BigDecimal giaTriGiamMoi;
    
    /**
     * Old expiry date
     */
    private Instant ngayHetHanCu;

    /**
     * New expiry date
     */
    private Instant ngayHetHanMoi;
    
    /**
     * Change type (STATUS_CHANGE, VALUE_CHANGE, EXPIRY_CHANGE, CREATED, DELETED)
     */
    private String loaiThayDoi;
    
    /**
     * User who made the change
     */
    private String nguoiThucHien;
    
    /**
     * Reason for voucher change
     */
    private String lyDoThayDoi;
    
    /**
     * Timestamp when change occurred
     */
    private Instant timestamp;
    
    /**
     * Check if voucher status changed
     */
    public boolean hasStatusChanged() {
        if (trangThaiCu == null && trangThaiMoi == null) return false;
        if (trangThaiCu == null || trangThaiMoi == null) return true;
        return !trangThaiCu.equals(trangThaiMoi);
    }
    
    /**
     * Check if voucher discount value changed
     */
    public boolean hasDiscountValueChanged() {
        if (giaTriGiamCu == null && giaTriGiamMoi == null) return false;
        if (giaTriGiamCu == null || giaTriGiamMoi == null) return true;
        return giaTriGiamCu.compareTo(giaTriGiamMoi) != 0;
    }
    
    /**
     * Check if voucher expiry date changed
     */
    public boolean hasExpiryDateChanged() {
        if (ngayHetHanCu == null && ngayHetHanMoi == null) return false;
        if (ngayHetHanCu == null || ngayHetHanMoi == null) return true;
        return !ngayHetHanCu.equals(ngayHetHanMoi);
    }
    
    /**
     * Check if voucher became active
     */
    public boolean isVoucherActivated() {
        return hasStatusChanged() && 
               (trangThaiCu == null || !trangThaiCu.equals("HOAT_DONG")) &&
               "HOAT_DONG".equals(trangThaiMoi);
    }
    
    /**
     * Check if voucher became inactive/expired
     */
    public boolean isVoucherDeactivated() {
        return hasStatusChanged() && 
               "HOAT_DONG".equals(trangThaiCu) &&
               (trangThaiMoi == null || !trangThaiMoi.equals("HOAT_DONG"));
    }
    
    /**
     * Check if this is a new voucher creation
     */
    public boolean isNewVoucher() {
        return "CREATED".equals(loaiThayDoi);
    }
    
    /**
     * Check if voucher was deleted
     */
    public boolean isVoucherDeleted() {
        return "DELETED".equals(loaiThayDoi);
    }
}
