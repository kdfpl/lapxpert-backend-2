package com.lapxpert.backend.sanpham.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;

/**
 * Entity representing individual serial numbers for laptop inventory tracking.
 * Each serial number represents exactly one physical laptop unit.
 * Provides complete traceability from manufacturing to sale/return.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "serial_number", 
    indexes = {
        @Index(name = "idx_serial_number_value", columnList = "serial_number_value"),
        @Index(name = "idx_serial_number_status", columnList = "trang_thai"),
        @Index(name = "idx_serial_number_variant", columnList = "san_pham_chi_tiet_id"),
        @Index(name = "idx_serial_number_reservation", columnList = "thoi_gian_dat_truoc"),
        @Index(name = "idx_serial_number_channel", columnList = "kenh_dat_truoc")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_serial_number_value", columnNames = {"serial_number_value"})
    })
public class SerialNumber extends BaseAuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "serial_number_id_gen")
    @SequenceGenerator(name = "serial_number_id_gen", sequenceName = "serial_number_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The actual serial number value (e.g., "MBA-M2-8GB-256GB-0001")
     * Must be unique across the entire system
     */
    @NotBlank(message = "Số serial không được để trống")
    @Size(max = 100, message = "Số serial không được vượt quá 100 ký tự")
    @Column(name = "serial_number_value", nullable = false, length = 100, unique = true)
    private String serialNumberValue;

    /**
     * Reference to the product variant this serial number belongs to
     * This defines the laptop's specifications (CPU, RAM, GPU, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT) // Prevent deletion of variants with existing serial numbers
    @JoinColumn(name = "san_pham_chi_tiet_id", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    /**
     * Current status of this individual laptop unit
     */
    @NotNull(message = "Trạng thái serial number không được để trống")
    @ColumnDefault("'AVAILABLE'")
    @Column(name = "trang_thai", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TrangThaiSerialNumber trangThai = TrangThaiSerialNumber.AVAILABLE;

    /**
     * Timestamp when this unit was reserved (for timeout management)
     */
    @Column(name = "thoi_gian_dat_truoc")
    private Instant thoiGianDatTruoc;

    /**
     * Channel that reserved this unit (POS, ONLINE, etc.)
     */
    @Column(name = "kenh_dat_truoc", length = 20)
    private String kenhDatTruoc;

    /**
     * Order ID that reserved this unit (for tracking)
     */
    @Column(name = "don_hang_dat_truoc", length = 50)
    private String donHangDatTruoc;

    /**
     * Batch number for tracking manufacturing batches
     */
    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    /**
     * Manufacturing date of this laptop unit
     */
    @Column(name = "ngay_san_xuat")
    private Instant ngaySanXuat;

    /**
     * Warranty expiration date
     */
    @Column(name = "ngay_het_bao_hanh")
    private Instant ngayHetBaoHanh;

    /**
     * Supplier/vendor information
     */
    @Column(name = "nha_cung_cap", length = 100)
    private String nhaCungCap;

    /**
     * Import batch reference for bulk operations
     */
    @Column(name = "import_batch_id", length = 50)
    private String importBatchId;

    /**
     * Additional notes or comments about this unit
     */
    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    // Business Logic Methods

    /**
     * Check if this serial number is available for sale
     */
    public boolean isAvailable() {
        return trangThai == TrangThaiSerialNumber.AVAILABLE;
    }

    /**
     * Check if this serial number is reserved
     */
    public boolean isReserved() {
        return trangThai == TrangThaiSerialNumber.RESERVED;
    }

    /**
     * Check if this serial number has been sold
     */
    public boolean isSold() {
        return trangThai == TrangThaiSerialNumber.SOLD;
    }

    /**
     * Check if this serial number has been returned
     */
    public boolean isReturned() {
        return trangThai == TrangThaiSerialNumber.RETURNED;
    }

    /**
     * Check if this serial number is damaged
     */
    public boolean isDamaged() {
        return trangThai == TrangThaiSerialNumber.DAMAGED;
    }

    /**
     * Reserve this serial number for an order
     */
    public void reserve() {
        if (!isAvailable()) {
            throw new IllegalStateException("Cannot reserve serial number that is not available. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.RESERVED;
        this.thoiGianDatTruoc = Instant.now();
    }

    /**
     * Reserve this serial number with tracking information
     */
    public void reserveWithTracking(String channel, String orderId) {
        if (!isAvailable()) {
            throw new IllegalStateException("Cannot reserve serial number that is not available. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.RESERVED;
        this.thoiGianDatTruoc = Instant.now();
        this.kenhDatTruoc = channel;
        this.donHangDatTruoc = orderId;
    }

    /**
     * Mark this serial number as sold
     */
    public void markAsSold() {
        if (!isReserved() && !isAvailable()) {
            throw new IllegalStateException("Cannot sell serial number that is not available or reserved. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.SOLD;
    }

    /**
     * Release reservation and make available again
     */
    public void releaseReservation() {
        if (!isReserved()) {
            throw new IllegalStateException("Cannot release reservation for serial number that is not reserved. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.AVAILABLE;
        this.thoiGianDatTruoc = null;
        this.kenhDatTruoc = null;
        this.donHangDatTruoc = null;
    }

    /**
     * Mark as returned from sold status
     */
    public void markAsReturned() {
        if (!isSold()) {
            throw new IllegalStateException("Cannot return serial number that was not sold. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.RETURNED;
    }

    /**
     * Release from sold status back to available (for refunds)
     */
    public void releaseFromSold() {
        if (!isSold() && !isReturned()) {
            throw new IllegalStateException("Cannot release serial number from sold that is not sold or returned. Current status: " + trangThai);
        }
        this.trangThai = TrangThaiSerialNumber.AVAILABLE;
        this.thoiGianDatTruoc = null;
        this.kenhDatTruoc = null;
        this.donHangDatTruoc = null;
    }

    /**
     * Mark as damaged
     */
    public void markAsDamaged(String reason) {
        this.trangThai = TrangThaiSerialNumber.DAMAGED;
        this.ghiChu = reason;
    }

    /**
     * Mark as unavailable
     */
    public void markAsUnavailable(String reason) {
        this.trangThai = TrangThaiSerialNumber.UNAVAILABLE;
        this.ghiChu = reason;
    }

    /**
     * Check if reservation has expired (15 minutes default)
     */
    public boolean isReservationExpired() {
        return isReserved() && 
               thoiGianDatTruoc != null && 
               Instant.now().isAfter(thoiGianDatTruoc.plusSeconds(900)); // 15 minutes
    }

    /**
     * Check if reservation has expired with custom timeout
     */
    public boolean isReservationExpired(long timeoutSeconds) {
        return isReserved() && 
               thoiGianDatTruoc != null && 
               Instant.now().isAfter(thoiGianDatTruoc.plusSeconds(timeoutSeconds));
    }

    /**
     * Generate display name for this serial number
     */
    public String getDisplayName() {
        if (sanPhamChiTiet != null && sanPhamChiTiet.getSanPham() != null) {
            return sanPhamChiTiet.getSanPham().getTenSanPham() + " - " + serialNumberValue;
        }
        return serialNumberValue;
    }
}
