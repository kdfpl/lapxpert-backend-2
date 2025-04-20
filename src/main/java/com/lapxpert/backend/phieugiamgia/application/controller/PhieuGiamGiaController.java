package com.lapxpert.backend.phieugiamgia.application.controller;

import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaRepository;
import com.lapxpert.backend.phieugiamgia.domain.service.PhieuGiamGiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("api/v1/phieu-giam-gia")
@CrossOrigin(origins = "*")
public class PhieuGiamGiaController {
    private final PhieuGiamGiaService phieuGiamGiaService;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public PhieuGiamGiaController(PhieuGiamGiaService phieuGiamGiaService,
                                  PhieuGiamGiaRepository phieuGiamGiaRepository) {
        this.phieuGiamGiaService = phieuGiamGiaService;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
    }

    @GetMapping()
    public List<PhieuGiamGiaDto> getAllPhieuGiamGia() {
        return phieuGiamGiaService.getAllPhieuGiamGia();
    }
    @PostMapping()
    public ResponseEntity<?> themPhieu(@RequestBody PhieuGiamGiaDto request) {
        phieuGiamGiaService.taoPhieu(request);
        return ResponseEntity.ok("Tạo phiếu thành công");
    }
    @PutMapping("/{id}")
    public ResponseEntity<String> capNhatPhieu(@PathVariable("id") Long phieuId,
                                                @RequestBody PhieuGiamGiaDto phieuGiamGiaDto) {
        try {
            phieuGiamGiaService.capNhatPhieu(phieuGiamGiaDto, phieuId);
            return ResponseEntity.ok("Cập nhật phiếu giảm giá thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getPhieuById(@PathVariable("id") Long id) {
        try {
            PhieuGiamGiaDto phieu = phieuGiamGiaService.getPhieuGiamGiaById(id);
            return ResponseEntity.ok(phieu);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy phiếu giảm giá với ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("delete/{id}")
    public ResponseEntity<String> closeVoucher(@PathVariable Long id) {
        try {
            phieuGiamGiaService.deletePhieuGiamGia(id);
            return ResponseEntity.ok("Phiếu giảm giá đã được tắt thành công.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi tắt phiếu giảm giá.");
        }
    }
}