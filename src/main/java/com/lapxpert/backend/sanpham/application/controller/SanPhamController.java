package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.domain.service.SanPhamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/san-pham")
@CrossOrigin(origins = "*")
public class SanPhamController {
    private final SanPhamService sanPhamService;

    public SanPhamController(SanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }

    @GetMapping
    public ResponseEntity<List<SanPhamDto>> findAll() {
        return ResponseEntity.ok(sanPhamService.findAll());
    }
}
