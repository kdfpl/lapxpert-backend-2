package com.lapxpert.backend.dotgiamgia.dto;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.dotgiamgia.entity.DotGiamGia;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link DotGiamGia}
 * Uses TrangThaiCampaign enum and includes audit fields from AdminAuditableEntity
 * Enhanced with Bean Validation annotations for data integrity
 */
@Data
public class DotGiamGiaDto implements Serializable {
    private Long id;

    @NotBlank(message = "Mã đợt giảm giá không được để trống")
    @Size(max = 50, message = "Mã đợt giảm giá không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã đợt giảm giá chỉ được chứa chữ hoa, số, dấu gạch dưới và gạch ngang")
    private String maDotGiamGia;

    @NotBlank(message = "Tên đợt giảm giá không được để trống")
    @Size(max = 255, message = "Tên đợt giảm giá không được vượt quá 255 ký tự")
    private String tenDotGiamGia;

    @NotNull(message = "Phần trăm giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Phần trăm giảm phải từ 0.01%")
    @DecimalMax(value = "100.00", message = "Phần trăm giảm không được vượt quá 100%")
    @Digits(integer = 3, fraction = 2, message = "Phần trăm giảm phải có tối đa 3 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal phanTramGiam;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private Instant ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private Instant ngayKetThuc;

    private TrangThaiCampaign trangThai;

    // Vietnam timezone formatted strings for display (following PhieuGiamGia pattern)
    private String ngayBatDauVietnam;
    private String ngayKetThucVietnam;
    private String ngayTaoVietnam;
    private String ngayCapNhatVietnam;

    // Timezone metadata for frontend
    private String businessTimezone;

    // Audit fields from BaseAuditableEntity (read-only, no validation needed)
    private String nguoiTao;
    private String nguoiCapNhat;
    private Instant ngayTao;
    private Instant ngayCapNhat;

    // Audit reason field for tracking changes (following PhieuGiamGia pattern)
    @Size(max = 500, message = "Lý do thay đổi không được vượt quá 500 ký tự")
    private String lyDoThayDoi;

    /**
     * Custom validation method to ensure end date is after start date
     * @return true if dates are valid
     */
    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu và đợt giảm giá phải diễn ra ít nhất 1 giờ")
    public boolean isDateRangeValid() {
        if (ngayBatDau == null || ngayKetThuc == null) {
            return true; // Let @NotNull handle null validation
        }

        // End date must be after start date
        if (!ngayKetThuc.isAfter(ngayBatDau)) {
            return false;
        }

        // Campaign must run for at least 1 hour (3600 seconds)
        return ngayKetThuc.isAfter(ngayBatDau.plusSeconds(3600));
    }
}