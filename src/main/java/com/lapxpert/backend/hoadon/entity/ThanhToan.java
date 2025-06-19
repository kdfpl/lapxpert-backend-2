package com.lapxpert.backend.hoadon.entity;

import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.enums.TrangThaiGiaoDich;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "thanh_toan")
@EntityListeners(AuditingEntityListener.class)
public class ThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "thanh_toan_id_gen")
    @SequenceGenerator(name = "thanh_toan_id_gen", sequenceName = "thanh_toan_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "ma_giao_dich")
    private String maGiaoDich;

    @Column(name = "gia_tri", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTri;

    @Column(name = "ghi_chu", length = Integer.MAX_VALUE)
    private String ghiChu;

    @Column(name = "thoi_gian_thanh_toan")
    private Instant thoiGianThanhToan;

    @CreatedDate
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private Instant ngayTao;

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat", nullable = false)
    private Instant ngayCapNhat;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "trang_thai_giao_dich", nullable = false)
    private TrangThaiGiaoDich trangThaiGiaoDich;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;
}