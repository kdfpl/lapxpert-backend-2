package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "san_pham")
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "san_pham_id_gen")
    @SequenceGenerator(name = "san_pham_id_gen", sequenceName = "san_pham_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_san_pham", nullable = false, length = 100)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "thuong_hieu_id")
    private ThuongHieu thuongHieu;

    @Column(name = "mo_ta", length = Integer.MAX_VALUE)
    private String moTa;

    @Column(name = "hinh_anh")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> hinhAnh;

    @Column(name = "ngay_ra_mat")
    private LocalDate ngayRaMat;

    @ColumnDefault("true")
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_tao")
    private OffsetDateTime ngayTao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_cap_nhat")
    private OffsetDateTime ngayCapNhat;

}