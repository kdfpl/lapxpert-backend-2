package com.lapxpert.backend.hoadon.domain.entity;

import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "hoa_don_phieu_giam_gia")
public class HoaDonPhieuGiamGia {
    @EmbeddedId
    private HoaDonPhieuGiamGiaId id;

    @MapsId("hoaDonId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "hoa_don_id", nullable = false)
    private HoaDon hoaDon;

    @MapsId("phieuGiamGiaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "phieu_giam_gia_id", nullable = false)
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "gia_tri_da_giam", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTriDaGiam;

}