package com.lapxpert.backend.hoadon.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * Order entity with enhanced audit trail for admin operations.
 * Extends AdminAuditableEntity to comply with enhanced audit requirements.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hoa_don", indexes = {
    @Index(name = "idx_hoa_don_ma", columnList = "ma_hoa_don"),
    @Index(name = "idx_hoa_don_khach_hang", columnList = "khach_hang_id"),
    @Index(name = "idx_hoa_don_trang_thai", columnList = "trang_thai_don_hang"),
    @Index(name = "idx_hoa_don_ngay_tao", columnList = "ngay_tao"),
    @Index(name = "idx_hoa_don_dia_chi", columnList = "dia_chi_giao_hang_id")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HoaDon extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hoa_don_id_gen")
    @SequenceGenerator(name = "hoa_don_id_gen", sequenceName = "hoa_don_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_hoa_don", nullable = false, unique = true)
    private String maHoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NguoiDung nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dia_chi_giao_hang_id")
    private DiaChi diaChiGiaoHang;

    // Delivery contact information (can be different from account holder)
    @Column(name = "nguoi_nhan_ten", length = 255)
    private String nguoiNhanTen;

    @Column(name = "nguoi_nhan_sdt", length = 20)
    private String nguoiNhanSdt;

    @Column(name = "tong_tien_hang", precision = 15, scale = 2)
    private BigDecimal tongTienHang;

    @Column(name = "gia_tri_giam_gia_voucher", precision = 15, scale = 2, nullable = false)
    private BigDecimal giaTriGiamGiaVoucher;

    @Column(name = "phi_van_chuyen", precision = 15, scale = 2, nullable = false)
    private BigDecimal phiVanChuyen;

    @Column(name = "tong_thanh_toan", precision = 15, scale = 2, nullable = false)
    private BigDecimal tongThanhToan;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "trang_thai_don_hang", nullable = false)
    private TrangThaiDonHang trangThaiDonHang;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "trang_thai_thanh_toan", nullable = false)
    private TrangThaiThanhToan trangThaiThanhToan;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "loai_hoa_don", nullable = false)
    private LoaiHoaDon loaiHoaDon;

    // Additional order-specific fields for enhanced tracking
    @Column(name = "ma_van_don", length = 100)
    private String maVanDon;

    @Column(name = "ngay_du_kien_giao_hang")
    private Instant ngayDuKienGiaoHang;

    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HoaDonChiTiet> hoaDonChiTiets = new ArrayList<>();

    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HoaDonPhieuGiamGia> hoaDonPhieuGiamGias = new ArrayList<>();
}