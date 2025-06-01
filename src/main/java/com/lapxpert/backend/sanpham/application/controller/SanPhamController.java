package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.application.dto.BatchOperationResult;
import com.lapxpert.backend.sanpham.application.dto.BatchStatusUpdateRequest;
import com.lapxpert.backend.sanpham.application.dto.SanPhamAuditHistoryDto;
import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamAuditHistoryMapper;
import com.lapxpert.backend.sanpham.domain.entity.SanPhamAuditHistory;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.service.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
public class SanPhamController {
    private final SanPhamService sanPhamService;
    private final SanPhamAuditHistoryMapper auditHistoryMapper;

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
    public ResponseEntity<SanPhamDto> addProduct(@Valid @RequestBody SanPhamDto sanPhamDto) {
        SanPhamDto savedProduct = sanPhamService.createSanPhamWithChiTiet(sanPhamDto);
        return ResponseEntity.ok(savedProduct);
    }

    @PostMapping("/addMultiple")
    public ResponseEntity<SanPhamDto> addProducts(@Valid @RequestBody SanPhamDto sanPhamDto) {
        SanPhamDto savedProduct = sanPhamService.createSanPhamWithChiTiet(sanPhamDto);
        return ResponseEntity.ok(savedProduct);
    }

    // Cập nhật sản phẩm
    @PutMapping("/update/{id}")
    public ResponseEntity<SanPhamDto> updateProduct(@PathVariable Long id, @Valid @RequestBody SanPhamDto sanPhamDto) {
        SanPhamDto updatedProduct = sanPhamService.updateProductDto(id, sanPhamDto);
        return ResponseEntity.ok(updatedProduct);
    }

    // Cập nhật sản phẩm với biến thể
    @PutMapping("/updateWithVariants/{id}")
    public ResponseEntity<SanPhamDto> updateProductWithVariants(@PathVariable Long id, @Valid @RequestBody SanPhamDto sanPhamDto) {
        SanPhamDto updatedProduct = sanPhamService.updateProductWithVariants(id, sanPhamDto);
        return ResponseEntity.ok(updatedProduct);
    }

    // Xóa mềm sản phẩm
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> softDeleteProduct(@PathVariable Long id) {
        sanPhamService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Cập nhật trạng thái hàng loạt sản phẩm
    @PutMapping("/batch/status")
    public ResponseEntity<BatchOperationResult> updateMultipleProductStatus(
            @Valid @RequestBody BatchStatusUpdateRequest request) {
        try {
            BatchOperationResult result = sanPhamService.updateMultipleProductStatus(
                    request.getProductIds(),
                    request.getTrangThai(),
                    request.getLyDoThayDoi()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Return error result instead of throwing exception to avoid circular reference issues
            BatchOperationResult errorResult = new BatchOperationResult(
                    "BATCH_STATUS_UPDATE",
                    0,
                    request.getProductIds().size(),
                    "Lỗi cập nhật hàng loạt: " + e.getMessage()
            );
            return ResponseEntity.ok(errorResult);
        }
    }

    // Lấy lịch sử thay đổi sản phẩm
    @GetMapping("/{id}/audit-history")
    public ResponseEntity<List<SanPhamAuditHistoryDto>> getProductAuditHistory(@PathVariable Long id) {
        try {
            List<SanPhamAuditHistory> auditHistory = sanPhamService.getAuditHistory(id);
            List<SanPhamAuditHistoryDto> auditHistoryDtos = auditHistoryMapper.toDtos(auditHistory);
            return ResponseEntity.ok(auditHistoryDtos);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Tìm kiếm sản phẩm với bộ lọc
    @PostMapping("/search")
    public ResponseEntity<List<SanPhamDto>> searchProducts(@RequestBody(required = false) Map<String, Object> searchFilters) {
        try {
            List<SanPhamDto> products = sanPhamService.searchProducts(searchFilters);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.ok(sanPhamService.getActiveProducts()); // Fallback to active products
        }
    }

}
