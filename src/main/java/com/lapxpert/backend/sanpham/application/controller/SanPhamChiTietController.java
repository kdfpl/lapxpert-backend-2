package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.service.SanPhamChiTietService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products-details")
@CrossOrigin(origins = "*")
public class SanPhamChiTietController {
    private final SanPhamChiTietService sanPhamChiTietService;

    public SanPhamChiTietController(SanPhamChiTietService sanPhamChiTietService) {
        this.sanPhamChiTietService = sanPhamChiTietService;
    }

    // Lấy danh sách sản phẩm có trạng thái = true
    @GetMapping("/list")
    public List<SanPhamChiTietDto> getActiveProductsDetailed() {
        return sanPhamChiTietService.getActiveProducts();
    }

    // Thêm sản phẩm mới
    @PostMapping("/add")
    public ResponseEntity<SanPhamChiTiet> addProductDetailed(@RequestBody SanPhamChiTiet sanPham) {
        SanPhamChiTiet savedProduct = sanPhamChiTietService.addProduct(sanPham);
        return ResponseEntity.ok(savedProduct);
    }

    // Cập nhật sản phẩm
    @PutMapping("/update/{id}")
    public ResponseEntity<SanPhamChiTiet> updateProductDetailed(@PathVariable Long id, @RequestBody SanPhamChiTiet sanPham) {
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
