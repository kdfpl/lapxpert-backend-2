package com.lapxpert.backend.auth.domain.service;

import com.lapxpert.backend.auth.domain.jwt.JwtUtil;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.entity.VaiTro;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final NguoiDungRepository nguoiDungRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String dangNhap(String taiKhoan, String matKhau) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepo
                .findByEmail(taiKhoan);

        NguoiDung nguoiDung = optionalNguoiDung
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hoặc vai trò không đúng."));

        if (!passwordEncoder.matches(matKhau, nguoiDung.getMatKhau())) {
            throw new RuntimeException("Mật khẩu không đúng.");
        }

        return jwtUtil.generateToken(nguoiDung);
    }
}
