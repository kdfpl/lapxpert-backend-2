package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.application.mapper.SanPhamChiTietMapper;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Lấy danh sách sản phẩm có trạng thái AVAILABLE
    @Transactional(readOnly = true)
    public List<SanPhamChiTietDto> getActiveProducts() {
        List<SanPhamChiTiet> entities = sanPhamChiTietRepository.findAllAvailable();
        return sanPhamChiTietMapper.toDtos(entities);
    }

    // Lấy danh sách sản phẩm có trạng thái AVAILABLE với giá động
    @Transactional(readOnly = true)
    public List<SanPhamChiTietDto> getActiveProductsWithDynamicPricing() {
        List<SanPhamChiTiet> entities = sanPhamChiTietRepository.findAllAvailable();
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
    public SanPhamChiTiet updateProduct(Long id, SanPhamChiTiet sanPham) {
        return sanPhamChiTietRepository.findById(id).map(existing -> {
            existing.setSanPham(sanPham.getSanPham());
            existing.setSerialNumber(sanPham.getSerialNumber());
            existing.setMauSac(sanPham.getMauSac());
            existing.setGiaBan(sanPham.getGiaBan());
            existing.setGiaKhuyenMai(sanPham.getGiaKhuyenMai());
            existing.setHinhAnh(sanPham.getHinhAnh());
            existing.setTrangThai(sanPham.getTrangThai());
            existing.setCpu(sanPham.getCpu());
            existing.setRam(sanPham.getRam());
            existing.setOCung(sanPham.getOCung());
            existing.setGpu(sanPham.getGpu());
            existing.setManHinh(sanPham.getManHinh());
            existing.setCongGiaoTiep(sanPham.getCongGiaoTiep());
            existing.setBanPhim(sanPham.getBanPhim());
            existing.setKetNoiMang(sanPham.getKetNoiMang());
            existing.setAmThanh(sanPham.getAmThanh());
            existing.setWebcam(sanPham.getWebcam());
            existing.setBaoMat(sanPham.getBaoMat());
            existing.setHeDieuHanh(sanPham.getHeDieuHanh());
            existing.setPin(sanPham.getPin());
            existing.setThietKe(sanPham.getThietKe());
            return sanPhamChiTietRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));
    }

    // Xóa mềm sản phẩm (đặt trạng thái thành UNAVAILABLE)
    @Transactional
    public void softDeleteProduct(Long id) {
        sanPhamChiTietRepository.findById(id).ifPresent(sanPham -> {
            sanPham.setTrangThai(TrangThaiSanPham.UNAVAILABLE);
            sanPhamChiTietRepository.save(sanPham);
        });
    }

    // Cập nhật trạng thái sản phẩm chi tiết với audit trail
    @Transactional
    public SanPhamChiTiet updateStatusWithAudit(Long id, TrangThaiSanPham newStatus, String reason, String ipAddress, String userAgent) {
        SanPhamChiTiet existingProduct = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));

        // Update status
        existingProduct.setTrangThai(newStatus);

        // Note: Audit information is handled by SanPhamChiTietAuditHistory, not inline audit fields

        return sanPhamChiTietRepository.save(existingProduct);
    }

    // Cập nhật giá sản phẩm chi tiết với audit trail
    @Transactional
    public SanPhamChiTiet updatePriceWithAudit(Long id, BigDecimal newPrice, BigDecimal newPromotionalPrice, String reason, String ipAddress, String userAgent) {
        SanPhamChiTiet existingProduct = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm chi tiết không tồn tại"));

        // Update prices
        existingProduct.setGiaBan(newPrice);
        existingProduct.setGiaKhuyenMai(newPromotionalPrice);

        // Note: Audit information is handled by SanPhamChiTietAuditHistory, not inline audit fields

        return sanPhamChiTietRepository.save(existingProduct);
    }

}
