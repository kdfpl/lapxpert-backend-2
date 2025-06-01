package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Product entity with enhanced audit trail for admin operations.
 * Uses BaseAuditableEntity for basic audit fields and SanPhamAuditHistory for detailed change tracking.
 */
@Getter
@Setter
@Entity
@Table(name = "san_pham")
public class SanPham extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "san_pham_id_gen")
    @SequenceGenerator(name = "san_pham_id_gen", sequenceName = "san_pham_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(max = 100, message = "Mã sản phẩm không được vượt quá 100 ký tự")
    @Column(name = "ma_san_pham", nullable = false, length = 100)
    private String maSanPham;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    @Column(name = "ten_san_pham", nullable = false)
    private String tenSanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "thuong_hieu_id")
    private ThuongHieu thuongHieu;

    @Size(max = 5000, message = "Mô tả sản phẩm không được vượt quá 5000 ký tự")
    @Column(name = "mo_ta", length = Integer.MAX_VALUE)
    private String moTa;

    @Column(name = "hinh_anh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> hinhAnh;

    @Column(name = "ngay_ra_mat")
    private LocalDate ngayRaMat;

    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    @ColumnDefault("true")
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @ManyToMany
    @JoinTable(name = "san_pham_danh_muc",
            joinColumns = @JoinColumn(name = "san_pham_id"),
            inverseJoinColumns = @JoinColumn(name = "danh_muc_id"))
    private Set<DanhMuc> danhMucs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY)
    private Set<SanPhamChiTiet> sanPhamChiTiets = new LinkedHashSet<>();

}