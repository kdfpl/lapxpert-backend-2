package com.lapxpert.backend.giohang.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for cart item availability checking
 * Used to validate cart items before checkout or order conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GioHangItemAvailabilityDto {
    
    private Long sanPhamChiTietId;
    
    /**
     * Product information for display
     */
    private String tenSanPham;
    private String serialNumber;
    private String mauSac;
    
    /**
     * Quantity information
     */
    private Integer soLuongYeuCau;
    private Integer soLuongCoSan;
    
    /**
     * Availability status
     */
    private boolean isAvailable;
    
    /**
     * Human-readable availability message
     */
    private String availabilityMessage;
    
    /**
     * Alternative product IDs if current item is unavailable
     */
    private List<Long> alternativeProductIds;
    
    /**
     * Availability status types
     */
    public enum AvailabilityStatus {
        AVAILABLE("Có sẵn"),
        OUT_OF_STOCK("Hết hàng"),
        INSUFFICIENT_QUANTITY("Không đủ số lượng"),
        DISCONTINUED("Ngừng kinh doanh"),
        TEMPORARILY_UNAVAILABLE("Tạm thời không có sẵn");
        
        private final String description;
        
        AvailabilityStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Check if requested quantity can be fulfilled
     * @return true if available quantity meets or exceeds requested quantity
     */
    public boolean canFulfillRequest() {
        return isAvailable && soLuongCoSan != null && soLuongYeuCau != null && 
               soLuongCoSan >= soLuongYeuCau;
    }
    
    /**
     * Get shortage amount if insufficient quantity
     * @return number of items short, or 0 if sufficient
     */
    public Integer getShortageAmount() {
        if (soLuongYeuCau == null || soLuongCoSan == null) {
            return 0;
        }
        return Math.max(0, soLuongYeuCau - soLuongCoSan);
    }
    
    /**
     * Check if item has alternatives available
     * @return true if alternative products are suggested
     */
    public boolean hasAlternatives() {
        return alternativeProductIds != null && !alternativeProductIds.isEmpty();
    }
}
