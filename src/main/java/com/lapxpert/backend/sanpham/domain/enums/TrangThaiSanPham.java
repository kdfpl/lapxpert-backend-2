package com.lapxpert.backend.sanpham.domain.enums;

/**
 * Enum representing the status of individual product items in the inventory system.
 * This replaces the simple Boolean trangThai field with more granular status tracking.
 */
public enum TrangThaiSanPham {
    /**
     * Item is available for sale
     */
    AVAILABLE("Có sẵn"),
    
    /**
     * Item is reserved for a pending order (not yet paid)
     */
    RESERVED("Đã đặt trước"),
    
    /**
     * Item has been sold and payment confirmed
     */
    SOLD("Đã bán"),
    
    /**
     * Item has been returned by customer
     */
    RETURNED("Đã trả lại"),
    
    /**
     * Item is damaged or defective
     */
    DAMAGED("Hỏng hóc"),
    
    /**
     * Item is temporarily unavailable (maintenance, etc.)
     */
    UNAVAILABLE("Không khả dụng");
    
    private final String description;
    
    TrangThaiSanPham(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the item is available for sale (not reserved, sold, or damaged)
     */
    public boolean isAvailableForSale() {
        return this == AVAILABLE;
    }
    
    /**
     * Check if the item is in a final state (sold, returned, damaged)
     */
    public boolean isFinalState() {
        return this == SOLD || this == RETURNED || this == DAMAGED;
    }
    
    /**
     * Convert from legacy Boolean trangThai to new enum
     * @param trangThai true = AVAILABLE, false = SOLD
     * @return corresponding enum value
     */
    public static TrangThaiSanPham fromBoolean(Boolean trangThai) {
        return (trangThai != null && trangThai) ? AVAILABLE : SOLD;
    }
    
    /**
     * Convert to legacy Boolean for backward compatibility
     * @return true if AVAILABLE, false otherwise
     */
    public Boolean toBoolean() {
        return this == AVAILABLE;
    }
}
