package com.lapxpert.backend.nguoidung.dto;

import com.lapxpert.backend.nguoidung.entity.GioiTinh;
import com.lapxpert.backend.nguoidung.entity.TrangThaiNguoiDung;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Generic DTO for NguoiDung entity used by BusinessEntityService template.
 * Combines fields from both KhachHangDTO and NhanVienDTO to support all user types.
 * This DTO is used for template CRUD operations while role-specific DTOs
 * (KhachHangDTO, NhanVienDTO) are used for business-specific operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDungDto {
    private Long id;
    private String maNguoiDung;

    @Size(max = 512, message = "URL avatar không được vượt quá 512 ký tự")
    private String avatar;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String hoTen;

    private GioiTinh gioiTinh;
    private LocalDate ngaySinh;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String soDienThoai;

    // CCCD field - optional for customers, required for staff
    @Size(max = 12, message = "CCCD không được vượt quá 12 ký tự")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD phải có từ 9-12 chữ số")
    private String cccd;

    @NotNull(message = "Vai trò không được để trống")
    private VaiTro vaiTro;

    @NotNull(message = "Trạng thái không được để trống")
    private TrangThaiNguoiDung trangThai;

    private List<DiaChiDto> diaChis;

    // Audit fields
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;
    private String nguoiCapNhat;
}
