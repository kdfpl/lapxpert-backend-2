package com.lapxpert.backend.phieugiamgia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia_nguoi_dung")
public class PhieuGiamGiaNguoiDung {
    @EmbeddedId
    private PhieuGiamGiaNguoiDungId id;

    @MapsId("phieuGiamGiaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "phieu_giam_gia_id", nullable = false)
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "ngay_nhan")
    private OffsetDateTime ngayNhan;

    @Column(name = "da_su_dung", nullable = false)
    private Boolean daSuDung = false;

}