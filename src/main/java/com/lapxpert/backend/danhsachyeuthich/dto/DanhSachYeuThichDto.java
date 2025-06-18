package com.lapxpert.backend.danhsachyeuthich.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for DanhSachYeuThich entity representing user's wishlist items
 * Follows established DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanhSachYeuThichDto {

    private Long id;

    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long sanPhamId;

    /**
     * Product information for display
     */
    private String tenSanPham;
    private String maSanPham;
    private String moTa;
    private String hinhAnh;

    /**
     * Price information
     */
    private BigDecimal giaThapNhat;
    private BigDecimal giaCaoNhat;
    private BigDecimal giaKhiThem;  // Price when added to wishlist (for tracking)
    private BigDecimal giaHienTai;  // Current minimum price

    /**
     * Standard audit fields for online modules
     */
    private Instant ngayThem;

    // Business logic fields
    /**
     * Indicates if the product is still available for purchase
     */
    private boolean isAvailable;

    /**
     * Indicates if the price has dropped since adding to wishlist
     */
    private boolean hasPriceDropped;

    /**
     * Price drop percentage if applicable
     */
    private BigDecimal priceDropPercentage;

    /**
     * Number of available variants for this product
     */
    private long availableVariantCount;

    /**
     * Indicates if the product has active discount campaigns
     */
    private boolean hasActiveDiscount;

    /**
     * Human-readable availability status
     */
    private String availabilityStatus;

    /**
     * Available product variants for selection when moving to cart
     */
    private List<SanPhamChiTietSummaryDto> availableVariants;

    /**
     * Check if price has dropped significantly
     * @param threshold minimum percentage drop to consider significant
     * @return true if price drop exceeds threshold
     */
    public boolean hasSignificantPriceDrop(BigDecimal threshold) {
        return priceDropPercentage != null &&
               priceDropPercentage.compareTo(threshold) >= 0;
    }

    /**
     * Calculate price drop amount
     * @return amount of price drop or zero if no drop
     */
    public BigDecimal getPriceDropAmount() {
        if (giaKhiThem == null || giaHienTai == null ||
            giaHienTai.compareTo(giaKhiThem) >= 0) {
            return BigDecimal.ZERO;
        }
        return giaKhiThem.subtract(giaHienTai);
    }

    /**
     * Check if product is in stock
     * @return true if product has available variants
     */
    public boolean isInStock() {
        return availableVariantCount > 0;
    }
}
