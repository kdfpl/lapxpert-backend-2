package com.lapxpert.backend.sanpham.application.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham}
 */
@Data
public class SanPhamDto implements Serializable {
    Long id;
    String maSanPham;
    String tenSanPham;
    ThuongHieuDto thuongHieu;
    List<String> hinhAnh;
    Set<DanhMucDto> danhMucs;
    Set<SanPhamChiTietDto> sanPhamChiTiets;
}