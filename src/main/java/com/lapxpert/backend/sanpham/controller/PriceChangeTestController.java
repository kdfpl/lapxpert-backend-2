package com.lapxpert.backend.sanpham.controller;

import com.lapxpert.backend.sanpham.service.PriceChangeNotificationService;
import com.lapxpert.backend.sanpham.service.SanPhamChiTietService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for testing price change notifications.
 * Provides endpoints to test real-time price update functionality.
 */
@RestController
@RequestMapping("/api/v1/price-change-test")
@RequiredArgsConstructor
@Slf4j
public class PriceChangeTestController {

    private final PriceChangeNotificationService notificationService;
    private final SanPhamChiTietService sanPhamChiTietService;

    /**
     * Test endpoint to send price change notification
     */
    @PostMapping("/notify")
    public ResponseEntity<String> testPriceChangeNotification(
            @RequestParam Long variantId,
            @RequestParam String sku,
            @RequestParam String productName,
            @RequestParam Double oldPrice,
            @RequestParam Double newPrice) {
        
        try {
            notificationService.sendTestNotification(variantId, sku, productName, oldPrice, newPrice);
            
            String message = String.format(
                "Test price change notification sent for variant %d (%s): %,.0f₫ -> %,.0f₫", 
                variantId, sku, oldPrice, newPrice);
            
            log.info(message);
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            String errorMessage = "Failed to send test notification: " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test endpoint to update actual product price (triggers real notifications)
     */
    @PostMapping("/update-price/{variantId}")
    public ResponseEntity<String> updateProductPrice(
            @PathVariable Long variantId,
            @RequestParam BigDecimal newPrice,
            @RequestParam(required = false) BigDecimal newPromotionalPrice,
            @RequestParam(defaultValue = "Test price update") String reason) {
        
        try {
            sanPhamChiTietService.updatePriceWithAudit(
                variantId, newPrice, newPromotionalPrice, reason, "127.0.0.1", "Test-Agent");
            
            String message = String.format(
                "Price updated for variant %d: regular=%s, promotional=%s", 
                variantId, newPrice, newPromotionalPrice);
            
            log.info(message);
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            String errorMessage = "Failed to update price: " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test endpoint to simulate multiple price changes
     */
    @PostMapping("/simulate-changes")
    public ResponseEntity<String> simulateMultiplePriceChanges(
            @RequestParam Long variantId,
            @RequestParam String sku,
            @RequestParam String productName,
            @RequestParam(defaultValue = "5") int changeCount) {
        
        try {
            double basePrice = 1000000; // 1 million VND
            
            for (int i = 0; i < changeCount; i++) {
                double oldPrice = basePrice + (i * 100000);
                double newPrice = basePrice + ((i + 1) * 100000);
                
                notificationService.sendTestNotification(variantId, sku, productName, oldPrice, newPrice);
                
                // Small delay between notifications
                Thread.sleep(500);
            }
            
            String message = String.format(
                "Simulated %d price changes for variant %d (%s)", 
                changeCount, variantId, sku);
            
            log.info(message);
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            String errorMessage = "Failed to simulate price changes: " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Price change notification system is running");
    }
}
