package com.lapxpert.backend.danhsachyeuthich.domain.service;

import com.lapxpert.backend.danhsachyeuthich.application.dto.DanhSachYeuThichDto;
import com.lapxpert.backend.danhsachyeuthich.application.dto.ThemVaoYeuThichRequest;
import com.lapxpert.backend.danhsachyeuthich.application.dto.ChuyenVaoGioHangRequest;
import com.lapxpert.backend.danhsachyeuthich.application.dto.ChuyenNhieuVaoGioHangRequest;
import com.lapxpert.backend.danhsachyeuthich.application.mapper.DanhSachYeuThichMapper;
import com.lapxpert.backend.danhsachyeuthich.domain.entity.DanhSachYeuThich;
import com.lapxpert.backend.danhsachyeuthich.domain.repository.DanhSachYeuThichRepository;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import com.lapxpert.backend.giohang.domain.service.GioHangService;
import com.lapxpert.backend.giohang.application.dto.ThemSanPhamVaoGioRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core service for DanhSachYeuThich (Wishlist) module
 * Implements comprehensive wishlist management with business rules validation
 * Provides CRUD operations, cart integration, and analytics
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DanhSachYeuThichService {

    private final DanhSachYeuThichRepository danhSachYeuThichRepository;
    private final DanhSachYeuThichMapper danhSachYeuThichMapper;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final GioHangService gioHangService;

    /**
     * Get all wishlist items for a user
     * @param nguoiDungId the user ID
     * @return list of wishlist DTOs
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getDanhSachYeuThichByNguoiDung(Long nguoiDungId) {
        log.debug("Getting wishlist for user: {}", nguoiDungId);

        List<DanhSachYeuThich> wishlistItems = danhSachYeuThichRepository.findByNguoiDungId(nguoiDungId);
        return danhSachYeuThichMapper.toDtoList(wishlistItems);
    }

    /**
     * Get wishlist items with pagination
     * @param nguoiDungId the user ID
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy sort field
     * @param sortDirection sort direction
     * @return page of wishlist DTOs
     */
    @Transactional(readOnly = true)
    public Page<DanhSachYeuThichDto> getDanhSachYeuThichWithPagination(Long nguoiDungId, int page, int size,
                                                                       String sortBy, String sortDirection) {
        log.debug("Getting paginated wishlist for user: {}, page: {}, size: {}", nguoiDungId, page, size);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DanhSachYeuThich> wishlistPage = danhSachYeuThichRepository.findByNguoiDungId(nguoiDungId, pageable);
        return wishlistPage.map(danhSachYeuThichMapper::toDto);
    }

    /**
     * Add product to wishlist
     * @param request the add to wishlist request
     * @return created wishlist DTO
     */
    public DanhSachYeuThichDto themVaoYeuThich(ThemVaoYeuThichRequest request) {
        log.info("Adding product {} to wishlist for user {}", request.getSanPhamId(), request.getNguoiDungId());

        // Validate user exists
        NguoiDung nguoiDung = nguoiDungRepository.findById(request.getNguoiDungId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + request.getNguoiDungId()));

        // Validate product exists and is active
        SanPham sanPham = sanPhamRepository.findById(request.getSanPhamId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getSanPhamId()));

        // Check if product is active
        if (!Boolean.TRUE.equals(sanPham.getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm không còn hoạt động");
        }

        // Check if already in wishlist
        if (danhSachYeuThichRepository.existsByNguoiDungIdAndSanPhamId(request.getNguoiDungId(), request.getSanPhamId())) {
            throw new IllegalArgumentException("Sản phẩm đã có trong danh sách yêu thích");
        }

        // Validate product has available variants
        boolean hasAvailableVariants = sanPham.getSanPhamChiTiets() != null &&
                sanPham.getSanPhamChiTiets().stream()
                        .anyMatch(variant -> variant.getTrangThai() == com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.AVAILABLE);

        if (!hasAvailableVariants) {
            log.warn("Adding product {} to wishlist but no variants are available", request.getSanPhamId());
        }

        // Use provided price or calculate current minimum price for tracking
        java.math.BigDecimal priceForTracking = request.getGiaKhiThem();
        if (priceForTracking == null || priceForTracking.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            priceForTracking = sanPham.getSanPhamChiTiets().stream()
                    .filter(variant -> variant.getTrangThai() == com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.AVAILABLE)
                    .map(variant -> variant.getGiaBan())
                    .filter(price -> price != null)
                    .min(java.math.BigDecimal::compareTo)
                    .orElse(java.math.BigDecimal.ZERO);
        }

        // Create wishlist item with original price for tracking
        DanhSachYeuThich wishlistItem = DanhSachYeuThich.builder()
                .nguoiDung(nguoiDung)
                .sanPham(sanPham)
                .giaKhiThem(priceForTracking)
                .build();

        DanhSachYeuThich savedItem = danhSachYeuThichRepository.save(wishlistItem);
        log.info("Successfully added product {} to wishlist for user {}", request.getSanPhamId(), request.getNguoiDungId());

        return danhSachYeuThichMapper.toDto(savedItem);
    }

    /**
     * Remove product from wishlist
     * @param nguoiDungId the user ID
     * @param sanPhamId the product ID
     */
    public void xoaKhoiYeuThich(Long nguoiDungId, Long sanPhamId) {
        log.info("Removing product {} from wishlist for user {}", sanPhamId, nguoiDungId);

        DanhSachYeuThich wishlistItem = danhSachYeuThichRepository.findByNguoiDungIdAndSanPhamId(nguoiDungId, sanPhamId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm trong danh sách yêu thích"));

        danhSachYeuThichRepository.delete(wishlistItem);
        log.info("Successfully removed product {} from wishlist for user {}", sanPhamId, nguoiDungId);
    }

    /**
     * Clear entire wishlist for a user
     * @param nguoiDungId the user ID
     */
    public void xoaToanBoYeuThich(Long nguoiDungId) {
        log.info("Clearing entire wishlist for user {}", nguoiDungId);

        danhSachYeuThichRepository.deleteByNguoiDungId(nguoiDungId);
        log.info("Successfully cleared wishlist for user {}", nguoiDungId);
    }

    /**
     * Check if product is in user's wishlist
     * @param nguoiDungId the user ID
     * @param sanPhamId the product ID
     * @return true if product is in wishlist
     */
    @Transactional(readOnly = true)
    public boolean kiemTraTrongYeuThich(Long nguoiDungId, Long sanPhamId) {
        return danhSachYeuThichRepository.existsByNguoiDungIdAndSanPhamId(nguoiDungId, sanPhamId);
    }

    /**
     * Get wishlist count for a user
     * @param nguoiDungId the user ID
     * @return count of wishlist items
     */
    @Transactional(readOnly = true)
    public long demSoLuongYeuThich(Long nguoiDungId) {
        return danhSachYeuThichRepository.countByNguoiDungId(nguoiDungId);
    }

    /**
     * Get wishlist items with available products only
     * @param nguoiDungId the user ID
     * @return list of wishlist DTOs with available products
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getSanPhamCoSanTrongYeuThich(Long nguoiDungId) {
        log.debug("Getting available products in wishlist for user: {}", nguoiDungId);

        List<DanhSachYeuThich> availableItems = danhSachYeuThichRepository.findByNguoiDungIdAndAvailableProducts(nguoiDungId);
        return danhSachYeuThichMapper.toDtoList(availableItems);
    }

    /**
     * Get wishlist items with price drops
     * @param nguoiDungId the user ID
     * @return list of wishlist DTOs with price drops
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getSanPhamGiamGiaTrongYeuThich(Long nguoiDungId) {
        log.debug("Getting price drop items in wishlist for user: {}", nguoiDungId);

        List<DanhSachYeuThich> priceDropItems = danhSachYeuThichRepository.findItemsWithPriceDrops(nguoiDungId);
        return danhSachYeuThichMapper.toDtoList(priceDropItems);
    }

    /**
     * Get recent wishlist items
     * @param nguoiDungId the user ID
     * @param limit maximum number of items
     * @return list of recent wishlist DTOs
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getYeuThichGanDay(Long nguoiDungId, int limit) {
        log.debug("Getting recent wishlist items for user: {}, limit: {}", nguoiDungId, limit);

        Pageable pageable = PageRequest.of(0, limit);
        Page<DanhSachYeuThich> recentItems = danhSachYeuThichRepository.findRecentByNguoiDungId(nguoiDungId, pageable);
        return danhSachYeuThichMapper.toDtoList(recentItems.getContent());
    }

    /**
     * Move single item from wishlist to cart
     * @param request the move to cart request
     * @return success message
     */
    public String chuyenVaoGioHang(ChuyenVaoGioHangRequest request) {
        log.info("Moving product {} from wishlist to cart for user {}",
                request.getSanPhamId(), request.getNguoiDungId());

        // Verify item is in wishlist
        if (!danhSachYeuThichRepository.existsByNguoiDungIdAndSanPhamId(
                request.getNguoiDungId(), request.getSanPhamId())) {
            throw new EntityNotFoundException("Sản phẩm không có trong danh sách yêu thích");
        }

        // Add to cart
        ThemSanPhamVaoGioRequest cartRequest = ThemSanPhamVaoGioRequest.builder()
                .nguoiDungId(request.getNguoiDungId())
                .sanPhamChiTietId(request.getSanPhamChiTietId())
                .soLuong(request.getSoLuong())
                .build();

        gioHangService.addProductToCart(cartRequest);

        // Remove from wishlist
        xoaKhoiYeuThich(request.getNguoiDungId(), request.getSanPhamId());

        log.info("Successfully moved product {} from wishlist to cart for user {}",
                request.getSanPhamId(), request.getNguoiDungId());

        return "Đã chuyển sản phẩm vào giỏ hàng thành công";
    }

    /**
     * Move multiple items from wishlist to cart
     * @param request the bulk move to cart request
     * @return result summary
     */
    public String chuyenNhieuVaoGioHang(ChuyenNhieuVaoGioHangRequest request) {
        log.info("Moving {} products from wishlist to cart for user {}",
                request.getDanhSachSanPham().size(), request.getNguoiDungId());

        int successCount = 0;
        int failureCount = 0;

        for (var item : request.getDanhSachSanPham()) {
            try {
                ChuyenVaoGioHangRequest singleRequest = ChuyenVaoGioHangRequest.builder()
                        .nguoiDungId(request.getNguoiDungId())
                        .sanPhamId(item.getSanPhamId())
                        .sanPhamChiTietId(item.getSanPhamChiTietId())
                        .soLuong(item.getSoLuong())
                        .build();

                chuyenVaoGioHang(singleRequest);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to move product {} to cart: {}", item.getSanPhamId(), e.getMessage());
                failureCount++;

                if (!request.getSkipUnavailableItems()) {
                    throw new RuntimeException("Không thể chuyển sản phẩm ID " + item.getSanPhamId() +
                                             " vào giỏ hàng: " + e.getMessage());
                }
            }
        }

        log.info("Bulk move completed for user {}: {} success, {} failures",
                request.getNguoiDungId(), successCount, failureCount);

        return String.format("Đã chuyển %d sản phẩm vào giỏ hàng thành công. %d sản phẩm thất bại.",
                           successCount, failureCount);
    }

    /**
     * Get wishlist items by category
     * @param nguoiDungId the user ID
     * @param categoryId the category ID
     * @return list of wishlist DTOs in the category
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getYeuThichTheoDanhMuc(Long nguoiDungId, Long categoryId) {
        log.debug("Getting wishlist items by category {} for user: {}", categoryId, nguoiDungId);

        List<DanhSachYeuThich> categoryItems = danhSachYeuThichRepository.findByNguoiDungIdAndCategory(nguoiDungId, categoryId);
        return danhSachYeuThichMapper.toDtoList(categoryItems);
    }

    /**
     * Get wishlist items added in date range
     * @param nguoiDungId the user ID
     * @param daysBack number of days to look back
     * @return list of wishlist DTOs
     */
    @Transactional(readOnly = true)
    public List<DanhSachYeuThichDto> getYeuThichTheoKhoangThoiGian(Long nguoiDungId, int daysBack) {
        log.debug("Getting wishlist items from last {} days for user: {}", daysBack, nguoiDungId);

        Instant endDate = Instant.now();
        Instant startDate = endDate.minus(daysBack, ChronoUnit.DAYS);

        List<DanhSachYeuThich> dateRangeItems = danhSachYeuThichRepository.findByNguoiDungIdAndDateRange(
                nguoiDungId, startDate, endDate);
        return danhSachYeuThichMapper.toDtoList(dateRangeItems);
    }
}
