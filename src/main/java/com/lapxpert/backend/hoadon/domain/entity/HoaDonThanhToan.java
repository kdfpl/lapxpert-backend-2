package com.lapxpert.backend.hoadon.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "hoa_don_thanh_toan")
@EntityListeners(AuditingEntityListener.class)
public class HoaDonThanhToan {
    @EmbeddedId
    private HoaDonThanhToanId id;

    @MapsId("hoaDonId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "hoa_don_id", nullable = false)
    private HoaDon hoaDon;

    @MapsId("thanhToanId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "thanh_toan_id", nullable = false)
    private ThanhToan thanhToan;

    @ColumnDefault("0")
    @Column(name = "so_tien_ap_dung", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTienApDung;

    @CreatedDate
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private Instant ngayTao;

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat", nullable = false)
    private Instant ngayCapNhat;
}