package com.lapxpert.backend.phieugiamgia.domain.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonPhieuGiamGia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing discount vouchers/coupons with enhanced audit trail.
 * Supports both percentage and fixed amount discounts.
 * Can be assigned to specific users (private vouchers) or made public.
 * Tracks usage limits and expiry dates.
 * Uses BaseAuditableEntity for basic audit fields and PhieuGiamGiaAuditHistory for detailed change tracking.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "phieu_giam_gia_id_gen")
    @SequenceGenerator(name = "phieu_giam_gia_id_gen", sequenceName = "phieu_giam_gia_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_phieu_giam_gia", nullable = false, length = 50, unique = true)
    private String maPhieuGiamGia;

    /**
     * Type of discount: percentage or fixed amount
     * Replaces the Boolean loaiPhieuGiamGia field with clear enum values
     */
    @Column(name = "loai_giam_gia", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private LoaiGiamGia loaiGiamGia = LoaiGiamGia.SO_TIEN_CO_DINH;

    /**
     * Campaign lifecycle status
     * Tracks whether the voucher campaign is active, expired, etc.
     */
    @Column(name = "trang_thai", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TrangThaiCampaign trangThai;
    @Column(name = "gia_tri_giam", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_don_hang_toi_thieu", precision = 15, scale = 2)
    private BigDecimal giaTriDonHangToiThieu;

    @Column(name = "ngay_bat_dau", nullable = false)
    private Instant ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private Instant ngayKetThuc;

    @Column(name = "mo_ta", length = Integer.MAX_VALUE)
    private String moTa;

    @Column(name = "so_luong_ban_dau", nullable = false)
    private Integer soLuongBanDau;

    @Column(name = "so_luong_da_dung")
    @Builder.Default
    private Integer soLuongDaDung = 0;



    // Bidirectional relationships
    @OneToMany(mappedBy = "phieuGiamGia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhieuGiamGiaNguoiDung> danhSachNguoiDung = new ArrayList<>();

    @OneToMany(mappedBy = "phieuGiamGia", fetch = FetchType.LAZY)
    @Builder.Default
    private List<HoaDonPhieuGiamGia> hoaDonPhieuGiamGias = new ArrayList<>();



    /**
     * Check if voucher campaign is currently active
     * @return true if campaign can be used and is not cancelled
     */
    public boolean isActive() {
        return trangThai != null && trangThai.isActive() && !trangThai.isCancelled() && hasRemainingUsage();
    }

    /**
     * Check if voucher has remaining usage
     * @return true if voucher can still be used
     */
    public boolean hasRemainingUsage() {
        return soLuongDaDung < soLuongBanDau;
    }

    /**
     * Get remaining usage count
     * @return number of times voucher can still be used
     */
    public int getRemainingUsage() {
        return Math.max(0, soLuongBanDau - soLuongDaDung);
    }

    /**
     * Increment usage count
     */
    public void incrementUsage() {
        if (hasRemainingUsage()) {
            this.soLuongDaDung++;
        } else {
            throw new IllegalStateException("Voucher has no remaining usage");
        }
    }

    /**
     * Check if this is a percentage discount
     * @return true if percentage discount, false if fixed amount
     */
    public boolean isPercentageDiscount() {
        return loaiGiamGia != null && loaiGiamGia.isPhanTram();
    }

    /**
     * Check if this is a fixed amount discount
     * @return true if fixed amount discount, false if percentage
     */
    public boolean isFixedAmountDiscount() {
        return loaiGiamGia != null && loaiGiamGia.isSoTienCoDinh();
    }

    /**
     * Check if this is a private voucher campaign
     * Private vouchers have user assignments in PhieuGiamGiaNguoiDung
     * @return true if private voucher, false if public
     */
    public boolean isPrivateVoucher() {
        return danhSachNguoiDung != null && !danhSachNguoiDung.isEmpty();
    }

    /**
     * Check if this is a public voucher campaign
     * Public vouchers have no user assignments
     * @return true if public voucher, false if private
     */
    public boolean isPublicVoucher() {
        return danhSachNguoiDung == null || danhSachNguoiDung.isEmpty();
    }

    /**
     * Check if a customer is eligible for this voucher
     * @param customerId the customer ID to check
     * @return true if customer is eligible
     */
    public boolean isCustomerEligible(Long customerId) {
        if (customerId == null) {
            return false;
        }

        // Public vouchers: available to all customers
        if (isPublicVoucher()) {
            return true;
        }

        // Private vouchers: check if customer has assignment
        return danhSachNguoiDung.stream()
            .anyMatch(assignment -> assignment.getNguoiDung().getId().equals(customerId));
    }

    /**
     * Check if a customer has used this voucher
     * @param customerId the customer ID to check
     * @return true if customer has used this voucher
     */
    public boolean hasCustomerUsedVoucher(Long customerId) {
        if (customerId == null || isPublicVoucher()) {
            return false;
        }

        return danhSachNguoiDung.stream()
            .anyMatch(assignment ->
                assignment.getNguoiDung().getId().equals(customerId) &&
                Boolean.TRUE.equals(assignment.getDaSuDung())
            );
    }

    /**
     * Calculate campaign status based on current date/time using Vietnam timezone
     * This method provides timezone-aware business logic for Vietnamese operations
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
     */
    public TrangThaiCampaign calculateStatusInVietnamTime() {
        return fromDatesWithTimezone(this.ngayBatDau, this.ngayKetThuc, ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    /**
     * Check if campaign should activate today in Vietnam timezone
     * Used by scheduler for precise activation timing
     */
    public boolean shouldActivateToday() {
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vietnamZone);
        LocalDate startDate = ngayBatDau.atZone(vietnamZone).toLocalDate();

        return startDate.equals(today) && this.trangThai == TrangThaiCampaign.CHUA_DIEN_RA;
    }

    /**
     * Check if campaign should expire today in Vietnam timezone
     * Used by scheduler for precise expiration timing
     */
    public boolean shouldExpireToday() {
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vietnamZone);
        LocalDate endDate = ngayKetThuc.atZone(vietnamZone).toLocalDate();

        return endDate.equals(today) && this.trangThai == TrangThaiCampaign.DA_DIEN_RA;
    }

    /**
     * Validate discount value based on discount type
     * @throws IllegalArgumentException if discount value is invalid
     */
    public void validateDiscountValue() {
        if (loaiGiamGia == null) {
            throw new IllegalArgumentException("Loại giảm giá không được để trống");
        }

        if (giaTriGiam == null || giaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0");
        }

        if (loaiGiamGia.isPhanTram()) {
            // Percentage discounts: 0 < value ≤ 100
            if (giaTriGiam.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Phần trăm giảm giá không được vượt quá 100%");
            }
            if (giaTriGiam.scale() > 2) {
                throw new IllegalArgumentException("Phần trăm giảm giá chỉ được có tối đa 2 chữ số thập phân");
            }
        } else {
            // Fixed amount discounts: value > 0, reasonable upper limit
            if (giaTriGiam.compareTo(BigDecimal.valueOf(100_000_000)) > 0) {
                throw new IllegalArgumentException("Số tiền giảm giá không được vượt quá 100,000,000 VND");
            }
        }
    }

    /**
     * Validate minimum order value
     * @throws IllegalArgumentException if minimum order value is invalid
     */
    public void validateMinimumOrderValue() {
        if (giaTriDonHangToiThieu != null) {
            if (giaTriDonHangToiThieu.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu không được âm");
            }

            // For fixed amount discounts, minimum order should be greater than discount
            if (loaiGiamGia != null && loaiGiamGia.isSoTienCoDinh() && giaTriGiam != null) {
                if (giaTriDonHangToiThieu.compareTo(giaTriGiam) <= 0) {
                    throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu phải lớn hơn số tiền giảm giá");
                }
            }
        }
    }

    /**
     * Validate voucher dates
     * @throws IllegalArgumentException if dates are invalid
     */
    public void validateDates() {
        if (ngayBatDau == null || ngayKetThuc == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }

        if (ngayBatDau.isAfter(ngayKetThuc)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc");
        }

        // Skip duration validation for cancelled vouchers (allows closing vouchers)
        if (trangThai == TrangThaiCampaign.BI_HUY) {
            return;
        }

        // Campaign should not be too short (at least 1 hour) - only for active campaigns
        if (ngayKetThuc.isBefore(ngayBatDau.plusSeconds(3600))) {
            throw new IllegalArgumentException("Chiến dịch phải kéo dài ít nhất 1 giờ");
        }
    }

    /**
     * Comprehensive validation before persist
     * @throws IllegalArgumentException if any validation fails
     */
    @PrePersist
    public void onPrePersist() {
        validateDiscountValue();
        validateMinimumOrderValue();
        validateDates();

        // Update status based on current date
        if (ngayBatDau != null && ngayKetThuc != null) {
            this.trangThai = fromDates(ngayBatDau, ngayKetThuc);
        }
    }

    /**
     * Comprehensive validation before update
     * @throws IllegalArgumentException if any validation fails
     */
    @PreUpdate
    public void onPreUpdate() {
        validateDiscountValue();
        validateMinimumOrderValue();
        validateDates();

        // Update status based on current date, but don't override manually set BI_HUY status
        if (ngayBatDau != null && ngayKetThuc != null && trangThai != TrangThaiCampaign.BI_HUY) {
            this.trangThai = fromDates(ngayBatDau, ngayKetThuc);
        }
    }
}
