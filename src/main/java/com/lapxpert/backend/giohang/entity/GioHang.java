package com.lapxpert.backend.giohang.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user's shopping cart
 * Maintains persistent cart state across sessions
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gio_hang",
    indexes = {
        @Index(name = "idx_gio_hang_nguoi_dung", columnList = "nguoi_dung_id"),
        @Index(name = "idx_gio_hang_ngay_cap_nhat", columnList = "ngay_cap_nhat")
    })
public class GioHang extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gio_hang_id_gen")
    @SequenceGenerator(name = "gio_hang_id_gen", sequenceName = "gio_hang_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false, unique = true)
    private NguoiDung nguoiDung;

    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GioHangChiTiet> chiTiets = new ArrayList<>();

    /**
     * Calculate total amount for all items in cart
     * @return total cart value
     */
    public BigDecimal getTongTien() {
        return chiTiets.stream()
            .map(GioHangChiTiet::getThanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total quantity of all items in cart
     * @return total item count
     */
    public Integer getTongSoLuong() {
        return chiTiets.stream()
            .mapToInt(GioHangChiTiet::getSoLuong)
            .sum();
    }

    /**
     * Check if cart is empty
     * @return true if cart has no items
     */
    public boolean isEmpty() {
        return chiTiets.isEmpty();
    }

    /**
     * Get number of unique items in cart
     * @return number of different products
     */
    public int getUniqueItemCount() {
        return chiTiets.size();
    }

    /**
     * Add item to cart or update quantity if already exists
     * @param gioHangChiTiet cart item to add
     */
    public void addItem(GioHangChiTiet gioHangChiTiet) {
        // Check if item already exists
        GioHangChiTiet existingItem = chiTiets.stream()
            .filter(item -> item.getSanPhamChiTiet().getId().equals(
                gioHangChiTiet.getSanPhamChiTiet().getId()))
            .findFirst()
            .orElse(null);

        if (existingItem != null) {
            // Update quantity of existing item
            existingItem.setSoLuong(existingItem.getSoLuong() + gioHangChiTiet.getSoLuong());
        } else {
            // Add new item
            gioHangChiTiet.setGioHang(this);
            chiTiets.add(gioHangChiTiet);
        }
    }

    /**
     * Remove item from cart
     * @param sanPhamChiTietId product variant ID to remove
     */
    public void removeItem(Long sanPhamChiTietId) {
        chiTiets.removeIf(item ->
            item.getSanPhamChiTiet().getId().equals(sanPhamChiTietId));
    }

    /**
     * Update item quantity in cart
     * @param sanPhamChiTietId product variant ID
     * @param newQuantity new quantity
     */
    public void updateItemQuantity(Long sanPhamChiTietId, Integer newQuantity) {
        if (newQuantity <= 0) {
            removeItem(sanPhamChiTietId);
            return;
        }

        chiTiets.stream()
            .filter(item -> item.getSanPhamChiTiet().getId().equals(sanPhamChiTietId))
            .findFirst()
            .ifPresent(item -> item.setSoLuong(newQuantity));
    }

    /**
     * Clear all items from cart
     */
    public void clearCart() {
        chiTiets.clear();
    }

    /**
     * Check if cart contains a specific product variant
     * @param sanPhamChiTietId product variant ID
     * @return true if cart contains the item
     */
    public boolean containsItem(Long sanPhamChiTietId) {
        return chiTiets.stream()
            .anyMatch(item -> item.getSanPhamChiTiet().getId().equals(sanPhamChiTietId));
    }

    /**
     * Get quantity of a specific item in cart
     * @param sanPhamChiTietId product variant ID
     * @return quantity or 0 if not found
     */
    public Integer getItemQuantity(Long sanPhamChiTietId) {
        return chiTiets.stream()
            .filter(item -> item.getSanPhamChiTiet().getId().equals(sanPhamChiTietId))
            .findFirst()
            .map(GioHangChiTiet::getSoLuong)
            .orElse(0);
    }
}
