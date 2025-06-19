package com.lapxpert.backend.giohang.controller;

import com.lapxpert.backend.common.response.ApiResponse;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.giohang.dto.CartReservationRequest;
import com.lapxpert.backend.giohang.dto.CartReservationResponse;
import com.lapxpert.backend.giohang.dto.InventoryAvailabilityResponse;
import com.lapxpert.backend.sanpham.service.SerialNumberService;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for cart-level inventory reservations
 * Provides endpoints for managing serial number reservations during cart operations
 * Simplified to use SerialNumberService directly instead of CartReservationService
 */
@RestController
@RequestMapping("/api/v1/cart/reservations")
@RequiredArgsConstructor
@Slf4j
public class CartReservationController {

    private final SerialNumberService serialNumberService;
    private final SerialNumberRepository serialNumberRepository;

    private static final int CART_RESERVATION_TIMEOUT_MINUTES = 30;

    /**
     * Generate cart session ID for tracking reservations
     */
    private String generateCartSessionId(Long userId, String tabId) {
        return String.format("CART-%d-%s", userId, tabId);
    }
    
    /**
     * Reserve serial numbers for cart
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<CartReservationResponse>> reserveForCart(
            @Valid @RequestBody CartReservationRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Cart reservation request for user {} tab {} variant {} quantity {}",
                currentUser.getId(), request.getTabId(), request.getSanPhamChiTietId(), request.getSoLuong());

        String cartSessionId = generateCartSessionId(currentUser.getId(), request.getTabId());

        try {
            log.info("Reserving {} items of variant {} for cart session {}",
                    request.getSoLuong(), request.getSanPhamChiTietId(), cartSessionId);

            // Use existing reservation system with cart session ID as order ID
            List<SerialNumber> reservedSerialNumbers = serialNumberService.reserveSerialNumbers(
                request.getSanPhamChiTietId(),
                request.getSoLuong(),
                "CART", // channel
                cartSessionId, // orderId
                "user-" + currentUser.getId() // user
            );

            List<String> serialNumberValues = reservedSerialNumbers.stream()
                .map(SerialNumber::getSerialNumberValue)
                .collect(Collectors.toList());

            Instant now = Instant.now();
            Instant expiration = now.plus(CART_RESERVATION_TIMEOUT_MINUTES, ChronoUnit.MINUTES);

            CartReservationResponse response = CartReservationResponse.builder()
                .sanPhamChiTietId(request.getSanPhamChiTietId())
                .soLuongDatTruoc(reservedSerialNumbers.size())
                .serialNumbers(serialNumberValues)
                .cartSessionId(cartSessionId)
                .thoiGianDatTruoc(now)
                .thoiGianHetHan(expiration)
                .thanhCong(true)
                .thongBao("Đã đặt trước thành công")
                .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Failed to reserve items for cart: {}", e.getMessage());
            CartReservationResponse response = CartReservationResponse.builder()
                .sanPhamChiTietId(request.getSanPhamChiTietId())
                .soLuongDatTruoc(0)
                .cartSessionId(cartSessionId)
                .thanhCong(false)
                .thongBao("Không đủ hàng trong kho: " + e.getMessage())
                .build();
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getThongBao()));
        }
    }
    
    /**
     * Release all cart reservations for a specific tab
     */
    @DeleteMapping("/release/{tabId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> releaseCartReservations(
            @PathVariable String tabId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Releasing cart reservations for user {} tab {}", currentUser.getId(), tabId);

        String cartSessionId = generateCartSessionId(currentUser.getId(), tabId);

        try {
            log.info("Releasing cart reservations for session: {}", cartSessionId);

            // Find all reservations for this cart session
            List<SerialNumber> cartReservations = serialNumberRepository.findByDonHangDatTruoc(cartSessionId);

            if (!cartReservations.isEmpty()) {
                List<Long> serialNumberIds = cartReservations.stream()
                    .map(SerialNumber::getId)
                    .collect(Collectors.toList());

                serialNumberService.releaseReservations(
                    serialNumberIds,
                    "user-" + currentUser.getId(),
                    "Xóa khỏi giỏ hàng"
                );

                log.info("Released {} cart reservations for session {}", serialNumberIds.size(), cartSessionId);
            }

            return ResponseEntity.ok(ApiResponse.success("Đã hủy đặt trước thành công"));
        } catch (Exception e) {
            log.error("Error releasing cart reservations", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể hủy đặt trước: " + e.getMessage()));
        }
    }
    
    /**
     * Release specific items from cart
     */
    @DeleteMapping("/release/{tabId}/variant/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> releaseSpecificItems(
            @PathVariable String tabId,
            @PathVariable Long variantId,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Releasing {} items of variant {} from user {} tab {}",
                quantity, variantId, currentUser.getId(), tabId);

        String cartSessionId = generateCartSessionId(currentUser.getId(), tabId);

        try {
            log.info("Releasing {} items of variant {} from cart session {}",
                    quantity, variantId, cartSessionId);

            // Find reservations for this variant in this cart session
            List<SerialNumber> cartReservations = serialNumberRepository
                .findByDonHangDatTruocAndSanPhamChiTiet_Id(cartSessionId, variantId);

            if (cartReservations.size() >= quantity) {
                List<Long> toRelease = cartReservations.stream()
                    .limit(quantity)
                    .map(SerialNumber::getId)
                    .collect(Collectors.toList());

                serialNumberService.releaseReservations(
                    toRelease,
                    "user-" + currentUser.getId(),
                    "Giảm số lượng trong giỏ hàng"
                );

                log.info("Released {} specific items from cart session {}", toRelease.size(), cartSessionId);
            }

            return ResponseEntity.ok(ApiResponse.success("Đã hủy đặt trước thành công"));
        } catch (Exception e) {
            log.error("Error releasing specific cart items", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể hủy đặt trước: " + e.getMessage()));
        }
    }
    
    /**
     * Get real-time inventory availability for a variant
     */
    @GetMapping("/availability/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<InventoryAvailabilityResponse>> getInventoryAvailability(
            @PathVariable Long variantId,
            @AuthenticationPrincipal NguoiDung currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Get total counts
            int totalCount = (int) serialNumberRepository.countByVariantId(variantId);
            int availableCount = (int) serialNumberRepository.countAvailableByVariant(variantId);
            int reservedCount = (int) serialNumberRepository.countReservedByVariant(variantId);
            int soldCount = (int) serialNumberRepository.countSoldByVariant(variantId);

            // Get current user's cart reservations across all tabs
            String userCartPrefix = "CART-" + currentUser.getId() + "-";
            int userCartReservations = serialNumberRepository.countByDonHangDatTruocStartingWithAndSanPhamChiTiet_Id(
                userCartPrefix, variantId
            );

            InventoryAvailabilityResponse response = InventoryAvailabilityResponse.builder()
                .sanPhamChiTietId(variantId)
                .tongSoLuong(totalCount)
                .soLuongCoSan(availableCount)
                .soLuongDatTruoc(reservedCount)
                .soLuongDaBan(soldCount)
                .soLuongCartHienTai(userCartReservations)
                .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting inventory availability", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể lấy thông tin tồn kho: " + e.getMessage()));
        }
    }
}
