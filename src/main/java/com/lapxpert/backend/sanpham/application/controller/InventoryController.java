package com.lapxpert.backend.sanpham.application.controller;

import com.lapxpert.backend.sanpham.domain.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for inventory management and monitoring
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Get inventory reservation statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<InventoryService.ReservationStats> getReservationStats() {
        try {
            InventoryService.ReservationStats stats = inventoryService.getReservationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting reservation statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check available quantity for a product variant
     */
    @GetMapping("/available/{productVariantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Integer> getAvailableQuantity(@PathVariable Long productVariantId) {
        try {
            int available = inventoryService.getAvailableQuantity(productVariantId);
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            log.error("Error getting available quantity for product variant {}: {}", productVariantId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Manually trigger cleanup of expired reservations (for admin use)
     */
    @PostMapping("/cleanup-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> cleanupExpiredReservations() {
        try {
            inventoryService.cleanupExpiredReservations();
            return ResponseEntity.ok("Expired reservations cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during manual cleanup of expired reservations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error during cleanup: " + e.getMessage());
        }
    }
}
