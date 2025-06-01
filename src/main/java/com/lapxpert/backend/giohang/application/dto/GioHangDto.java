package com.lapxpert.backend.giohang.application.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for GioHang entity representing user's shopping cart
 * Follows established DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GioHangDto {
    
    private Long id;
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    private String tenNguoiDung;
    
    @Builder.Default
    private List<GioHangChiTietDto> chiTiets = new ArrayList<>();
    
    /**
     * Total amount for all items in cart
     */
    private BigDecimal tongTien;
    
    /**
     * Total quantity of all items in cart
     */
    private Integer tongSoLuong;
    
    /**
     * Number of unique items in cart
     */
    private Integer soLuongSanPhamKhacNhau;
    
    /**
     * Standard audit fields for online modules
     */
    private Instant ngayTao;
    private Instant ngayCapNhat;
    
    // Business logic fields
    /**
     * Indicates if cart has items that have expired
     */
    private boolean hasExpiredItems;
    
    /**
     * Indicates if cart has items with price changes
     */
    private boolean hasPriceChanges;
    
    /**
     * Indicates if cart has items that are no longer available
     */
    private boolean hasUnavailableItems;
    
    /**
     * Check if cart is empty
     * @return true if cart has no items
     */
    public boolean isEmpty() {
        return chiTiets == null || chiTiets.isEmpty();
    }
    
    /**
     * Get total number of items (sum of quantities)
     * @return total item count
     */
    public Integer getTotalItemCount() {
        if (chiTiets == null) {
            return 0;
        }
        return chiTiets.stream()
            .mapToInt(item -> item.getSoLuong() != null ? item.getSoLuong() : 0)
            .sum();
    }
    
    /**
     * Get number of unique products
     * @return number of different products
     */
    public Integer getUniqueProductCount() {
        return chiTiets != null ? chiTiets.size() : 0;
    }
    
    /**
     * Calculate total cart value
     * @return total amount
     */
    public BigDecimal calculateTotalAmount() {
        if (chiTiets == null) {
            return BigDecimal.ZERO;
        }
        return chiTiets.stream()
            .map(item -> item.getThanhTien() != null ? item.getThanhTien() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
