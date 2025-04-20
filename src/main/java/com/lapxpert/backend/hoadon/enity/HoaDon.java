package com.lapxpert.backend.hoadon.enity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hoa_don")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Tránh lỗi Lazy Loading
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hoa_don", nullable = false, unique = true)
    private String maHoaDon;

    @Column(name = "khach_hang_id", nullable = false)
    private Long khachHangId;

    @Column(name = "nhan_vien_id")
    private Long nhanVienId;

    @Column(name = "dia_chi_giao_hang_ho_ten")
    private String diaChiGiaoHangHoTen;

    @Column(name = "dia_chi_giao_hang_so_dien_thoai")
    private String diaChiGiaoHangSoDienThoai;

    @Column(name = "dia_chi_giao_hang_duong")
    private String diaChiGiaoHangDuong;

    @Column(name = "dia_chi_giao_hang_phuong_xa")
    private String diaChiGiaoHangPhuongXa;

    @Column(name = "dia_chi_giao_hang_quan_huyen")
    private String diaChiGiaoHangQuanHuyen;

    @Column(name = "dia_chi_giao_hang_tinh_thanh")
    private String diaChiGiaoHangTinhThanh;

    @Column(name = "gia_tri_san_pham", precision = 15, scale = 2, nullable = false)
    private BigDecimal giaTriSanPham;

    @Column(name = "gia_tri_giam_gia_voucher", precision = 15, scale = 2, nullable = false)
    private BigDecimal giaTriGiamGiaVoucher;

    @Column(name = "gia_tri_giam_gia_dot_giam", precision = 15, scale = 2, nullable = false)
    private BigDecimal giaTriGiamGiaDotGiam;

    @Column(name = "phi_van_chuyen", precision = 15, scale = 2, nullable = false)
    private BigDecimal phiVanChuyen;

    @Column(name = "tong_thanh_toan", precision = 15, scale = 2, nullable = false)
    private BigDecimal tongThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_giao_hang", nullable = false)
    private TrangThaiGiaoHang trangThaiGiaoHang;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    @Column(name = "loai_don_hang", nullable = false)
    private Boolean loaiDonHang;

    // Enum inside the entity class
    public enum TrangThaiGiaoHang {
        DANG_XU_LY,           // Đang xử lý
        CHO_XAC_NHAN,         // Chờ xác nhận
        DA_XAC_NHAN,          // Đã xác nhận
        DANG_DONG_GOI,        // Đang đóng gói
        DANG_GIAO_HANG,       // Đang giao hàng
        DA_GIAO_HANG,         // Đã giao hàng
        HOAN_THANH,           // Hoàn thành
        DA_HUY,               // Đã hủy
        YEU_CAU_TRA_HANG,     // Yêu cầu trả hàng
        DA_TRA_HANG           // Đã trả hàng
    }
}
