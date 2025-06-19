package com.lapxpert.backend.danhsachyeuthich.controller;

import com.lapxpert.backend.danhsachyeuthich.dto.DanhSachYeuThichDto;
import com.lapxpert.backend.danhsachyeuthich.dto.ThemVaoYeuThichRequest;
import com.lapxpert.backend.danhsachyeuthich.dto.ChuyenVaoGioHangRequest;
import com.lapxpert.backend.danhsachyeuthich.dto.ChuyenNhieuVaoGioHangRequest;
import com.lapxpert.backend.danhsachyeuthich.service.DanhSachYeuThichService;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for DanhSachYeuThich (Wishlist) operations
 * Provides comprehensive wishlist management endpoints
 * Supports both admin and customer operations with proper security
 */
@RestController
@RequestMapping("/api/v1/wishlist")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DanhSachYeuThichController {

    private final DanhSachYeuThichService danhSachYeuThichService;

    /**
     * Get current user's wishlist
     * @param currentUser authenticated user
     * @return list of wishlist items
     */
    @GetMapping
    public ResponseEntity<List<DanhSachYeuThichDto>> getDanhSachYeuThich(
            @AuthenticationPrincipal NguoiDung currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> wishlist = danhSachYeuThichService.getDanhSachYeuThichByNguoiDung(currentUser.getId());
            return ResponseEntity.ok(wishlist);
        } catch (Exception e) {
            log.error("Error getting wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get wishlist with pagination
     * @param currentUser authenticated user
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @param sortBy sort field (default: ngayThem)
     * @param sortDirection sort direction (default: desc)
     * @return paginated wishlist items
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<DanhSachYeuThichDto>> getDanhSachYeuThichPaginated(
            @AuthenticationPrincipal NguoiDung currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayThem") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Page<DanhSachYeuThichDto> wishlistPage = danhSachYeuThichService.getDanhSachYeuThichWithPagination(
                    currentUser.getId(), page, size, sortBy, sortDirection);
            return ResponseEntity.ok(wishlistPage);
        } catch (Exception e) {
            log.error("Error getting paginated wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add product to wishlist
     * @param request add to wishlist request
     * @param currentUser authenticated user
     * @return created wishlist item
     */
    @PostMapping("/add")
    public ResponseEntity<DanhSachYeuThichDto> themVaoYeuThich(
            @Valid @RequestBody ThemVaoYeuThichRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Ensure user can only add to their own wishlist
        if (!currentUser.getId().equals(request.getNguoiDungId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            DanhSachYeuThichDto createdItem = danhSachYeuThichService.themVaoYeuThich(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found when adding to wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request when adding to wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error adding to wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove product from wishlist
     * @param sanPhamId product ID to remove
     * @param currentUser authenticated user
     * @return success response
     */
    @DeleteMapping("/remove/{sanPhamId}")
    public ResponseEntity<Map<String, String>> xoaKhoiYeuThich(
            @PathVariable Long sanPhamId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            danhSachYeuThichService.xoaKhoiYeuThich(currentUser.getId(), sanPhamId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi danh sách yêu thích"));
        } catch (EntityNotFoundException e) {
            log.warn("Product not found in wishlist when removing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sản phẩm không có trong danh sách yêu thích"));
        } catch (Exception e) {
            log.error("Error removing from wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống"));
        }
    }

    /**
     * Clear entire wishlist
     * @param currentUser authenticated user
     * @return success response
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> xoaToanBoYeuThich(
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            danhSachYeuThichService.xoaToanBoYeuThich(currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ danh sách yêu thích"));
        } catch (Exception e) {
            log.error("Error clearing wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống"));
        }
    }

    /**
     * Check if product is in wishlist
     * @param sanPhamId product ID to check
     * @param currentUser authenticated user
     * @return boolean result
     */
    @GetMapping("/check/{sanPhamId}")
    public ResponseEntity<Map<String, Boolean>> kiemTraTrongYeuThich(
            @PathVariable Long sanPhamId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean isInWishlist = danhSachYeuThichService.kiemTraTrongYeuThich(currentUser.getId(), sanPhamId);
            return ResponseEntity.ok(Map.of("isInWishlist", isInWishlist));
        } catch (Exception e) {
            log.error("Error checking wishlist for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get wishlist count
     * @param currentUser authenticated user
     * @return count of wishlist items
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> demSoLuongYeuThich(
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            long count = danhSachYeuThichService.demSoLuongYeuThich(currentUser.getId());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Error getting wishlist count for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available products in wishlist
     * @param currentUser authenticated user
     * @return list of available wishlist items
     */
    @GetMapping("/available")
    public ResponseEntity<List<DanhSachYeuThichDto>> getSanPhamCoSanTrongYeuThich(
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> availableItems = danhSachYeuThichService.getSanPhamCoSanTrongYeuThich(currentUser.getId());
            return ResponseEntity.ok(availableItems);
        } catch (Exception e) {
            log.error("Error getting available wishlist items for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get products with price drops in wishlist
     * @param currentUser authenticated user
     * @return list of wishlist items with price drops
     */
    @GetMapping("/price-drops")
    public ResponseEntity<List<DanhSachYeuThichDto>> getSanPhamGiamGiaTrongYeuThich(
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> priceDropItems = danhSachYeuThichService.getSanPhamGiamGiaTrongYeuThich(currentUser.getId());
            return ResponseEntity.ok(priceDropItems);
        } catch (Exception e) {
            log.error("Error getting price drop items for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent wishlist items
     * @param currentUser authenticated user
     * @param limit maximum number of items (default: 5)
     * @return list of recent wishlist items
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DanhSachYeuThichDto>> getYeuThichGanDay(
            @AuthenticationPrincipal NguoiDung currentUser,
            @RequestParam(defaultValue = "5") int limit) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> recentItems = danhSachYeuThichService.getYeuThichGanDay(currentUser.getId(), limit);
            return ResponseEntity.ok(recentItems);
        } catch (Exception e) {
            log.error("Error getting recent wishlist items for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Move single item from wishlist to cart
     * @param request move to cart request
     * @param currentUser authenticated user
     * @return success response
     */
    @PostMapping("/move-to-cart")
    public ResponseEntity<Map<String, String>> chuyenVaoGioHang(
            @Valid @RequestBody ChuyenVaoGioHangRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Ensure user can only move their own wishlist items
        if (!currentUser.getId().equals(request.getNguoiDungId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String result = danhSachYeuThichService.chuyenVaoGioHang(request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found when moving to cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error moving to cart for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống"));
        }
    }

    /**
     * Move multiple items from wishlist to cart
     * @param request bulk move to cart request
     * @param currentUser authenticated user
     * @return result summary
     */
    @PostMapping("/move-multiple-to-cart")
    public ResponseEntity<Map<String, String>> chuyenNhieuVaoGioHang(
            @Valid @RequestBody ChuyenNhieuVaoGioHangRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Ensure user can only move their own wishlist items
        if (!currentUser.getId().equals(request.getNguoiDungId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String result = danhSachYeuThichService.chuyenNhieuVaoGioHang(request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            log.error("Error bulk moving to cart for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get wishlist items by category
     * @param categoryId category ID
     * @param currentUser authenticated user
     * @return list of wishlist items in the category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<DanhSachYeuThichDto>> getYeuThichTheoDanhMuc(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> categoryItems = danhSachYeuThichService.getYeuThichTheoDanhMuc(currentUser.getId(), categoryId);
            return ResponseEntity.ok(categoryItems);
        } catch (Exception e) {
            log.error("Error getting wishlist by category for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get wishlist items from recent days
     * @param daysBack number of days to look back (default: 7)
     * @param currentUser authenticated user
     * @return list of wishlist items from the period
     */
    @GetMapping("/recent-days")
    public ResponseEntity<List<DanhSachYeuThichDto>> getYeuThichTheoKhoangThoiGian(
            @RequestParam(defaultValue = "7") int daysBack,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<DanhSachYeuThichDto> dateRangeItems = danhSachYeuThichService.getYeuThichTheoKhoangThoiGian(currentUser.getId(), daysBack);
            return ResponseEntity.ok(dateRangeItems);
        } catch (Exception e) {
            log.error("Error getting wishlist by date range for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
