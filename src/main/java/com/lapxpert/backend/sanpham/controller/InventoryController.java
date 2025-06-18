package com.lapxpert.backend.sanpham.controller;

import com.lapxpert.backend.sanpham.service.SerialNumberService;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for inventory management and monitoring
 * Updated to use SerialNumberService directly instead of InventoryService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final SerialNumberService serialNumberService;
    private final SerialNumberRepository serialNumberRepository;

    /**
     * Get inventory reservation statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getReservationStats() {
        try {
            // Get reservation statistics using SerialNumberRepository
            long totalReserved = serialNumberRepository.countByTrangThai(TrangThaiSerialNumber.RESERVED);
            long posReserved = serialNumberRepository.countByKenhDatTruoc("POS");
            long onlineReserved = serialNumberRepository.countByKenhDatTruoc("ONLINE");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalReserved", totalReserved);
            stats.put("posReserved", posReserved);
            stats.put("onlineReserved", onlineReserved);

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
            int available = serialNumberService.getAvailableQuantityByVariant(productVariantId);
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
            serialNumberService.cleanupExpiredReservations();
            return ResponseEntity.ok("Expired reservations cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during manual cleanup of expired reservations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error during cleanup: " + e.getMessage());
        }
    }
}
