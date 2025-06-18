package com.lapxpert.backend.sanpham.controller;

import com.lapxpert.backend.sanpham.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.service.SanPhamChiTietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products-details")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
public class SanPhamChiTietController {
    private final SanPhamChiTietService sanPhamChiTietService;

    // Lấy danh sách sản phẩm có trạng thái = true
    @GetMapping("/list")
    public List<SanPhamChiTietDto> getActiveProductsDetailed() {
        return sanPhamChiTietService.getActiveProducts();
    }

    // Thêm sản phẩm mới
    @PostMapping("/add")
    public ResponseEntity<SanPhamChiTiet> addProductDetailed(@Valid @RequestBody SanPhamChiTiet sanPham) {
        SanPhamChiTiet savedProduct = sanPhamChiTietService.addProduct(sanPham);
        return ResponseEntity.ok(savedProduct);
    }

    // Cập nhật sản phẩm
    @PutMapping("/update/{id}")
    public ResponseEntity<SanPhamChiTiet> updateProductDetailed(@PathVariable Long id, @Valid @RequestBody SanPhamChiTiet sanPham) {
        SanPhamChiTiet updatedProduct = sanPhamChiTietService.updateProduct(id, sanPham);
        return ResponseEntity.ok(updatedProduct);
    }

    // Xóa mềm sản phẩm
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> softDeleteProductDetailed(@PathVariable Long id) {
        sanPhamChiTietService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
