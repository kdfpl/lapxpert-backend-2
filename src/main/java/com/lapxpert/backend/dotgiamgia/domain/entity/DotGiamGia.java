package com.lapxpert.backend.dotgiamgia.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity representing discount campaigns for products
 * Provides percentage-based discounts for specific time periods
 * Uses BaseAuditableEntity for basic audit fields and DotGiamGiaAuditHistory for detailed change tracking
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "dot_giam_gia",
    indexes = {
        @Index(name = "idx_dot_giam_gia_ma", columnList = "ma_dot_giam_gia"),
        @Index(name = "idx_dot_giam_gia_trang_thai", columnList = "trang_thai"),
        @Index(name = "idx_dot_giam_gia_ngay_bat_dau", columnList = "ngay_bat_dau"),
        @Index(name = "idx_dot_giam_gia_ngay_ket_thuc", columnList = "ngay_ket_thuc")
    })
public class DotGiamGia extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dot_giam_gia_id_gen")
    @SequenceGenerator(name = "dot_giam_gia_id_gen", sequenceName = "dot_giam_gia_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_dot_giam_gia", nullable = false, length = 50)
    private String maDotGiamGia;

    @Column(name = "ten_dot_giam_gia", nullable = false)
    private String tenDotGiamGia;

    @Column(name = "phan_tram_giam", nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    @Column(name = "ngay_bat_dau", nullable = false)
    private Instant ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private Instant ngayKetThuc;



    @ColumnDefault("'CHUA_DIEN_RA'")
    @Column(name = "trang_thai")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TrangThaiCampaign trangThai = TrangThaiCampaign.CHUA_DIEN_RA;

    @ManyToMany(mappedBy = "dotGiamGias")
    @Builder.Default
    private Set<SanPhamChiTiet> sanPhamChiTiets = new LinkedHashSet<>();

    /**
     * Transient field to track if status was manually set
     * This prevents automatic status updates from overriding manual changes
     */
    @Transient
    @Builder.Default
    private boolean statusManuallySet = false;

    /**
     * Check if discount campaign is currently active
     * @return true if campaign is active and not cancelled
     */
    public boolean isActive() {
        return trangThai != null && trangThai.isActive() && !trangThai.isCancelled();
    }

    /**
     * Check if campaign is currently running based on dates and status
     * @return true if campaign is currently running and not cancelled
     */
    public boolean isCurrentlyActive() {
        Instant now = Instant.now();
        return trangThai == TrangThaiCampaign.DA_DIEN_RA &&
               !trangThai.isCancelled() &&
               ngayBatDau != null && ngayKetThuc != null &&
               now.isAfter(ngayBatDau) &&
               now.isBefore(ngayKetThuc);
    }

    /**
     * Check if campaign can be modified
     * @return true if campaign hasn't started yet
     */
    public boolean canBeModified() {
        return trangThai != null && trangThai.canBeModified();
    }

    /**
     * Activate the campaign
     * Restores campaign from BI_HUY status if it was cancelled
     */
    public void activate() {
        if (canBeModified()) {
            // If campaign was cancelled, restore it and update status based on dates
            if (this.trangThai == TrangThaiCampaign.BI_HUY) {
                updateStatus();
            }
        }
    }

    /**
     * Deactivate the campaign (soft delete)
     * Sets status to BI_HUY regardless of current campaign status
     */
    public void deactivate() {
        this.trangThai = TrangThaiCampaign.BI_HUY;
    }

    /**
     * Comprehensive validation before persist
     * Following PhieuGiamGia pattern exactly
     * @throws IllegalArgumentException if any validation fails
     */
    @PrePersist
    public void onPrePersist() {
        validateCampaignDates();

        // Update status based on current date using Vietnam timezone
        if (ngayBatDau != null && ngayKetThuc != null) {
            this.trangThai = fromDates(ngayBatDau, ngayKetThuc);
        }
    }

    /**
     * Comprehensive validation before update
     * Following PhieuGiamGia pattern exactly
     * @throws IllegalArgumentException if any validation fails
     */
    @PreUpdate
    public void onPreUpdate() {
        validateCampaignDates();

        // Update status based on current date, but don't override manually set BI_HUY status
        // Following PhieuGiamGia pattern exactly
        if (ngayBatDau != null && ngayKetThuc != null && trangThai != TrangThaiCampaign.BI_HUY) {
            this.trangThai = fromDates(ngayBatDau, ngayKetThuc);
        }
    }

    /**
     * Validate campaign dates and business rules
     * Private method to ensure proper encapsulation
     */
    private void validateCampaignDates() {
        if (ngayBatDau != null && ngayKetThuc != null) {
            if (ngayKetThuc.isBefore(ngayBatDau) || ngayKetThuc.equals(ngayBatDau)) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }

            // Campaign must run for at least 1 hour
            if (ngayKetThuc.isBefore(ngayBatDau.plusSeconds(3600))) {
                throw new IllegalArgumentException("Đợt giảm giá phải diễn ra ít nhất 1 giờ");
            }
        }
    }

    /**
     * Calculate campaign status based on current date/time using Vietnam timezone
     * This method provides timezone-aware business logic for Vietnamese operations
     * Following PhieuGiamGia pattern exactly
     */
    public static TrangThaiCampaign fromDates(Instant ngayBatDau, Instant ngayKetThuc) {
        if (ngayBatDau == null || ngayKetThuc == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không thể null.");
        }
        return fromDatesWithTimezone(ngayBatDau, ngayKetThuc, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    /**
     * Calculate campaign status with specific timezone
     * Allows for flexible timezone handling while maintaining business logic accuracy
     * Following PhieuGiamGia pattern exactly
     */
    public static TrangThaiCampaign fromDatesWithTimezone(Instant ngayBatDau, Instant ngayKetThuc, ZoneId timezone) {
        if (ngayBatDau == null || ngayKetThuc == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không thể null.");
        }

        ZonedDateTime now = ZonedDateTime.now(timezone);
        ZonedDateTime start = ngayBatDau.atZone(timezone);
        ZonedDateTime end = ngayKetThuc.atZone(timezone);

        if (now.isBefore(start)) {
            return TrangThaiCampaign.CHUA_DIEN_RA;
        } else if (now.isAfter(end)) {
            return TrangThaiCampaign.KET_THUC;
        } else {
            return TrangThaiCampaign.DA_DIEN_RA;
        }
    }

    /**
     * Calculate current status using Vietnam timezone for business operations
     * Following PhieuGiamGia pattern exactly
     */
    public TrangThaiCampaign calculateStatusInVietnamTime() {
        return fromDatesWithTimezone(this.ngayBatDau, this.ngayKetThuc, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    /**
     * Check if campaign should activate today in Vietnam timezone
     * Used by scheduler for precise activation timing
     * Following PhieuGiamGia pattern exactly
     */
    public boolean shouldActivateToday() {
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vietnamZone);
        LocalDate startDate = ngayBatDau.atZone(vietnamZone).toLocalDate();

        return startDate.equals(today) && this.trangThai == TrangThaiCampaign.CHUA_DIEN_RA;
    }

    /**
     * Override setter to track manual status changes
     * This allows us to preserve manual status updates from being overridden by lifecycle methods
     */
    public void setTrangThai(TrangThaiCampaign trangThai) {
        this.trangThai = trangThai;
        this.statusManuallySet = true; // Mark as manually set
    }

    /**
     * Method to reset the manual status flag (for internal use)
     * This allows automatic status updates when needed
     */
    public void resetStatusManualFlag() {
        this.statusManuallySet = false;
    }

    /**
     * Public method to manually update status (for service layer use)
     * This method can be called explicitly when needed
     * Following PhieuGiamGia pattern exactly
     */
    public void updateStatus() {
        if (ngayBatDau != null && ngayKetThuc != null) {
            this.trangThai = fromDates(ngayBatDau, ngayKetThuc);
        }
    }

}