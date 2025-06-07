package com.lapxpert.backend.hoadon.domain.dto;

import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.nguoidung.application.dto.DiaChiDto;
import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.application.dto.NhanVienDTO;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class HoaDonDto {
    private Long id;
    private String maHoaDon;
    private Long khachHangId;
    private Long nhanVienId;

    // Full customer and employee objects for frontend display
    private KhachHangDTO khachHang;
    private NhanVienDTO nhanVien;

    // Delivery address - can be provided as ID or full object
    private Long diaChiGiaoHangId;
    private DiaChiDto diaChiGiaoHang;

    // Delivery contact information (can be different from account holder)
    private String nguoiNhanTen;
    private String nguoiNhanSdt;

    private BigDecimal tongTienHang;
    private BigDecimal giaTriGiamGiaVoucher;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongThanhToan;
    private TrangThaiDonHang trangThaiDonHang;
    private TrangThaiThanhToan trangThaiThanhToan;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private LoaiHoaDon loaiHoaDon;
    // private List<LichSuHoaDonDto> lichSuHoaDons; // Removed as LichSuHoaDon is typically handled separately
    private List<HoaDonChiTietDto> chiTiet;

    // Voucher fields for order creation
    private List<String> voucherCodes; // List of voucher codes to apply
}
