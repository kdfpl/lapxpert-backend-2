package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "san_pham_chi_tiet")
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "san_pham_chi_tiet_id_gen")
    @SequenceGenerator(name = "san_pham_chi_tiet_id_gen", sequenceName = "san_pham_chi_tiet_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @ColumnDefault("0")
    @Column(name = "so_luong_ton_kho", nullable = false)
    private Integer soLuongTonKho;

    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "gia_khuyen_mai", precision = 15, scale = 2)
    private BigDecimal giaKhuyenMai;

    @Column(name = "hinh_anh")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> hinhAnh;

    @ColumnDefault("true")
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_tao")
    private OffsetDateTime ngayTao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_cap_nhat")
    private OffsetDateTime ngayCapNhat;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "cpu_id")
    private Cpu cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ram_id")
    private Ram ram;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "o_cung_id")
    private OCung oCung;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "gpu_id")
    private Gpu gpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "man_hinh_id")
    private ManHinh manHinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "cong_giao_tiep_id")
    private CongGiaoTiep congGiaoTiep;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ban_phim_id")
    private BanPhim banPhim;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "ket_noi_mang_id")
    private KetNoiMang ketNoiMang;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "am_thanh_id")
    private AmThanh amThanh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "webcam_id")
    private Webcam webcam;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "bao_mat_id")
    private BaoMat baoMat;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "he_dieu_hanh_id")
    private HeDieuHanh heDieuHanh;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "pin_id")
    private Pin pin;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "thiet_ke_id")
    private ThietKe thietKe;

    @ManyToMany
    @JoinTable(name = "san_pham_chi_tiet_dot_giam_gia",
            joinColumns = @JoinColumn(name = "san_pham_chi_tiet_id"),
            inverseJoinColumns = @JoinColumn(name = "dot_giam_gia_id"))
    private Set<DotGiamGia> dotGiamGias = new LinkedHashSet<>();

}