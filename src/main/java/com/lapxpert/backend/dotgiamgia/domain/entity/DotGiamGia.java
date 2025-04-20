package com.lapxpert.backend.dotgiamgia.domain.entity;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "dot_giam_gia")
public class DotGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dot_giam_gia_id_gen")
    @SequenceGenerator(name = "dot_giam_gia_id_gen", sequenceName = "dot_giam_gia_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ma_dot_giam_gia", nullable = false, length = 50)
    private String maDotGiamGia;

    @Column(name = "ten_dot_giam_gia", nullable = false)
    private String tenDotGiamGia;

    @Column(name = "phan_tram_giam", nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    @Column(name = "ngay_bat_dau", nullable = false)
    private Instant ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private Instant ngayKetThuc;

    @ColumnDefault("true")
    @Column(name = "da_an", nullable = false)
    private Boolean daAn = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_tao")
    @CreatedDate
    private Instant ngayTao;
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_cap_nhat")
    @LastModifiedDate
    private Instant ngayCapNhat;

    @ColumnDefault("'CHUA_DIEN_RA'")
    @Column(name = "trang_thai")
    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TrangThai trangThai;


    @ManyToMany(mappedBy = "dotGiamGias")
    private Set<SanPhamChiTiet> sanPhamChiTiets = new LinkedHashSet<>();

}