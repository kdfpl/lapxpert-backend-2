package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "san_pham_danh_muc")
public class SanPhamDanhMuc {
    @SequenceGenerator(name = "san_pham_danh_muc_id_gen", sequenceName = "san_pham_chi_tiet_id_seq", allocationSize = 1)
    @EmbeddedId
    private SanPhamDanhMucId id;

    @MapsId("sanPhamId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @MapsId("danhMucId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

}