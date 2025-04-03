package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class SanPhamChiTietDotGiamGiaId implements Serializable {
    @Serial
    private static final long serialVersionUID = 9065899857899921613L;
    @Column(name = "san_pham_chi_tiet_id", nullable = false)
    private Long sanPhamChiTietId;

    @Column(name = "dot_giam_gia_id", nullable = false)
    private Long dotGiamGiaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SanPhamChiTietDotGiamGiaId entity = (SanPhamChiTietDotGiamGiaId) o;
        return Objects.equals(this.dotGiamGiaId, entity.dotGiamGiaId) &&
                Objects.equals(this.sanPhamChiTietId, entity.sanPhamChiTietId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dotGiamGiaId, sanPhamChiTietId);
    }

}