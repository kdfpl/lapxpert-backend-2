package com.lapxpert.backend.hoadon.domain.entity;

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
public class HoaDonThanhToanId implements Serializable {
    private static final long serialVersionUID = 700320410035993192L;
    @Column(name = "hoa_don_id", nullable = false)
    private Long hoaDonId;

    @Column(name = "thanh_toan_id", nullable = false)
    private Long thanhToanId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        HoaDonThanhToanId entity = (HoaDonThanhToanId) o;
        return Objects.equals(this.hoaDonId, entity.hoaDonId) &&
                Objects.equals(this.thanhToanId, entity.thanhToanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hoaDonId, thanhToanId);
    }

}