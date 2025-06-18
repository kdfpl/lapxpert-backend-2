package com.lapxpert.backend.sanpham.service;

import com.lapxpert.backend.sanpham.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;

import com.lapxpert.backend.sanpham.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SanPhamChiTietService {
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamChiTietMapper sanPhamChiTietMapper;
    private final PricingService pricingService;
    private final SanPhamChiTietAuditService auditService;

    // Lấy danh sách sản phẩm có trạng thái AVAILABLE
    @Transactional(readOnly = true)
    public List<SanPhamChiTietDto> getActiveProducts() {
        List<SanPhamChiTiet> entities = sanPhamChiTietRepository.findAllActive();
        return sanPhamChiTietMapper.toDtos(entities);
    }

    // Lấy danh sách sản phẩm có trạng thái AVAILABLE với giá động
    @Transactional(readOnly = true)
    public List<SanPhamChiTietDto> getActiveProductsWithDynamicPricing() {
        List<SanPhamChiTiet> entities = sanPhamChiTietRepository.findAllActive();
        List<SanPhamChiTietDto> dtos = sanPhamChiTietMapper.toDtos(entities);

        // Apply dynamic pricing to each product
        for (int i = 0; i < entities.size(); i++) {
            SanPhamChiTiet entity = entities.get(i);
            SanPhamChiTietDto dto = dtos.get(i);

            // Calculate effective price using PricingService
            BigDecimal effectivePrice = pricingService.calculateEffectivePrice(entity);
            dto.setGiaKhuyenMai(effectivePrice);

            // Log if discount is applied
            if (effectivePrice.compareTo(entity.getGiaBan()) < 0) {
                log.debug("Applied discount to product {}: {} -> {}",
                    entity.getId(), entity.getGiaBan(), effectivePrice);
            }
        }

        return dtos;
    }

    // Lấy thông tin chi tiết giá cho một sản phẩm
    @Transactional(readOnly = true)
    public Optional<PricingService.PricingResult> getProductPricingDetails(Long productId) {
        return sanPhamChiTietRepository.findById(productId)
            .map(pricingService::calculateDetailedPrice);
    }

    // Kiểm tra xem sản phẩm có đang được giảm giá không
    @Transactional(readOnly = true)
    public boolean hasActiveDiscount(Long productId) {
        return sanPhamChiTietRepository.findById(productId)
            .map(pricingService::hasActiveDiscount)
            .orElse(false);
    }

    // Thêm sản phẩm mới
    @Transactional
    public SanPhamChiTiet addProduct(SanPhamChiTiet sanPham) {
        return sanPhamChiTietRepository.save(sanPham);
    }

    // Cập nhật sản phẩm
    @Transactional
    @CacheEvict(value = {"activeSanPhamList", "sanPhamList", "cartData"}, allEntries = true)
    public SanPhamChiTiet updateProduct(Long id, SanPhamChiTiet sanPham) {
        return sanPhamChiTietRepository.findById(id).map(existing -> {
            // Capture old prices for audit and notification
            BigDecimal oldPrice = existing.getGiaBan();
            BigDecimal oldPromotionalPrice = existing.getGiaKhuyenMai();

            existing.setSanPham(sanPham.getSanPham());
            existing.setSku(sanPham.getSku());
            existing.setGiaBan(sanPham.getGiaBan());
            existing.setGiaKhuyenMai(sanPham.getGiaKhuyenMai());
            existing.setHinhAnh(sanPham.getHinhAnh());
            existing.setTrangThai(sanPham.getTrangThai());

            // === 6 CORE ATTRIBUTES (as per requirements) ===
            existing.setMauSac(sanPham.getMauSac());
            existing.setCpu(sanPham.getCpu());
            existing.setRam(sanPham.getRam());
            existing.setBoNho(sanPham.getBoNho());
            existing.setGpu(sanPham.getGpu());
            existing.setManHinh(sanPham.getManHinh());

            SanPhamChiTiet savedProduct = sanPhamChiTietRepository.save(existing);

            // CRITICAL FIX: Trigger price change audit and real-time notifications if prices changed
            if ((oldPrice != null && !oldPrice.equals(sanPham.getGiaBan())) ||
                (oldPromotionalPrice != null && !oldPromotionalPrice.equals(sanPham.getGiaKhuyenMai())) ||
                (oldPrice == null && sanPham.getGiaBan() != null) ||
                (oldPromotionalPrice == null && sanPham.getGiaKhuyenMai() != null)) {

                auditService.logPriceChange(
                    id, oldPrice, sanPham.getGiaBan(), oldPromotionalPrice, sanPham.getGiaKhuyenMai(),
                    savedProduct.getNguoiCapNhat(), "Cập nhật giá qua ProductVariantManager", savedProduct
                );
            }

            return savedProduct;
        }).orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));
    }

    // Xóa mềm sản phẩm (đặt trạng thái thành inactive)
    @Transactional
    public void softDeleteProduct(Long id) {
        sanPhamChiTietRepository.findById(id).ifPresent(sanPham -> {
            sanPham.setTrangThai(false);
            sanPhamChiTietRepository.save(sanPham);
        });
    }

    // Cập nhật trạng thái sản phẩm chi tiết với audit trail
    @Transactional
    public SanPhamChiTiet updateStatusWithAudit(Long id, Boolean newStatus, String reason, String ipAddress, String userAgent) {
        SanPhamChiTiet existingProduct = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));

        // Update status
        existingProduct.setTrangThai(newStatus);

        // Note: Audit information is handled by SanPhamChiTietAuditHistory, not inline audit fields

        return sanPhamChiTietRepository.save(existingProduct);
    }

    // Cập nhật giá sản phẩm chi tiết với audit trail và thông báo real-time
    @Transactional
    public SanPhamChiTiet updatePriceWithAudit(Long id, BigDecimal newPrice, BigDecimal newPromotionalPrice, String reason, String ipAddress, String userAgent) {
        SanPhamChiTiet existingProduct = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));

        // Capture old prices for audit and notification
        BigDecimal oldPrice = existingProduct.getGiaBan();
        BigDecimal oldPromotionalPrice = existingProduct.getGiaKhuyenMai();

        // Update prices
        existingProduct.setGiaBan(newPrice);
        existingProduct.setGiaKhuyenMai(newPromotionalPrice);

        // Save the updated product
        SanPhamChiTiet savedProduct = sanPhamChiTietRepository.save(existingProduct);

        // Log price change with real-time notification (if prices actually changed)
        if ((oldPrice != null && !oldPrice.equals(newPrice)) ||
            (oldPromotionalPrice != null && !oldPromotionalPrice.equals(newPromotionalPrice)) ||
            (oldPrice == null && newPrice != null) ||
            (oldPromotionalPrice == null && newPromotionalPrice != null)) {

            auditService.logPriceChange(
                id, oldPrice, newPrice, oldPromotionalPrice, newPromotionalPrice,
                savedProduct.getNguoiCapNhat(), reason, savedProduct
            );
        }

        return savedProduct;
    }

}
