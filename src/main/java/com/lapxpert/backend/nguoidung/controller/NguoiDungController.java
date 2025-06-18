package com.lapxpert.backend.nguoidung.controller;

import com.lapxpert.backend.nguoidung.dto.KhachHangDTO;
import com.lapxpert.backend.nguoidung.dto.NhanVienDTO;
import com.lapxpert.backend.nguoidung.entity.NguoiDungAuditHistory;
import com.lapxpert.backend.nguoidung.service.NguoiDungService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<KhachHangDTO>> getAllKhachHang(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(nguoiDungService.searchKhachHang(search.trim()));
        }
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
    public ResponseEntity<KhachHangDTO> addKhachHang(@RequestBody @Valid KhachHangDTO khachHangDTO) {
        KhachHangDTO createdKhachHang = nguoiDungService.addKhachHang(khachHangDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdKhachHang);
    }

    @PostMapping("/staff")
    public ResponseEntity<NhanVienDTO> addNhanVien(@RequestBody @Valid NhanVienDTO nhanVienDTO) {
        NhanVienDTO createdNhanVien = nguoiDungService.addNhanVien(nhanVienDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNhanVien);
    }

    @PutMapping("/customer/{id}")
    public ResponseEntity<KhachHangDTO> updateKhachHang(@PathVariable Long id, @RequestBody @Valid KhachHangDTO khachHangDTO) {
        KhachHangDTO updatedNguoiDung = nguoiDungService.updateKhachHang(id, khachHangDTO);
        return ResponseEntity.ok(updatedNguoiDung);
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<NhanVienDTO> updateNhanVien(@PathVariable Long id, @RequestBody @Valid NhanVienDTO nhanVienDTO) {
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

    // Utility endpoints for frontend validation
    @GetMapping("/validate/email/{email}")
    public ResponseEntity<Boolean> isEmailAvailable(@PathVariable String email) {
        boolean isAvailable = nguoiDungService.isEmailAvailable(email);
        return ResponseEntity.ok(isAvailable);
    }

    @GetMapping("/validate/phone/{phone}")
    public ResponseEntity<Boolean> isPhoneAvailable(@PathVariable String phone) {
        boolean isAvailable = nguoiDungService.isPhoneAvailable(phone);
        return ResponseEntity.ok(isAvailable);
    }

    @GetMapping("/validate/cccd/{cccd}")
    public ResponseEntity<Boolean> isCccdAvailable(@PathVariable String cccd) {
        boolean isAvailable = nguoiDungService.isCccdAvailable(cccd);
        return ResponseEntity.ok(isAvailable);
    }



    // Audit history endpoints
    @GetMapping("/customer/{id}/audit-history")
    public ResponseEntity<List<NguoiDungAuditHistory>> getCustomerAuditHistory(@PathVariable Long id) {
        List<NguoiDungAuditHistory> auditHistory = nguoiDungService.getAuditHistory(id);
        return ResponseEntity.ok(auditHistory);
    }

    @GetMapping("/staff/{id}/audit-history")
    public ResponseEntity<List<NguoiDungAuditHistory>> getStaffAuditHistory(@PathVariable Long id) {
        List<NguoiDungAuditHistory> auditHistory = nguoiDungService.getAuditHistory(id);
        return ResponseEntity.ok(auditHistory);
    }
}

