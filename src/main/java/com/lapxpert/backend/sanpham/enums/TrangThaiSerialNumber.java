package com.lapxpert.backend.sanpham.enums;

/**
 * Enum representing the status of individual serial numbers in the inventory system.
 * Each serial number represents exactly one physical laptop unit.
 * This provides granular tracking of individual laptop lifecycle.
 */
public enum TrangThaiSerialNumber {
    /**
     * Serial number is available for sale
     */
    AVAILABLE("Có sẵn"),
    
    /**
     * Serial number is reserved for a pending order (not yet paid)
     */
    RESERVED("Đã đặt trước"),
    
    /**
     * Serial number has been sold and payment confirmed
     */
    SOLD("Đã bán"),
    
    /**
     * Serial number has been returned by customer
     */
    RETURNED("Đã trả lại"),
    
    /**
     * Serial number is damaged or defective
     */
    DAMAGED("Hỏng hóc"),
    
    /**
     * Serial number is temporarily unavailable (maintenance, etc.)
     */
    UNAVAILABLE("Không khả dụng"),
    
    /**
     * Serial number is in transit (being shipped to store)
     */
    IN_TRANSIT("Đang vận chuyển"),
    
    /**
     * Serial number is in quality control/testing
     */
    QUALITY_CONTROL("Kiểm tra chất lượng"),
    
    /**
     * Serial number is allocated for display/demo purposes
     */
    DISPLAY_UNIT("Máy trưng bày"),
    
    /**
     * Serial number has been disposed/written off
     */
    DISPOSED("Đã thanh lý");
    
    private final String description;
    
    TrangThaiSerialNumber(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the serial number is available for sale
     */
    public boolean isAvailableForSale() {
        return this == AVAILABLE;
    }
    
    /**
     * Check if the serial number is in a final state (sold, returned, damaged, disposed)
     */
    public boolean isFinalState() {
        return this == SOLD || this == RETURNED || this == DAMAGED || this == DISPOSED;
    }
    
    /**
     * Check if the serial number can be reserved
     */
    public boolean canBeReserved() {
        return this == AVAILABLE;
    }
    
    /**
     * Check if the serial number can be sold
     */
    public boolean canBeSold() {
        return this == AVAILABLE || this == RESERVED;
    }
    
    /**
     * Check if the serial number can be returned
     */
    public boolean canBeReturned() {
        return this == SOLD;
    }
    
    /**
     * Check if the serial number is in inventory (countable)
     */
    public boolean isInInventory() {
        return this == AVAILABLE || this == RESERVED || this == DISPLAY_UNIT || this == QUALITY_CONTROL;
    }
    
    /**
     * Check if the serial number requires physical location tracking
     */
    public boolean requiresLocationTracking() {
        return this == AVAILABLE || this == RESERVED || this == DISPLAY_UNIT || 
               this == QUALITY_CONTROL || this == IN_TRANSIT;
    }
    
    /**
     * Get status category for reporting
     */
    public String getCategory() {
        switch (this) {
            case AVAILABLE:
            case RESERVED:
                return "SELLABLE";
            case SOLD:
                return "SOLD";
            case RETURNED:
                return "RETURNED";
            case DAMAGED:
            case DISPOSED:
                return "UNUSABLE";
            case IN_TRANSIT:
            case QUALITY_CONTROL:
                return "PROCESSING";
            case DISPLAY_UNIT:
                return "DISPLAY";
            case UNAVAILABLE:
                return "UNAVAILABLE";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Get severity level for UI display
     */
    public String getSeverity() {
        switch (this) {
            case AVAILABLE:
                return "success";
            case RESERVED:
                return "info";
            case SOLD:
                return "success";
            case RETURNED:
                return "warning";
            case DAMAGED:
            case DISPOSED:
                return "danger";
            case IN_TRANSIT:
            case QUALITY_CONTROL:
                return "info";
            case DISPLAY_UNIT:
                return "secondary";
            case UNAVAILABLE:
                return "warning";
            default:
                return "secondary";
        }
    }
    

    
    /**
     * Get all statuses that are considered "active inventory"
     */
    public static TrangThaiSerialNumber[] getActiveInventoryStatuses() {
        return new TrangThaiSerialNumber[]{
            AVAILABLE, RESERVED, DISPLAY_UNIT, QUALITY_CONTROL
        };
    }
    
    /**
     * Get all statuses that are considered "sellable"
     */
    public static TrangThaiSerialNumber[] getSellableStatuses() {
        return new TrangThaiSerialNumber[]{
            AVAILABLE, RESERVED
        };
    }
    
    /**
     * Get all statuses that require attention/action
     */
    public static TrangThaiSerialNumber[] getActionRequiredStatuses() {
        return new TrangThaiSerialNumber[]{
            DAMAGED, RETURNED, QUALITY_CONTROL, IN_TRANSIT
        };
    }
}
