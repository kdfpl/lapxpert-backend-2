package com.lapxpert.backend.nguoidung.application.controller;


import com.lapxpert.backend.nguoidung.application.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.application.dto.NhanVienDTO;
import com.lapxpert.backend.nguoidung.domain.service.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "*")
public class NguoiDungController {

    @Autowired
    private NguoiDungService nguoiDungService;

    public NguoiDungController(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @GetMapping("/customer")
    public ResponseEntity<List<KhachHangDTO>> getAllKhachHang() {
        return ResponseEntity.ok(nguoiDungService.getAllKhachHang());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<NhanVienDTO>> getAllNhanVien() {
        return ResponseEntity.ok(nguoiDungService.getAllNhanVien());
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<KhachHangDTO> getKhachHangByID(@PathVariable Long id) {
        return ResponseEntity.ok(nguoiDungService.getKhachHang(id));
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<NhanVienDTO> getNhanVienByID(@PathVariable Long id) {
        return ResponseEntity.ok(nguoiDungService.getNhanVien(id));
    }

    @PostMapping("/customer")
    public ResponseEntity<KhachHangDTO> addKhachHang(@RequestBody KhachHangDTO khachHangDTO) {
        KhachHangDTO createdKhachHang = nguoiDungService.addKhachHang(khachHangDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdKhachHang);
    }

    @PostMapping("/staff")
    public ResponseEntity<NhanVienDTO> addNhanVien(@RequestBody NhanVienDTO nhanVienDTO) {
        NhanVienDTO createdNhanVien = nguoiDungService.addNhanVien(nhanVienDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNhanVien);
    }

    @PutMapping("/customer/{id}")
    public ResponseEntity<KhachHangDTO> updateKhachHang(@PathVariable Long id, @RequestBody KhachHangDTO khachHangDTO) {
        KhachHangDTO updatedNguoiDung = nguoiDungService.updateKhachHang(id, khachHangDTO);
        return ResponseEntity.ok(updatedNguoiDung);
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<NhanVienDTO> updateNhanVien(@PathVariable Long id, @RequestBody NhanVienDTO nhanVienDTO) {
        NhanVienDTO updatedNguoiDung = nguoiDungService.updateNhanVien(id, nhanVienDTO);
        return ResponseEntity.ok(updatedNguoiDung);
    }

    @DeleteMapping("/customer/{id}")
    public ResponseEntity<Void> deleteKhachHang(@PathVariable Long id) {
        nguoiDungService.deleteKhachHang(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteNhanVien(@PathVariable Long id) {
        nguoiDungService.deleteNhanVien(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/customer/restore/{id}")
    public ResponseEntity<Void> restoreKhachHang(@PathVariable Long id) {
        nguoiDungService.restoreKhachHang(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/staff/restore/{id}")
    public ResponseEntity<Void> restoreNhanVien(@PathVariable Long id) {
        nguoiDungService.restoreNhanVien(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

