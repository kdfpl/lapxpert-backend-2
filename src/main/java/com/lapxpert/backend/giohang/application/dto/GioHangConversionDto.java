package com.lapxpert.backend.giohang.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for cart-to-order conversion preparation
 * Contains all information needed to convert cart to order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GioHangConversionDto {
    
    /**
     * Original cart information
     */
    private GioHangDto gioHang;
    
    /**
     * Availability check results for all cart items
     */
    private List<GioHangItemAvailabilityDto> availabilityCheck;
    
    /**
     * Pricing breakdown
     */
    private BigDecimal tongTienTruocGiam;
    private BigDecimal tongTienGiamGia;
    private BigDecimal tongTienSauGiam;
    private BigDecimal tongTienVanChuyen;
    private BigDecimal tongTienThue;
    private BigDecimal tongTienThanhToan;
    
    /**
     * Applied discounts and vouchers
     */
    private List<AppliedDiscountDto> appliedDiscounts;
    
    /**
     * Delivery information
     */
    private Long diaChiGiaoHangId;
    private String diaChiGiaoHang;
    private BigDecimal phiVanChuyen;
    private String phuongThucVanChuyen;
    
    /**
     * Warnings and notifications
     */
    private List<String> warnings;
    private List<String> priceChangeNotifications;
    private List<String> availabilityWarnings;
    
    /**
     * Conversion status
     */
    private boolean canProceedToOrder;
    private String conversionStatus;
    
    /**
     * Check if all items are available
     * @return true if all cart items can be fulfilled
     */
    public boolean areAllItemsAvailable() {
        if (availabilityCheck == null) {
            return false;
        }
        return availabilityCheck.stream()
            .allMatch(GioHangItemAvailabilityDto::canFulfillRequest);
    }
    
    /**
     * Get total number of unavailable items
     * @return count of items that cannot be fulfilled
     */
    public long getUnavailableItemCount() {
        if (availabilityCheck == null) {
            return 0;
        }
        return availabilityCheck.stream()
            .filter(item -> !item.canFulfillRequest())
            .count();
    }
    
    /**
     * Check if cart has any warnings
     * @return true if there are warnings to display
     */
    public boolean hasWarnings() {
        return (warnings != null && !warnings.isEmpty()) ||
               (priceChangeNotifications != null && !priceChangeNotifications.isEmpty()) ||
               (availabilityWarnings != null && !availabilityWarnings.isEmpty());
    }
    
    /**
     * Get total discount amount
     * @return sum of all applied discounts
     */
    public BigDecimal getTotalDiscountAmount() {
        if (tongTienTruocGiam == null || tongTienSauGiam == null) {
            return BigDecimal.ZERO;
        }
        return tongTienTruocGiam.subtract(tongTienSauGiam);
    }
}
