package com.lapxpert.backend.hoadon.entity;

import com.lapxpert.backend.common.audit.BaseAuditableEntity;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

/**
 * Order line item entity with basic audit trail.
 * Uses BaseAuditableEntity for standard audit fields.
 */
@Getter
@Setter
@Entity
@Table(name = "hoa_don_chi_tiet", indexes = {
    @Index(name = "idx_hoa_don_chi_tiet_hoa_don", columnList = "hoa_don_id"),
    @Index(name = "idx_hoa_don_chi_tiet_san_pham", columnList = "san_pham_chi_tiet_id")
})
public class HoaDonChiTiet extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hoa_don_chi_tiet_id_gen")
    @SequenceGenerator(name = "hoa_don_chi_tiet_id_gen", sequenceName = "hoa_don_chi_tiet_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "hoa_don_id", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "san_pham_chi_tiet_id", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "gia_goc", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaGoc;

    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "thanh_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal thanhTien;

    @Column(name = "ten_san_pham_snapshot")
    private String tenSanPhamSnapshot;

    @Column(name = "sku_snapshot", length = 100)
    private String skuSnapshot;

    @Column(name = "hinh_anh_snapshot", length = 512)
    private String hinhAnhSnapshot;

    /**
     * Version field for optimistic locking to prevent race conditions
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}