package com.lapxpert.backend.danhsachyeuthich.application.dto;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import lombok.*;

import java.math.BigDecimal;

/**
 * Summary DTO for SanPhamChiTiet used in wishlist context
 * Provides essential product variant information for wishlist-to-cart conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamChiTietSummaryDto {

    private Long id;

    /**
     * Product variant identification
     */
    private String serialNumber;
    private MauSac mauSac;

    /**
     * Pricing information
     */
    private BigDecimal giaBan;
    private BigDecimal giaKhuyenMai;

    /**
     * Product variant images
     */
    private String hinhAnh;

    /**
     * Product variant status
     */
    private TrangThaiSanPham trangThai;

    /**
     * Computed fields for business logic
     */

    /**
     * Indicates if variant has promotional pricing
     */
    public boolean hasPromotionalPrice() {
        return giaKhuyenMai != null &&
               giaBan != null &&
               giaKhuyenMai.compareTo(BigDecimal.ZERO) > 0 &&
               giaKhuyenMai.compareTo(giaBan) < 0;
    }

    /**
     * Get effective selling price (promotional if available, otherwise regular)
     */
    public BigDecimal getEffectivePrice() {
        if (hasPromotionalPrice()) {
            return giaKhuyenMai;
        }
        return giaBan != null ? giaBan : BigDecimal.ZERO;
    }

    /**
     * Calculate discount percentage if promotional price is available
     */
    public BigDecimal getDiscountPercentage() {
        if (!hasPromotionalPrice() || giaBan.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = giaBan.subtract(giaKhuyenMai);
        return discount.divide(giaBan, 4, java.math.RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100))
                      .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Check if variant is available for purchase
     */
    public boolean isAvailable() {
        return trangThai == TrangThaiSanPham.AVAILABLE;
    }

    /**
     * Get display name for variant
     * @return formatted variant name with color if available
     */
    public String getDisplayName() {
        if (mauSac != null && mauSac.getMoTaMauSac() != null && !mauSac.getMoTaMauSac().trim().isEmpty()) {
            return mauSac.getMoTaMauSac();
        }
        return serialNumber != null ? serialNumber : "Mặc định";
    }


}
