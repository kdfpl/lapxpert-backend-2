package com.lapxpert.backend.phieugiamgia.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class PhieuGiamGiaNguoiDungId implements Serializable {
    private static final long serialVersionUID = 1824000244307698624L;

    @Column(name = "phieu_giam_gia_id", nullable = false)
    private Long phieuGiamGiaId;

    @Column(name = "nguoi_dung_id", nullable = false)
    private Long nguoiDungId;

    public PhieuGiamGiaNguoiDungId() {}

    public PhieuGiamGiaNguoiDungId(Long phieuGiamGiaId, Long nguoiDungId) {
        this.phieuGiamGiaId = phieuGiamGiaId;
        this.nguoiDungId = nguoiDungId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PhieuGiamGiaNguoiDungId entity = (PhieuGiamGiaNguoiDungId) o;
        return Objects.equals(this.phieuGiamGiaId, entity.phieuGiamGiaId) &&
                Objects.equals(this.nguoiDungId, entity.nguoiDungId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phieuGiamGiaId, nguoiDungId);
    }
}
