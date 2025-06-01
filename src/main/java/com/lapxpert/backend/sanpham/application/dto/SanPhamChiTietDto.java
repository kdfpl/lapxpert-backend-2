package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet}
 * Updated to align with entity structure using serialNumber and TrangThaiSanPham enum
 */
@Data
public class SanPhamChiTietDto implements Serializable {
    private Long id;

    /**
     * Unique serial number for individual item tracking
     * Renamed from 'sku' to align with entity structure
     */
    @NotBlank(message = "Số serial không được để trống")
    @Size(max = 100, message = "Số serial không được vượt quá 100 ký tự")
    private String serialNumber;

    private MauSac mauSac;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải lớn hơn hoặc bằng 0")
    @Digits(integer = 13, fraction = 2, message = "Giá bán không hợp lệ")
    private BigDecimal giaBan;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá khuyến mãi phải lớn hơn hoặc bằng 0")
    @Digits(integer = 13, fraction = 2, message = "Giá khuyến mãi không hợp lệ")
    private BigDecimal giaKhuyenMai;

    private List<String> hinhAnh;

    /**
     * Individual item status using enum for better type safety
     * Changed from Boolean to TrangThaiSanPham enum
     */
    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    private TrangThaiSanPham trangThai;

    private Instant ngayTao;
    private Instant ngayCapNhat;

    // Product attributes
    private Cpu cpu;
    private Ram ram;
    private OCung oCung;
    private Gpu gpu;
    private ManHinh manHinh;
    private CongGiaoTiep congGiaoTiep;
    private BanPhim banPhim;
    private KetNoiMang ketNoiMang;
    private AmThanh amThanh;
    private Webcam webcam;
    private BaoMat baoMat;
    private HeDieuHanh heDieuHanh;
    private ThietKe thietKe;
    private Pin pin;

    // Computed fields for business logic
    /**
     * Check if this item is available for purchase
     */
    public boolean isAvailable() {
        return trangThai == TrangThaiSanPham.AVAILABLE;
    }

    /**
     * Check if this item is reserved
     */
    public boolean isReserved() {
        return trangThai == TrangThaiSanPham.RESERVED;
    }

    /**
     * Check if this item has been sold
     */
    public boolean isSold() {
        return trangThai == TrangThaiSanPham.SOLD;
    }
}
