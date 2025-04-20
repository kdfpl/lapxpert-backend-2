package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.service.SanPhamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SanPhamController {
    private final SanPhamService sanPhamService;

    @GetMapping
    public ResponseEntity<List<SanPhamDto>> findAll() {
        return ResponseEntity.ok(sanPhamService.findAll());
    }

    // Lấy danh sách sản phẩm có trạng thái = true
    @GetMapping("/list")
    public List<SanPhamDto> getActiveProducts() {
        return sanPhamService.getActiveProducts();
    }

    // Thêm sản phẩm mới
    @PostMapping("/add")
    public ResponseEntity<SanPham> addProduct(@RequestBody SanPham sanPham) {
        SanPham savedProduct = sanPhamService.addProduct(sanPham);
        return ResponseEntity.ok(savedProduct);
    }

    @PostMapping("/addMultiple")
    public ResponseEntity<SanPhamDto> addProducts(@RequestBody SanPhamDto sanPhamDto) {
        SanPhamDto savedProduct = sanPhamService.createSanPhamWithChiTiet(sanPhamDto);
        return ResponseEntity.ok(savedProduct);
    }

    // Cập nhật sản phẩm
    @PutMapping("/update/{id}")
    public ResponseEntity<SanPham> updateProduct(@PathVariable Long id, @RequestBody SanPham sanPham) {
        SanPham updatedProduct = sanPhamService.updateProduct(id, sanPham);
        return ResponseEntity.ok(updatedProduct);
    }

    // Xóa mềm sản phẩm
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> softDeleteProduct(@PathVariable Long id) {
        sanPhamService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

}
