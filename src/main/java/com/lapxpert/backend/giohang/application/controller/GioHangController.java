package com.lapxpert.backend.giohang.application.controller;

import com.lapxpert.backend.giohang.application.dto.GioHangDto;
import com.lapxpert.backend.giohang.application.dto.GioHangChiTietDto;
import com.lapxpert.backend.giohang.application.dto.GioHangConversionDto;
import com.lapxpert.backend.giohang.application.dto.ThemSanPhamVaoGioRequest;
import com.lapxpert.backend.giohang.application.dto.CapNhatSoLuongRequest;
import com.lapxpert.backend.giohang.application.dto.CartToOrderRequestDto;
import com.lapxpert.backend.giohang.domain.service.GioHangService;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * REST Controller for GioHang (Shopping Cart) operations
 * Provides comprehensive cart management API endpoints
 * Follows established controller patterns with Vietnamese naming conventions
 */
@RestController
@RequestMapping("/api/v1/cart")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class GioHangController {

    private final GioHangService gioHangService;

    /**
     * Get current user's cart
     * @param currentUser authenticated user
     * @return user's cart with all items
     */
    @GetMapping
    public ResponseEntity<GioHangDto> getCurrentUserCart(@AuthenticationPrincipal NguoiDung currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Getting cart for user: {}", currentUser.getEmail());
        GioHangDto cart = gioHangService.getCartByUserId(currentUser.getId());
        return ResponseEntity.ok(cart);
    }

    /**
     * Get cart by user ID (admin only)
     * @param userId user ID
     * @return user's cart
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<GioHangDto> getCartByUserId(@PathVariable Long userId) {
        log.info("Getting cart for user ID: {}", userId);
        GioHangDto cart = gioHangService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Add product to cart
     * @param request add to cart request
     * @param currentUser authenticated user
     * @return updated cart
     */
    @PostMapping("/add")
    public ResponseEntity<GioHangDto> addProductToCart(
            @Valid @RequestBody ThemSanPhamVaoGioRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set user ID from authenticated user
        request.setNguoiDungId(currentUser.getId());

        log.info("Adding product {} to cart for user {}",
                request.getSanPhamChiTietId(), currentUser.getEmail());

        try {
            GioHangDto updatedCart = gioHangService.addProductToCart(request);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid add to cart request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error adding product to cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update item quantity in cart
     * @param request update quantity request
     * @param currentUser authenticated user
     * @return updated cart
     */
    @PutMapping("/update-quantity")
    public ResponseEntity<GioHangDto> updateItemQuantity(
            @Valid @RequestBody CapNhatSoLuongRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set user ID from authenticated user
        request.setNguoiDungId(currentUser.getId());

        log.info("Updating cart item quantity for user {} product {} to {}",
                currentUser.getEmail(), request.getSanPhamChiTietId(), request.getSoLuongMoi());

        try {
            GioHangDto updatedCart = gioHangService.updateItemQuantity(request);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid update quantity request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating cart item quantity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove item from cart
     * @param sanPhamChiTietId product variant ID to remove
     * @param currentUser authenticated user
     * @return updated cart
     */
    @DeleteMapping("/remove/{sanPhamChiTietId}")
    public ResponseEntity<GioHangDto> removeItemFromCart(
            @PathVariable Long sanPhamChiTietId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Removing item {} from cart for user {}", sanPhamChiTietId, currentUser.getEmail());

        try {
            GioHangDto updatedCart = gioHangService.removeItemFromCart(currentUser.getId(), sanPhamChiTietId);
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            log.error("Error removing item from cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear all items from cart
     * @param currentUser authenticated user
     * @return empty cart
     */
    @DeleteMapping("/clear")
    public ResponseEntity<GioHangDto> clearCart(@AuthenticationPrincipal NguoiDung currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Clearing cart for user {}", currentUser.getEmail());

        try {
            GioHangDto clearedCart = gioHangService.clearCart(currentUser.getId());
            return ResponseEntity.ok(clearedCart);
        } catch (Exception e) {
            log.error("Error clearing cart", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get cart items with price changes
     * @param currentUser authenticated user
     * @return list of items with price changes
     */
    @GetMapping("/price-changes")
    public ResponseEntity<List<GioHangChiTietDto>> getItemsWithPriceChanges(
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Getting items with price changes for user {}", currentUser.getEmail());

        try {
            List<GioHangChiTietDto> itemsWithPriceChanges =
                gioHangService.getItemsWithPriceChanges(currentUser.getId());
            return ResponseEntity.ok(itemsWithPriceChanges);
        } catch (Exception e) {
            log.error("Error getting items with price changes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Sync cart prices with current product prices
     * @param currentUser authenticated user
     * @return updated cart with current prices
     */
    @PostMapping("/sync-prices")
    public ResponseEntity<GioHangDto> syncCartPrices(@AuthenticationPrincipal NguoiDung currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Syncing cart prices for user {}", currentUser.getEmail());

        try {
            GioHangDto updatedCart = gioHangService.syncCartPrices(currentUser.getId());
            return ResponseEntity.ok(updatedCart);
        } catch (Exception e) {
            log.error("Error syncing cart prices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get cart summary (item count and total)
     * @param currentUser authenticated user
     * @return cart summary information
     */
    @GetMapping("/summary")
    public ResponseEntity<CartSummaryDto> getCartSummary(@AuthenticationPrincipal NguoiDung currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GioHangDto cart = gioHangService.getCartByUserId(currentUser.getId());
            CartSummaryDto summary = CartSummaryDto.builder()
                .itemCount(cart.getTongSoLuong() != null ? cart.getTongSoLuong() : 0)
                .uniqueProductCount(cart.getSoLuongSanPhamKhacNhau() != null ? cart.getSoLuongSanPhamKhacNhau() : 0)
                .totalAmount(cart.getTongTien() != null ? cart.getTongTien() : java.math.BigDecimal.ZERO)
                .hasItems(!cart.isEmpty())
                .hasPriceChanges(cart.isHasPriceChanges())
                .hasUnavailableItems(cart.isHasUnavailableItems())
                .build();

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting cart summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO for cart summary information
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CartSummaryDto {
        private Integer itemCount;
        private Integer uniqueProductCount;
        private java.math.BigDecimal totalAmount;
        private boolean hasItems;
        private boolean hasPriceChanges;
        private boolean hasUnavailableItems;
    }

    /**
     * Convert cart to order
     * @param request cart to order conversion request
     * @param currentUser authenticated user
     * @return created order
     */
    @PostMapping("/convert-to-order")
    public ResponseEntity<HoaDonDto> convertCartToOrder(
            @Valid @RequestBody CartToOrderRequestDto request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set user ID from authenticated user
        request.setNguoiDungId(currentUser.getId());

        log.info("Converting cart to order for user {}", currentUser.getEmail());

        try {
            HoaDonDto createdOrder = gioHangService.convertCartToOrder(request);
            log.info("Successfully converted cart to order {} for user {}",
                    createdOrder.getId(), currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cart to order conversion request for user {}: {}",
                    currentUser.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            log.warn("Entity not found during cart to order conversion for user {}: {}",
                    currentUser.getEmail(), e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error converting cart to order for user {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate cart for order conversion (preview)
     * @param request cart to order validation request
     * @param currentUser authenticated user
     * @return validation result
     */
    @PostMapping("/validate-for-order")
    public ResponseEntity<CartValidationResponseDto> validateCartForOrder(
            @Valid @RequestBody CartToOrderRequestDto request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set user ID from authenticated user
        request.setNguoiDungId(currentUser.getId());

        log.info("Validating cart for order conversion for user {}", currentUser.getEmail());

        try {
            // Use the new validation service method
            com.lapxpert.backend.giohang.domain.service.CartValidationResult validationResult =
                gioHangService.validateCartForOrderPreview(request);

            CartValidationResponseDto response = CartValidationResponseDto.builder()
                .isValid(validationResult.isValid())
                .message(validationResult.isValid() ? "Cart validation passed" : validationResult.getValidationSummary())
                .unavailableItemCount(validationResult.getUnavailableItemCount())
                .priceChangedItemCount(validationResult.getPriceChangedItemCount())
                .totalPriceDifference(validationResult.getTotalPriceDifference())
                .expectedTotal(validationResult.getCalculatedTotal())
                .providedTotal(validationResult.getProvidedTotal())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating cart for order conversion for user {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get cart conversion preview with detailed validation and pricing information
     * @param request cart to order conversion request
     * @param currentUser authenticated user
     * @return conversion preview with validation results
     */
    @PostMapping("/conversion-preview")
    public ResponseEntity<GioHangConversionDto> getCartConversionPreview(
            @Valid @RequestBody CartToOrderRequestDto request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set user ID from authenticated user
        request.setNguoiDungId(currentUser.getId());

        log.info("Getting cart conversion preview for user {}", currentUser.getEmail());

        try {
            GioHangConversionDto conversionPreview = gioHangService.getCartConversionPreview(request);
            return ResponseEntity.ok(conversionPreview);
        } catch (EntityNotFoundException e) {
            log.warn("Cart not found for user {}: {}", currentUser.getEmail(), e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting cart conversion preview for user {}", currentUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO for cart validation response
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CartValidationResponseDto {
        private boolean isValid;
        private String message;
        private Integer unavailableItemCount;
        private Integer priceChangedItemCount;
        private java.math.BigDecimal totalPriceDifference;
        private java.math.BigDecimal expectedTotal;
        private java.math.BigDecimal providedTotal;
    }
}
