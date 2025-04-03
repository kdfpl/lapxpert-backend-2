package com.lapxpert.backend.sanpham.domain.entity.sanpham;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "san_pham_chi_tiet_dot_giam_gia")
public class SanPhamChiTietDotGiamGia {
    @SequenceGenerator(name = "san_pham_chi_tiet_dot_giam_gia_id_gen", sequenceName = "san_pham_chi_tiet_id_seq", allocationSize = 1)
    @EmbeddedId
    private SanPhamChiTietDotGiamGiaId id;

    @MapsId("sanPhamChiTietId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "san_pham_chi_tiet_id", nullable = false)
    private SanPhamChiTiet sanPhamChiTiet;

    @MapsId("dotGiamGiaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "dot_giam_gia_id", nullable = false)
    private DotGiamGia dotGiamGia;

}