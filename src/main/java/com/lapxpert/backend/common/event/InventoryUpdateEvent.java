package com.lapxpert.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when inventory levels change for products.
 * Used for real-time notifications and cache invalidation.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateEvent {
    
    /**
     * Product variant ID
     */
    private Long variantId;
    
    /**
     * Product variant SKU for identification
     */
    private String sku;
    
    /**
     * Product name for display
     */
    private String tenSanPham;
    
    /**
     * Old inventory quantity
     */
    private Integer soLuongTonKhoCu;
    
    /**
     * New inventory quantity
     */
    private Integer soLuongTonKhoMoi;
    
    /**
     * Change type (INCREASE, DECREASE, ADJUSTMENT)
     */
    private String loaiThayDoi;
    
    /**
     * User who made the change
     */
    private String nguoiThucHien;
    
    /**
     * Reason for inventory change
     */
    private String lyDoThayDoi;
    
    /**
     * Timestamp when change occurred
     */
    private Instant timestamp;
    
    /**
     * Order ID if this change is related to an order
     */
    private Long hoaDonId;
    
    /**
     * Check if inventory decreased (potential out-of-stock situation)
     */
    public boolean isInventoryDecrease() {
        if (soLuongTonKhoCu == null || soLuongTonKhoMoi == null) return false;
        return soLuongTonKhoMoi < soLuongTonKhoCu;
    }
    
    /**
     * Check if inventory increased (restocking)
     */
    public boolean isInventoryIncrease() {
        if (soLuongTonKhoCu == null || soLuongTonKhoMoi == null) return false;
        return soLuongTonKhoMoi > soLuongTonKhoCu;
    }
    
    /**
     * Check if product is now out of stock
     */
    public boolean isOutOfStock() {
        return soLuongTonKhoMoi != null && soLuongTonKhoMoi <= 0;
    }
    
    /**
     * Check if product was previously out of stock but now has inventory
     */
    public boolean isBackInStock() {
        return (soLuongTonKhoCu == null || soLuongTonKhoCu <= 0) && 
               (soLuongTonKhoMoi != null && soLuongTonKhoMoi > 0);
    }
    
    /**
     * Get inventory change amount
     */
    public Integer getInventoryChange() {
        if (soLuongTonKhoCu == null || soLuongTonKhoMoi == null) return 0;
        return soLuongTonKhoMoi - soLuongTonKhoCu;
    }
}
