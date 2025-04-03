package com.lapxpert.backend.dotgiamgia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "dot_giam_gia")
@EntityListeners(AuditingEntityListener.class)
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

}