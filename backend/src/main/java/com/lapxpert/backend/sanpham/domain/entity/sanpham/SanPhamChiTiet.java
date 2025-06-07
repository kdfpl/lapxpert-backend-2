package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing product variants with specific configurations.
 * Each variant defines a unique sellable configuration with 6 core attributes:
 * CPU, RAM, GPU, Color (MauSac), Storage (OCung), Screen Size (ManHinh).
 * Individual units are tracked separately in SerialNumber entity.
 * Uses BaseAuditableEntity for basic audit fields and SanPhamChiTietAuditHistory for detailed change tracking.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "san_pham_chi_tiet",
    indexes = {
        @Index(name = "idx_san_pham_chi_tiet_sku", columnList = "sku"),
        @Index(name = "idx_san_pham_chi_tiet_san_pham", columnList = "san_pham_id"),
        @Index(name = "idx_san_pham_chi_tiet_active", columnList = "trang_thai")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_san_pham_chi_tiet_sku", columnNames = {"sku"})
    })
public class SanPhamChiTiet extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "san_pham_chi_tiet_id_gen")
    @SequenceGenerator(name = "san_pham_chi_tiet_id_gen", sequenceName = "san_pham_chi_tiet_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    /**
     * Unique SKU for variant identification (e.g., "MBA-M2-8GB-256GB-SILVER")
     * Used for variant-level operations and inventory grouping
     */
    @Column(name = "sku", length = 100, unique = true)
    private String sku;

    // === 6 CORE ATTRIBUTES (as per requirements) ===

    /**
     * CPU specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "cpu_id")
    private Cpu cpu;

    /**
     * RAM specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ram_id")
    private Ram ram;

    /**
     * GPU specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "gpu_id")
    private Gpu gpu;

    /**
     * Color specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "mau_sac_id")
    private MauSac mauSac;

    /**
     * Storage specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "o_cung_id")
    private OCung oCung;

    /**
     * Screen size specification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "man_hinh_id")
    private ManHinh manHinh;

    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "gia_khuyen_mai", precision = 15, scale = 2)
    private BigDecimal giaKhuyenMai;

    @Column(name = "hinh_anh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> hinhAnh;

    /**
     * Variant status (active/inactive for sales)
     */
    @ColumnDefault("true")
    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private Boolean trangThai = true;

    @ManyToMany
    @JoinTable(name = "san_pham_chi_tiet_dot_giam_gia",
            joinColumns = @JoinColumn(name = "san_pham_chi_tiet_id"),
            inverseJoinColumns = @JoinColumn(name = "dot_giam_gia_id"))
    @Builder.Default
    private Set<DotGiamGia> dotGiamGias = new LinkedHashSet<>();

    /**
     * Check if this variant is active for sales
     * @return true if variant is active
     */
    public boolean isActive() {
        return trangThai != null && trangThai;
    }

    /**
     * Activate this variant for sales
     */
    public void activate() {
        this.trangThai = true;
    }

    /**
     * Deactivate this variant from sales
     */
    public void deactivate() {
        this.trangThai = false;
    }

    /**
     * Get effective selling price (promotional price if available, otherwise regular price)
     * @return effective price for this item
     */
    public BigDecimal getEffectivePrice() {
        if (giaKhuyenMai != null && giaKhuyenMai.compareTo(BigDecimal.ZERO) > 0) {
            return giaKhuyenMai;
        }
        return giaBan != null ? giaBan : BigDecimal.ZERO;
    }

    /**
     * Check if item has promotional pricing
     * @return true if promotional price is set and lower than regular price
     */
    public boolean hasPromotionalPrice() {
        return giaKhuyenMai != null &&
               giaBan != null &&
               giaKhuyenMai.compareTo(giaBan) < 0;
    }

    /**
     * Calculate discount amount if promotional price is available
     * @return discount amount or zero if no promotion
     */
    public BigDecimal getDiscountAmount() {
        if (hasPromotionalPrice()) {
            return giaBan.subtract(giaKhuyenMai);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate discount percentage if promotional price is available
     * @return discount percentage or zero if no promotion
     */
    public BigDecimal getDiscountPercentage() {
        if (hasPromotionalPrice() && giaBan.compareTo(BigDecimal.ZERO) > 0) {
            return getDiscountAmount()
                .divide(giaBan, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get product name for display
     * @return product name or empty string if not available
     */
    public String getProductName() {
        return sanPham != null ? sanPham.getTenSanPham() : "";
    }

    /**
     * Get full product display name including variant info
     * @return formatted product name with color if available
     */
    public String getFullDisplayName() {
        String productName = getProductName();
        if (mauSac != null && mauSac.getMoTaMauSac() != null && !mauSac.getMoTaMauSac().trim().isEmpty()) {
            return productName + " - " + mauSac.getMoTaMauSac();
        }
        return productName;
    }

    /**
     * Check if item has active discount campaigns
     * @return true if any active campaigns apply to this item
     */
    public boolean hasActiveCampaigns() {
        return dotGiamGias != null &&
               dotGiamGias.stream().anyMatch(DotGiamGia::isActive);
    }

    /**
     * Get the best discount percentage from active campaigns
     * @return highest discount percentage from active campaigns
     */
    public BigDecimal getBestCampaignDiscount() {
        if (dotGiamGias == null) {
            return BigDecimal.ZERO;
        }

        return dotGiamGias.stream()
            .filter(DotGiamGia::isActive)
            .map(DotGiamGia::getPhanTramGiam)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Validate entity before persist
     */
    @PrePersist
    public void onPrePersist() {
        validateForPersist();
    }

    /**
     * Validate entity before update
     */
    @PreUpdate
    public void onPreUpdate() {
        validateForUpdate();
    }

    /**
     * Validation method for entity creation
     */
    private void validateForPersist() {
        validateCommonFields();
        // Additional validation specific to entity creation can be added here
    }

    /**
     * Validation method for entity updates
     */
    private void validateForUpdate() {
        validateCommonFields();
        // Additional validation specific to entity updates can be added here
    }

    /**
     * Common validation method for both persist and update
     */
    private void validateCommonFields() {
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Selling price must be non-negative");
        }
        if (giaKhuyenMai != null && giaKhuyenMai.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Promotional price must be non-negative");
        }
        if (giaKhuyenMai != null && giaBan != null && giaKhuyenMai.compareTo(giaBan) > 0) {
            throw new IllegalArgumentException("Promotional price cannot be higher than regular price");
        }
    }
}