package com.lapxpert.backend.auth.application.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String taiKhoan;
    private String matKhau;
}

