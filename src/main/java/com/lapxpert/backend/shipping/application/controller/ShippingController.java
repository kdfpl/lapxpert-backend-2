package com.lapxpert.backend.shipping.application.controller;

import com.lapxpert.backend.ghtk.domain.GHTKConfig;
import com.lapxpert.backend.ghn.domain.GHNConfig;
import com.lapxpert.backend.ghn.domain.service.GHNService;
import com.lapxpert.backend.shipping.domain.service.ShippingProviderComparator;
import com.lapxpert.backend.shipping.domain.dto.ShippingRequest;
import com.lapxpert.backend.shipping.domain.dto.ShippingFeeResponse;
import com.lapxpert.backend.shipping.domain.dto.ProviderComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Shipping Controller for GHTK integration
 * Provides shipping configuration and basic endpoints for frontend integration
 * Follows Vietnamese business terminology and LapXpert patterns
 */
@RestController
@RequestMapping("/api/v1/shipping")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {

    private final GHNService ghnService;
    private final ShippingProviderComparator providerComparator;

    /**
     * Get shipping configuration for frontend
     * Returns default shipping settings and pickup address
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getShippingConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Default shipping settings
            config.put("defaultWeight", 500); // 500g default
            config.put("defaultTransport", GHTKConfig.ghtk_DefaultTransport != null ? 
                      GHTKConfig.ghtk_DefaultTransport : "road");
            
            // Pickup address configuration
            Map<String, String> pickupAddress = new HashMap<>();
            pickupAddress.put("province", GHTKConfig.ghtk_DefaultPickProvince != null ? 
                            GHTKConfig.ghtk_DefaultPickProvince : "Hà Nội");
            pickupAddress.put("district", GHTKConfig.ghtk_DefaultPickDistrict != null ? 
                            GHTKConfig.ghtk_DefaultPickDistrict : "Cầu Giấy");
            pickupAddress.put("ward", GHTKConfig.ghtk_DefaultPickWard != null ? 
                            GHTKConfig.ghtk_DefaultPickWard : "Dịch Vọng");
            pickupAddress.put("address", GHTKConfig.ghtk_DefaultPickAddress != null ? 
                            GHTKConfig.ghtk_DefaultPickAddress : "Số 1 Đại Cồ Việt");
            
            config.put("pickupAddress", pickupAddress);
            
            // Service availability
            config.put("serviceAvailable", GHTKConfig.ghtk_ApiToken != null && 
                      !GHTKConfig.ghtk_ApiToken.trim().isEmpty());
            
            log.info("Shipping configuration retrieved successfully");
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("Error retrieving shipping configuration: {}", e.getMessage(), e);
            
            // Return fallback configuration
            Map<String, Object> fallbackConfig = new HashMap<>();
            fallbackConfig.put("defaultWeight", 500);
            fallbackConfig.put("defaultTransport", "road");
            
            Map<String, String> fallbackPickupAddress = new HashMap<>();
            fallbackPickupAddress.put("province", "Hà Nội");
            fallbackPickupAddress.put("district", "Cầu Giấy");
            fallbackPickupAddress.put("ward", "Dịch Vọng");
            fallbackPickupAddress.put("address", "Số 1 Đại Cồ Việt");
            
            fallbackConfig.put("pickupAddress", fallbackPickupAddress);
            fallbackConfig.put("serviceAvailable", false);
            fallbackConfig.put("errorMessage", "Shipping service temporarily unavailable");
            
            return ResponseEntity.ok(fallbackConfig);
        }
    }

    /**
     * Calculate shipping fee (placeholder endpoint)
     * Returns fallback response since actual calculation is handled in HoaDonService
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateShippingFee(@RequestBody ShippingRequest request) {
        try {
            log.info("Shipping fee calculation requested for: {} -> {}", 
                    request.getPickProvince(), request.getProvince());
            
            // Validate request
            if (!request.isValid()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("fee", BigDecimal.ZERO);
                errorResponse.put("errorMessage", request.getValidationError());
                errorResponse.put("isManualOverride", true);
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Return placeholder response - actual calculation happens in HoaDonService
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("fee", BigDecimal.ZERO);
            response.put("isManualOverride", true);
            response.put("message", "Shipping fee calculation is handled during order creation. Please enter manually for now.");
            response.put("fallbackReason", "Standalone calculation not implemented");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in shipping fee calculation: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("fee", BigDecimal.ZERO);
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("isManualOverride", true);
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get available shipping services (placeholder endpoint)
     */
    @PostMapping("/services")
    public ResponseEntity<Map<String, Object>> getAvailableServices(@RequestBody Map<String, String> routeRequest) {
        try {
            log.info("Available services requested for route: {} -> {}", 
                    routeRequest.get("pickProvince"), routeRequest.get("province"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", new String[]{"standard"}); // Default service
            response.put("message", "Available services fetched successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching available services: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", new String[0]);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Validate delivery address (placeholder endpoint)
     */
    @PostMapping("/validate-address")
    public ResponseEntity<Map<String, Object>> validateAddress(@RequestBody Map<String, String> addressRequest) {
        try {
            log.info("Address validation requested for: {}, {}, {}", 
                    addressRequest.get("province"), 
                    addressRequest.get("district"), 
                    addressRequest.get("ward"));
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("isValid", true);
            data.put("suggestions", new String[0]);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "Address validated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error validating address: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("isValid", false);
            errorData.put("suggestions", new String[0]);
            errorData.put("errorMessage", e.getMessage());
            
            errorResponse.put("success", false);
            errorResponse.put("data", errorData);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get estimated delivery time (placeholder endpoint)
     */
    @PostMapping("/delivery-time")
    public ResponseEntity<Map<String, Object>> getEstimatedDeliveryTime(@RequestBody Map<String, String> deliveryRequest) {
        try {
            log.info("Delivery time estimation requested for: {} -> {}", 
                    deliveryRequest.get("pickProvince"), deliveryRequest.get("province"));
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("estimatedDays", 2); // Default 2 days
            data.put("estimatedTime", "2-3 ngày làm việc");
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "Delivery time estimated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error estimating delivery time: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("estimatedDays", 0);
            errorData.put("estimatedTime", "Không xác định");
            errorData.put("errorMessage", e.getMessage());
            
            errorResponse.put("success", false);
            errorResponse.put("data", errorData);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    // GHN-specific endpoints

    /**
     * Calculate shipping fee using GHN
     */
    @PostMapping("/ghn/calculate")
    public ResponseEntity<ShippingFeeResponse> calculateGHNShippingFee(@RequestBody ShippingRequest request) {
        try {
            log.info("GHN shipping fee calculation requested for: {} -> {}",
                    request.getPickProvince(), request.getProvince());

            ShippingFeeResponse response = ghnService.calculateShippingFee(request);

            if (response.isSuccess()) {
                log.info("GHN shipping fee calculated successfully: {} VND", response.getShippingFee());
                return ResponseEntity.ok(response);
            } else {
                log.warn("GHN shipping fee calculation failed: {}", response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error calculating GHN shipping fee: {}", e.getMessage(), e);
            ShippingFeeResponse errorResponse = ShippingFeeResponse.error(
                "CALCULATION_ERROR",
                "Failed to calculate GHN shipping fee: " + e.getMessage(),
                "GHN"
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get GHN service availability
     */
    @GetMapping("/ghn/availability")
    public ResponseEntity<Map<String, Object>> getGHNAvailability() {
        try {
            Map<String, Object> response = new HashMap<>();
            boolean isAvailable = ghnService.isAvailable();

            response.put("success", true);
            response.put("available", isAvailable);
            response.put("provider", "GHN");
            response.put("message", isAvailable ? "GHN service is available" : "GHN service is not configured");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking GHN availability: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("available", false);
            errorResponse.put("provider", "GHN");
            errorResponse.put("message", "Error checking GHN availability: " + e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Compare all available shipping providers and get the best option
     */
    @PostMapping("/compare")
    public ResponseEntity<ProviderComparisonResult> compareShippingProviders(@RequestBody ShippingRequest request) {
        try {
            log.info("Provider comparison requested for: {} -> {}",
                    request.getPickProvince(), request.getProvince());

            ProviderComparisonResult comparisonResult = providerComparator.compareProviders(request);

            if (comparisonResult.hasValidShippingOptions()) {
                log.info("Provider comparison successful. Selected: {} with fee: {} VND",
                    comparisonResult.getSelectedProviderName(), comparisonResult.getBestShippingFee());
                return ResponseEntity.ok(comparisonResult);
            } else {
                log.warn("Provider comparison failed: {}", comparisonResult.getSelectionReason());
                return ResponseEntity.ok(comparisonResult); // Return result even if no valid options
            }

        } catch (Exception e) {
            log.error("Error during provider comparison: {}", e.getMessage(), e);
            ProviderComparisonResult errorResult = ProviderComparisonResult.failed(
                "Provider comparison failed: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}
