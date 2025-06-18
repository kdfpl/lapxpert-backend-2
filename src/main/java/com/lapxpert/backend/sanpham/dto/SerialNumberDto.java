package com.lapxpert.backend.sanpham.dto;

import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link SerialNumber}
 */
@Data
public class SerialNumberDto implements Serializable {
    private Long id;

    @NotBlank(message = "Số serial không được để trống")
    @Size(max = 100, message = "Số serial không được vượt quá 100 ký tự")
    private String serialNumberValue;

    @NotNull(message = "ID biến thể sản phẩm không được để trống")
    private Long sanPhamChiTietId;

    // Product variant information for display
    private String tenSanPham;
    private String maSanPham;
    private String tenBienThe; // Variant name like "MacBook Air M2 - 8GB/256GB - Silver"

    @NotNull(message = "Trạng thái serial number không được để trống")
    private TrangThaiSerialNumber trangThai;

    private String trangThaiDisplay; // Human-readable status
    private String trangThaiSeverity; // UI severity level

    // Reservation information
    private Instant thoiGianDatTruoc;
    private String kenhDatTruoc;
    private String donHangDatTruoc;

    // Manufacturing and supplier information
    private String batchNumber;
    private Instant ngaySanXuat;
    private Instant ngayHetBaoHanh;
    private String nhaCungCap;
    private String importBatchId;
    private String ghiChu;

    // Audit information
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;
    private String nguoiCapNhat;

    // Computed fields
    private boolean isExpiredReservation;
    private boolean isWarrantyExpiringSoon;
    private long daysUntilWarrantyExpiry;

    // Helper methods for UI

    public String getTrangThaiDisplay() {
        return trangThai != null ? trangThai.getDescription() : "";
    }

    public String getTrangThaiSeverity() {
        return trangThai != null ? trangThai.getSeverity() : "secondary";
    }

    public boolean getIsExpiredReservation() {
        return trangThai == TrangThaiSerialNumber.RESERVED && 
               thoiGianDatTruoc != null && 
               Instant.now().isAfter(thoiGianDatTruoc.plusSeconds(900)); // 15 minutes
    }

    public boolean getIsWarrantyExpiringSoon() {
        if (ngayHetBaoHanh == null) return false;
        Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 60 * 60); // 30 days
        return ngayHetBaoHanh.isBefore(thirtyDaysFromNow);
    }

    public long getDaysUntilWarrantyExpiry() {
        if (ngayHetBaoHanh == null) return -1;
        return java.time.Duration.between(Instant.now(), ngayHetBaoHanh).toDays();
    }

    public boolean isAvailable() {
        return trangThai == TrangThaiSerialNumber.AVAILABLE;
    }

    public boolean isReserved() {
        return trangThai == TrangThaiSerialNumber.RESERVED;
    }

    public boolean isSold() {
        return trangThai == TrangThaiSerialNumber.SOLD;
    }

    public boolean canBeReserved() {
        return trangThai != null && trangThai.canBeReserved();
    }

    public boolean canBeSold() {
        return trangThai != null && trangThai.canBeSold();
    }

    public boolean canBeReturned() {
        return trangThai != null && trangThai.canBeReturned();
    }

    public String getDisplayName() {
        if (tenSanPham != null && serialNumberValue != null) {
            return tenSanPham + " - " + serialNumberValue;
        }
        return serialNumberValue != null ? serialNumberValue : "";
    }

    public String getStatusCategory() {
        return trangThai != null ? trangThai.getCategory() : "UNKNOWN";
    }
}
