package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.DanhMucDto;
import com.lapxpert.backend.sanpham.application.dto.thuoctinh.ThuongHieuDto;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham}
 */
@Data
public class SanPhamDto implements Serializable {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private ThuongHieuDto thuongHieu;
    private String moTa;
    private List<String> hinhAnh;
    private LocalDate ngayRaMat;
    private Boolean trangThai;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private Set<DanhMucDto> danhMucs;
    private Set<SanPhamChiTietDto> sanPhamChiTiets;
}