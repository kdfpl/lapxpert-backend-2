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
public class SanPhamDanhMucId implements Serializable {
    @Serial
    private static final long serialVersionUID = 2626755930074680159L;
    @Column(name = "san_pham_id", nullable = false)
    private Long sanPhamId;

    @Column(name = "danh_muc_id", nullable = false)
    private Long danhMucId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SanPhamDanhMucId entity = (SanPhamDanhMucId) o;
        return Objects.equals(this.sanPhamId, entity.sanPhamId) &&
                Objects.equals(this.danhMucId, entity.danhMucId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sanPhamId, danhMucId);
    }

}