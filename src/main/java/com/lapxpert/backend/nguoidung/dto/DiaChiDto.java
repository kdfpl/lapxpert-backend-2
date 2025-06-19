package com.lapxpert.backend.nguoidung.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * DTO for DiaChi entity representing user addresses
 * Enhanced with Bean Validation annotations for data integrity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChiDto {
    private Long id;

    // Note: nguoiDungId is set internally by the service layer during customer/staff creation
    // No validation needed as it's not provided by the frontend for new entities
    private Long nguoiDungId;

    @NotBlank(message = "Đường không được để trống")
    @Size(max = 255, message = "Đường không được vượt quá 255 ký tự")
    private String duong;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
    private String phuongXa;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String quanHuyen;

    @NotBlank(message = "Tỉnh/Thành không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành không được vượt quá 100 ký tự")
    private String tinhThanh;

    @Size(max = 100, message = "Quốc gia không được vượt quá 100 ký tự")
    @Builder.Default
    private String quocGia = "Việt Nam";

    @Size(max = 50, message = "Loại địa chỉ không được vượt quá 50 ký tự")
    private String loaiDiaChi;

    @Builder.Default
    private Boolean laMacDinh = false;

    // Audit fields (read-only, no validation needed)
    private ZonedDateTime ngayTao;
    private ZonedDateTime ngayCapNhat;
}
