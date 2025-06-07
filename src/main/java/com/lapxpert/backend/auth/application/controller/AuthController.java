package com.lapxpert.backend.auth.application.controller;

import com.lapxpert.backend.auth.application.dto.LoginRequest;
import com.lapxpert.backend.auth.domain.service.AuthService;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    private final NguoiDungRepository nguoiDungRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Kiểm tra đăng nhập
        String token = authService.dangNhap(
                loginRequest.getTaiKhoan(),
                loginRequest.getMatKhau());

        // Nếu không có token (đăng nhập thất bại)
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Đăng nhập thất bại"));
        }

        // Tìm người dùng bằng email
        Optional<NguoiDung> nguoiDung = nguoiDungRepo.findByEmail(loginRequest.getTaiKhoan());

        // Nếu không tìm thấy người dùng
        if (nguoiDung.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Người dùng không tồn tại"));
        }

        // Trả về thông tin người dùng và token
        NguoiDung user = nguoiDung.get();
        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "vaiTro", user.getVaiTro()
                )
        ));
    }
}
