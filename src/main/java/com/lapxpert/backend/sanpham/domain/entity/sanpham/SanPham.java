package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "san_pham")
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "hinh_anh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> hinhAnh;

    @Column(name = "ngay_ra_mat")
    private LocalDate ngayRaMat;

    @ColumnDefault("true")
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = false;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_tao")
    @CreatedDate
    private Instant ngayTao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ngay_cap_nhat")
    @LastModifiedDate
    private Instant ngayCapNhat;

    @ManyToMany
    @JoinTable(name = "san_pham_danh_muc",
            joinColumns = @JoinColumn(name = "san_pham_id"),
            inverseJoinColumns = @JoinColumn(name = "danh_muc_id"))
    private Set<DanhMuc> danhMucs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    private Set<SanPhamChiTiet> sanPhamChiTiets = new LinkedHashSet<>();

}