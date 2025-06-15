package com.lapxpert.backend.nguoidung.application.dto;


import com.lapxpert.backend.nguoidung.domain.entity.GioiTinh;
import com.lapxpert.backend.nguoidung.domain.entity.TrangThaiNguoiDung;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHangDTO {
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

    @NotNull(message = "Trạng thái không được để trống")
    private TrangThaiNguoiDung trangThai;

    private List<DiaChiDto> diaChis;

    // Audit fields
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private String nguoiTao;
    private String nguoiCapNhat;
}
