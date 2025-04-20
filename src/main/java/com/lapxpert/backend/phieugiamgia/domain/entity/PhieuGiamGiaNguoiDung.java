package com.lapxpert.backend.phieugiamgia.domain.entity;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
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

    @ManyToOne
    @MapsId("phieuGiamGiaId")
    @JoinColumn(name = "phieu_giam_gia_id")
    private PhieuGiamGia phieuGiamGia;

    @ManyToOne
    @MapsId("nguoiDungId")
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung;


}