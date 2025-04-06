package com.lapxpert.backend.phieugiamgia.application.controller;

import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.service.PhieuGiamGiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/phieu-giam-gia")
@CrossOrigin(origins = "*")
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService phieuGiamGiaService;

    public PhieuGiamGiaController(PhieuGiamGiaService phieuGiamGiaService) {
        this.phieuGiamGiaService = phieuGiamGiaService;
    }

    // ✅ Lấy tất cả phiếu giảm giá
    @GetMapping
    public ResponseEntity<List<PhieuGiamGia>> getAllPhieuGiamGia() {
        List<PhieuGiamGia> danhSach = phieuGiamGiaService.getAllPhieuGiamGia();
        return ResponseEntity.ok(danhSach);
    }

    // ✅ Lấy phiếu giảm giá theo ID
    @GetMapping("/{id}")
    public ResponseEntity<PhieuGiamGia> getPhieuGiamGiaById(@PathVariable Long id) {
        PhieuGiamGia phieuGiamGia = phieuGiamGiaService.getPhieuGiamGiaById(id);
        return ResponseEntity.ok(phieuGiamGia);
    }

    // ✅ Tạo phiếu giảm giá mới
    @PostMapping
    public ResponseEntity<PhieuGiamGia> createPhieuGiamGia(@RequestBody PhieuGiamGia phieuGiamGia) {
        PhieuGiamGia savedPhieuGiamGia = phieuGiamGiaService.createPhieuGiamGia(phieuGiamGia);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPhieuGiamGia);
    }

    // ✅ Cập nhật phiếu giảm giá
    @PutMapping("/{id}")
    public ResponseEntity<PhieuGiamGia> updatePhieuGiamGia(@PathVariable Long id, @RequestBody PhieuGiamGia phieuGiamGiaMoi) {
        PhieuGiamGia updatedPhieuGiamGia = phieuGiamGiaService.updatePhieuGiamGia(id, phieuGiamGiaMoi);
        return ResponseEntity.ok(updatedPhieuGiamGia);
    }

    // ✅ Đổi trạng thái phiếu giảm giá thành "Kết thúc"
    @PutMapping("/end/{id}")
    public ResponseEntity<String> endPhieuGiamGia(@PathVariable Long id) {
        try {
            phieuGiamGiaService.endPhieuGiamGia(id);
            return ResponseEntity.ok("Phiếu giảm giá đã được kết thúc.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        }
    }
}
