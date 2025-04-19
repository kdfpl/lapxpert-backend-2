package com.lapxpert.backend.phieugiamgia.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_phieu_giam_gia", nullable = false, length = 50)
    private String maPhieuGiamGia;

    @Column(name = "loai_phieu_giam_gia")
    private Boolean loaiPhieuGiamGia;
    @Column(name = "trang_thai", nullable = false)
    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TrangThaiPhieuGiamGia trangThai;
    @Column(name = "gia_tri_giam", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_don_hang_toi_thieu", precision = 15, scale = 2)
    private BigDecimal giaTriDonHangToiThieu;

    @Column(name = "ngay_bat_dau", nullable = false)
    private OffsetDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private OffsetDateTime ngayKetThuc;

    @Column(name = "mo_ta", length = Integer.MAX_VALUE)
    private String moTa;

    @Column(name = "phieu_rieng_tu")
    private Boolean phieuRiengTu = false;

    @Column(name = "so_luong_ban_dau", nullable = false)
    private Integer soLuongBanDau;

    @Column(name = "so_luong_da_dung")
    private Integer soLuongDaDung = 0;

    @Column(name = "ngay_tao")
    private OffsetDateTime ngayTao;
    @Column(name = "ngay_cap_nhat")
    private OffsetDateTime ngayCapNhat;

    public enum TrangThaiPhieuGiamGia {
        CHUA_DIEN_RA,
        DA_DIEN_RA,
        KET_THUC
    }

    // Phương thức xác định trạng thái dựa trên ngày hiện tại
    public static TrangThaiPhieuGiamGia fromDates(OffsetDateTime ngayBatDau, OffsetDateTime ngayKetThuc) {
        if (ngayBatDau == null || ngayKetThuc == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không thể null.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(ngayBatDau)) {
            return TrangThaiPhieuGiamGia.CHUA_DIEN_RA;
        } else if (now.isAfter(ngayKetThuc)) {
            return TrangThaiPhieuGiamGia.KET_THUC;
        } else {
            return TrangThaiPhieuGiamGia.DA_DIEN_RA;
        }
    }
    @OneToMany(mappedBy = "phieuGiamGia")
    private List<PhieuGiamGiaNguoiDung> danhSachNguoiDung;
}
