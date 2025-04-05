package com.lapxpert.backend.nguoidung.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.entity.GioiTinh;
import lombok.*;
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
    private String avatar;
    private String hoTen;
    private GioiTinh gioiTinh;
    private LocalDate ngaySinh;
    private String email;
    private String soDienThoai;
    private Boolean trangThai;
    private List<DiaChi> diaChis;
}
