package com.lapxpert.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when order status or details change.
 * Used for real-time notifications and cache invalidation.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangeEvent {
    
    /**
     * Order ID
     */
    private Long hoaDonId;
    
    /**
     * Order code for identification
     */
    private String maHoaDon;
    
    /**
     * Customer ID
     */
    private Long khachHangId;
    
    /**
     * Customer name for display
     */
    private String tenKhachHang;
    
    /**
     * Old order status
     */
    private String trangThaiCu;
    
    /**
     * New order status
     */
    private String trangThaiMoi;
    
    /**
     * Old total amount
     */
    private BigDecimal tongTienCu;
    
    /**
     * New total amount
     */
    private BigDecimal tongTienMoi;
    
    /**
     * Change type (STATUS_CHANGE, AMOUNT_CHANGE, CREATED, CANCELLED, COMPLETED)
     */
    private String loaiThayDoi;
    
    /**
     * User who made the change
     */
    private String nguoiThucHien;
    
    /**
     * Reason for order change
     */
    private String lyDoThayDoi;
    
    /**
     * Timestamp when change occurred
     */
    private Instant timestamp;
    
    /**
     * Payment method if relevant
     */
    private String phuongThucThanhToan;
    
    /**
     * Check if order status changed
     */
    public boolean hasStatusChanged() {
        if (trangThaiCu == null && trangThaiMoi == null) return false;
        if (trangThaiCu == null || trangThaiMoi == null) return true;
        return !trangThaiCu.equals(trangThaiMoi);
    }
    
    /**
     * Check if order total amount changed
     */
    public boolean hasTotalAmountChanged() {
        if (tongTienCu == null && tongTienMoi == null) return false;
        if (tongTienCu == null || tongTienMoi == null) return true;
        return tongTienCu.compareTo(tongTienMoi) != 0;
    }
    
    /**
     * Check if order was completed
     */
    public boolean isOrderCompleted() {
        return hasStatusChanged() && "HOAN_THANH".equals(trangThaiMoi);
    }
    
    /**
     * Check if order was cancelled
     */
    public boolean isOrderCancelled() {
        return hasStatusChanged() && "BI_HUY".equals(trangThaiMoi);
    }
    
    /**
     * Check if this is a new order
     */
    public boolean isNewOrder() {
        return "CREATED".equals(loaiThayDoi);
    }
    
    /**
     * Check if order is pending payment
     */
    public boolean isPendingPayment() {
        return "CHO_THANH_TOAN".equals(trangThaiMoi);
    }
    
    /**
     * Check if order payment was confirmed
     */
    public boolean isPaymentConfirmed() {
        return hasStatusChanged() && 
               "CHO_THANH_TOAN".equals(trangThaiCu) && 
               "DA_THANH_TOAN".equals(trangThaiMoi);
    }
}
