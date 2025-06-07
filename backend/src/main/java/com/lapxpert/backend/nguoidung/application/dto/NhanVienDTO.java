package com.lapxpert.backend.nguoidung.application.dto;


import com.lapxpert.backend.nguoidung.domain.entity.GioiTinh;
import com.lapxpert.backend.nguoidung.domain.entity.TrangThaiNguoiDung;
import lombok.*;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVienDTO {
    private Long id;
    private String maNguoiDung;

    @Size(max = 512, message = "URL avatar không được vượt quá 512 ký tự")
    private String avatar;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String hoTen;

    private GioiTinh gioiTinh;
    private LocalDate ngaySinh;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String soDienThoai;

    @NotBlank(message = "CCCD không được để trống cho nhân viên")
    @Size(max = 12, message = "CCCD không được vượt quá 12 ký tự")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD phải có từ 9-12 chữ số")
    private String cccd;

    @NotNull(message = "Vai trò không được để trống")
    private VaiTro vaiTro;

    @NotNull(message = "Trạng thái không được để trống")
    private TrangThaiNguoiDung trangThai;

    private List<DiaChiDto> diaChis;
}
