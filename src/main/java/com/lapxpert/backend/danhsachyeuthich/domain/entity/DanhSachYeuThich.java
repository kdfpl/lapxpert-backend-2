package com.lapxpert.backend.danhsachyeuthich.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Entity representing user's wishlist items
 * Allows users to save products for later consideration
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "danh_sach_yeu_thich",
    indexes = {
        @Index(name = "idx_danh_sach_yeu_thich_nguoi_dung", columnList = "nguoi_dung_id"),
        @Index(name = "idx_danh_sach_yeu_thich_san_pham", columnList = "san_pham_id"),
        @Index(name = "idx_danh_sach_yeu_thich_ngay_them", columnList = "ngay_tao")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_nguoi_dung_san_pham",
                         columnNames = {"nguoi_dung_id", "san_pham_id"})
    })
public class DanhSachYeuThich extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "danh_sach_yeu_thich_id_gen")
    @SequenceGenerator(name = "danh_sach_yeu_thich_id_gen", sequenceName = "danh_sach_yeu_thich_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    /**
     * Price when item was added to wishlist for price tracking
     * Stores the minimum price at the time of addition
     */
    @Column(name = "gia_khi_them", precision = 15, scale = 2)
    private BigDecimal giaKhiThem;

    /**
     * Check if the product is still available
     * @return true if product is available
     * TODO: Update to use TrangThaiSanPham enum when SanPham entity is migrated from Boolean
     */
    public boolean isProductAvailable() {
        return sanPham != null && Boolean.TRUE.equals(sanPham.getTrangThai());
    }

    /**
     * Get product name for display
     * @return product name or empty string if not available
     */
    public String getProductName() {
        return sanPham != null ? sanPham.getTenSanPham() : "";
    }

    /**
     * Get product code for display
     * @return product code or empty string if not available
     */
    public String getProductCode() {
        return sanPham != null ? sanPham.getMaSanPham() : "";
    }

    /**
     * Check if product has any available variants
     * @return true if product has available variants
     */
    public boolean hasAvailableVariants() {
        return sanPham != null &&
               sanPham.getSanPhamChiTiets() != null &&
               sanPham.getSanPhamChiTiets().stream()
                   .anyMatch(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE);
    }

    /**
     * Get the lowest price among available variants
     * @return lowest price or null if no variants available
     */
    public java.math.BigDecimal getLowestPrice() {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return null;
        }

        return sanPham.getSanPhamChiTiets().stream()
            .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
            .map(variant -> variant.getGiaBan())
            .filter(price -> price != null)
            .min(java.math.BigDecimal::compareTo)
            .orElse(null);
    }

    /**
     * Get the highest price among available variants
     * @return highest price or null if no variants available
     */
    public java.math.BigDecimal getHighestPrice() {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return null;
        }

        return sanPham.getSanPhamChiTiets().stream()
            .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
            .map(variant -> variant.getGiaBan())
            .filter(price -> price != null)
            .max(java.math.BigDecimal::compareTo)
            .orElse(null);
    }

    /**
     * Check if product is on sale (has promotional price)
     * @return true if any variant has promotional pricing
     */
    public boolean isOnSale() {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return false;
        }

        return sanPham.getSanPhamChiTiets().stream()
            .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
            .anyMatch(variant -> variant.getGiaKhuyenMai() != null &&
                               variant.getGiaKhuyenMai().compareTo(variant.getGiaBan()) < 0);
    }

    /**
     * Get number of available variants
     * @return count of available variants
     */
    public long getAvailableVariantCount() {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return 0;
        }

        return sanPham.getSanPhamChiTiets().stream()
            .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
            .count();
    }

    /**
     * Check if price has dropped since adding to wishlist
     * @return true if current price is lower than stored original price
     */
    public boolean hasPriceDropped() {
        if (giaKhiThem == null || giaKhiThem.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return false;
        }

        java.math.BigDecimal currentPrice = getLowestPrice();
        return currentPrice != null &&
               currentPrice.compareTo(java.math.BigDecimal.ZERO) > 0 &&
               currentPrice.compareTo(giaKhiThem) < 0;
    }

    /**
     * Calculate price drop percentage
     * @return percentage of price drop or zero if no drop
     */
    public java.math.BigDecimal getPriceDropPercentage() {
        if (!hasPriceDropped()) {
            return java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal currentPrice = getLowestPrice();
        java.math.BigDecimal difference = giaKhiThem.subtract(currentPrice);

        return difference.divide(giaKhiThem, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Validate wishlist item before persist
     */
    @PrePersist
    public void onPrePersist() {
        validateWishlistItem();
    }

    /**
     * Validate wishlist item before update
     */
    @PreUpdate
    public void onPreUpdate() {
        validateWishlistItem();
    }

    /**
     * Private validation method
     */
    private void validateWishlistItem() {
        if (nguoiDung == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (sanPham == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
    }
}
