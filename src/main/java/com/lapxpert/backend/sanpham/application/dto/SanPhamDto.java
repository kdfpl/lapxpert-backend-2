package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.DanhMucDto;
import com.lapxpert.backend.sanpham.application.dto.thuoctinh.ThuongHieuDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    // Product code is optional - will be auto-generated if null/empty
    @Size(max = 100, message = "Mã sản phẩm không được vượt quá 100 ký tự")
    private String maSanPham;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String tenSanPham;

    private ThuongHieuDto thuongHieu;

    @Size(max = 5000, message = "Mô tả sản phẩm không được vượt quá 5000 ký tự")
    private String moTa;

    private List<String> hinhAnh;
    private LocalDate ngayRaMat;

    @NotNull(message = "Trạng thái sản phẩm không được để trống")
    private Boolean trangThai;

    private Instant ngayTao;
    private Instant ngayCapNhat;
    private Set<DanhMucDto> danhMucs;
    private Set<SanPhamChiTietDto> sanPhamChiTiets;
}