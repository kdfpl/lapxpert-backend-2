package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.math.BigDecimal;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.*;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing individual product variants with specific configurations.
 * Each variant represents a unique sellable item with its own serial number.
 * Uses BaseAuditableEntity for basic audit fields and SanPhamChiTietAuditHistory for detailed change tracking.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "san_pham_chi_tiet")
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
     * Unique serial number for individual item tracking
     * Renamed from 'sku' to better represent individual item identification
     */
    @NotBlank(message = "Số serial không được để trống")
    @Size(max = 100, message = "Số serial không được vượt quá 100 ký tự")
    @Column(name = "serial_number", nullable = false, length = 100, unique = true)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "mau_sac_id")
    private MauSac mauSac;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải lớn hơn hoặc bằng 0")
    @Digits(integer = 13, fraction = 2, message = "Giá bán không hợp lệ")
    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá khuyến mãi phải lớn hơn hoặc bằng 0")
    @Digits(integer = 13, fraction = 2, message = "Giá khuyến mãi không hợp lệ")
    @Column(name = "gia_khuyen_mai", precision = 15, scale = 2)
    private BigDecimal giaKhuyenMai;

    @Column(name = "hinh_anh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> hinhAnh;

    /**
     * Individual item status for inventory tracking
     * Uses enum for better type safety and business logic
     */
    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    @ColumnDefault("'AVAILABLE'")
    @Column(name = "trang_thai", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TrangThaiSanPham trangThai = TrangThaiSanPham.AVAILABLE;

    /**
     * Timestamp when item was reserved (for timeout management)
     */
    @Column(name = "thoi_gian_dat_truoc")
    private Instant thoiGianDatTruoc;

    /**
     * Channel that reserved this item (POS, ONLINE, etc.)
     */
    @Column(name = "kenh_dat_truoc", length = 20)
    private String kenhDatTruoc;

    /**
     * Order ID that reserved this item (for tracking)
     */
    @Column(name = "don_hang_dat_truoc", length = 50)
    private String donHangDatTruoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "cpu_id")
    private Cpu cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ram_id")
    private Ram ram;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "o_cung_id")
    private OCung oCung;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "gpu_id")
    private Gpu gpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "man_hinh_id")
    private ManHinh manHinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "cong_giao_tiep_id")
    private CongGiaoTiep congGiaoTiep;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ban_phim_id")
    private BanPhim banPhim;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ket_noi_mang_id")
    private KetNoiMang ketNoiMang;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "am_thanh_id")
    private AmThanh amThanh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "webcam_id")
    private Webcam webcam;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "bao_mat_id")
    private BaoMat baoMat;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "he_dieu_hanh_id")
    private HeDieuHanh heDieuHanh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "pin_id")
    private Pin pin;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "thiet_ke_id")
    private ThietKe thietKe;

    @ManyToMany
    @JoinTable(name = "san_pham_chi_tiet_dot_giam_gia",
            joinColumns = @JoinColumn(name = "san_pham_chi_tiet_id"),
            inverseJoinColumns = @JoinColumn(name = "dot_giam_gia_id"))
    @Builder.Default
    private Set<DotGiamGia> dotGiamGias = new LinkedHashSet<>();

    /**
     * Check if this item is available for purchase
     * @return true if item can be sold
     */
    public boolean isAvailable() {
        return trangThai == TrangThaiSanPham.AVAILABLE;
    }

    /**
     * Check if this item is reserved
     * @return true if item is reserved for an order
     */
    public boolean isReserved() {
        return trangThai == TrangThaiSanPham.RESERVED;
    }

    /**
     * Check if this item has been sold
     * @return true if item has been sold
     */
    public boolean isSold() {
        return trangThai == TrangThaiSanPham.SOLD;
    }

    /**
     * Reserve this item for an order
     */
    public void reserve() {
        if (!isAvailable()) {
            throw new IllegalStateException("Cannot reserve item that is not available. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.RESERVED;
        this.thoiGianDatTruoc = Instant.now();
    }

    /**
     * Reserve this item for an order with channel and order tracking
     * @param channel Channel that is reserving the item (POS, ONLINE, etc.)
     * @param orderId Order ID for tracking
     */
    public void reserveWithTracking(String channel, String orderId) {
        if (!isAvailable()) {
            throw new IllegalStateException("Cannot reserve item that is not available. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.RESERVED;
        this.thoiGianDatTruoc = Instant.now();
        this.kenhDatTruoc = channel;
        this.donHangDatTruoc = orderId;
    }

    /**
     * Mark this item as sold
     */
    public void markAsSold() {
        if (!isReserved() && !isAvailable()) {
            throw new IllegalStateException("Cannot sell item that is not available or reserved. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.SOLD;
    }

    /**
     * Release reservation and make item available again
     */
    public void releaseReservation() {
        if (!isReserved()) {
            throw new IllegalStateException("Cannot release reservation for item that is not reserved. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.AVAILABLE;
        this.thoiGianDatTruoc = null;
        this.kenhDatTruoc = null;
        this.donHangDatTruoc = null;
    }

    /**
     * Check if reservation has expired based on timeout duration
     * @param timeoutMinutes Timeout duration in minutes
     * @return true if reservation has expired
     */
    public boolean isReservationExpired(long timeoutMinutes) {
        if (!isReserved() || thoiGianDatTruoc == null) {
            return false;
        }
        Instant expireTime = thoiGianDatTruoc.plusSeconds(timeoutMinutes * 60);
        return Instant.now().isAfter(expireTime);
    }

    /**
     * Check if this item is reserved by a specific channel
     * @param channel Channel to check
     * @return true if reserved by the specified channel
     */
    public boolean isReservedByChannel(String channel) {
        return isReserved() && channel != null && channel.equals(kenhDatTruoc);
    }

    /**
     * Check if this item is reserved for a specific order
     * @param orderId Order ID to check
     * @return true if reserved for the specified order
     */
    public boolean isReservedForOrder(String orderId) {
        return isReserved() && orderId != null && orderId.equals(donHangDatTruoc);
    }

    /**
     * Release item from sold status back to available (for refunds)
     */
    public void releaseFromSold() {
        if (!isSold()) {
            throw new IllegalStateException("Cannot release from sold status for item that was not sold. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.AVAILABLE;
        // Clear any tracking information
        this.thoiGianDatTruoc = null;
        this.kenhDatTruoc = null;
        this.donHangDatTruoc = null;
    }

    /**
     * Mark item as returned
     */
    public void markAsReturned() {
        if (!isSold()) {
            throw new IllegalStateException("Cannot return item that was not sold. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSanPham.RETURNED;
    }

    /**
     * Mark item as damaged
     */
    public void markAsDamaged() {
        this.trangThai = TrangThaiSanPham.DAMAGED;
    }

    /**
     * Mark item as unavailable
     */
    public void markAsUnavailable() {
        this.trangThai = TrangThaiSanPham.UNAVAILABLE;
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
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Serial number cannot be null or empty");
        }
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