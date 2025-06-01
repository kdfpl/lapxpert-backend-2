package com.lapxpert.backend.giohang.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Entity representing individual items in a shopping cart
 * Tracks product variants, quantities, and prices at time of addition
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gio_hang_chi_tiet",
    indexes = {
        @Index(name = "idx_gio_hang_chi_tiet_gio_hang", columnList = "gio_hang_id"),
        @Index(name = "idx_gio_hang_chi_tiet_san_pham", columnList = "san_pham_chi_tiet_id"),
        @Index(name = "idx_gio_hang_chi_tiet_ngay_them", columnList = "ngay_tao")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_gio_hang_san_pham",
                         columnNames = {"gio_hang_id", "san_pham_chi_tiet_id"})
    })
public class GioHangChiTiet extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gio_hang_chi_tiet_id_gen")
    @SequenceGenerator(name = "gio_hang_chi_tiet_id_gen", sequenceName = "gio_hang_chi_tiet_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gio_hang_id", nullable = false)
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_chi_tiet_id", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    /**
     * Price at the time the item was added to cart
     * This preserves pricing even if product price changes
     */
    @Column(name = "gia_tai_thoi_diem_them", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTaiThoiDiemThem;

    /**
     * Calculate total amount for this cart item
     * @return quantity * price at time of addition
     */
    public BigDecimal getThanhTien() {
        if (giaTaiThoiDiemThem == null || soLuong == null) {
            return BigDecimal.ZERO;
        }
        return giaTaiThoiDiemThem.multiply(BigDecimal.valueOf(soLuong));
    }

    /**
     * Check if the current product price differs from cart price
     * @return true if price has changed since adding to cart
     */
    public boolean hasPriceChanged() {
        if (sanPhamChiTiet == null || sanPhamChiTiet.getGiaBan() == null) {
            return false;
        }
        return !giaTaiThoiDiemThem.equals(sanPhamChiTiet.getGiaBan());
    }

    /**
     * Get the current product price
     * @return current selling price of the product
     */
    public BigDecimal getCurrentPrice() {
        return sanPhamChiTiet != null ? sanPhamChiTiet.getGiaBan() : BigDecimal.ZERO;
    }

    /**
     * Calculate price difference between cart price and current price
     * @return positive if current price is higher, negative if lower
     */
    public BigDecimal getPriceDifference() {
        return getCurrentPrice().subtract(giaTaiThoiDiemThem);
    }

    /**
     * Update cart price to current product price
     */
    public void updateToCurrentPrice() {
        if (sanPhamChiTiet != null && sanPhamChiTiet.getGiaBan() != null) {
            this.giaTaiThoiDiemThem = sanPhamChiTiet.getGiaBan();
        }
    }

    /**
     * Check if the product is still available
     * @return true if product is available for purchase
     */
    public boolean isProductAvailable() {
        return sanPhamChiTiet != null &&
               sanPhamChiTiet.getTrangThai() == TrangThaiSanPham.AVAILABLE;
    }

    /**
     * Get product name for display
     * @return product name or empty string if not available
     */
    public String getProductName() {
        return sanPhamChiTiet != null && sanPhamChiTiet.getSanPham() != null
            ? sanPhamChiTiet.getSanPham().getTenSanPham()
            : "";
    }

    /**
     * Get product serial number for display
     * @return product serial number or empty string if not available
     */
    public String getProductSerialNumber() {
        return sanPhamChiTiet != null ? sanPhamChiTiet.getSerialNumber() : "";
    }

    /**
     * JPA lifecycle method for price initialization before persist
     */
    @PrePersist
    public void onPrePersist() {
        // Step 1: Set price if null (must happen before validation)
        if (giaTaiThoiDiemThem == null && sanPhamChiTiet != null) {
            this.giaTaiThoiDiemThem = sanPhamChiTiet.getGiaBan();
        }

        // Step 2: Validate after price is set
        validateCartItem();
    }

    /**
     * JPA lifecycle method for validation before update
     */
    @PreUpdate
    public void onPreUpdate() {
        // Validate cart item data
        validateCartItem();
    }

    /**
     * Private validation method called from lifecycle methods
     */
    private void validateCartItem() {
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (giaTaiThoiDiemThem == null || giaTaiThoiDiemThem.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
    }
}
