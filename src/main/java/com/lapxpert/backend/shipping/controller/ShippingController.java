package com.lapxpert.backend.shipping.controller;


import com.lapxpert.backend.shipping.service.GHNService;
import com.lapxpert.backend.shipping.service.GHNAddressService;
import com.lapxpert.backend.shipping.dto.ShippingRequest;
import com.lapxpert.backend.shipping.dto.ShippingFeeResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shipping Controller for GHN integration
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
    private final GHNAddressService ghnAddressService;

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
            config.put("defaultTransport", "road");

            // Pickup address configuration
            Map<String, String> pickupAddress = new HashMap<>();
            pickupAddress.put("province", "Hà Nội");
            pickupAddress.put("district", "Cầu Giấy");
            pickupAddress.put("ward", "Dịch Vọng");
            pickupAddress.put("address", "Số 1 Đại Cồ Việt");

            config.put("pickupAddress", pickupAddress);

            // Service availability - now based on GHN service
            config.put("serviceAvailable", ghnService.isAvailable());
            
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
     * Calculate shipping fee using GHN service directly
     * Returns shipping fee calculation result with backward-compatible format
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

            // Calculate shipping fee using GHN service directly
            ShippingFeeResponse ghnResponse = ghnService.calculateShippingFee(request);

            // Convert to backward-compatible format
            Map<String, Object> response = new HashMap<>();
            response.put("success", ghnResponse.isSuccess());
            response.put("fee", ghnResponse.isSuccess() ? ghnResponse.getTotalFee() : BigDecimal.ZERO);
            response.put("shippingFee", ghnResponse.getShippingFee());
            response.put("totalFee", ghnResponse.getTotalFee());
            response.put("provider", ghnResponse.getProviderName());
            response.put("serviceName", ghnResponse.getServiceName());
            response.put("estimatedDeliveryTime", ghnResponse.getEstimatedDeliveryTime());
            response.put("calculatedAt", ghnResponse.getCalculatedAt());
            response.put("isManualOverride", false);

            if (ghnResponse.isSuccess()) {
                response.put("message", "Shipping fee calculated successfully using GHN");
                log.info("Shipping fee calculated successfully: {} VND", ghnResponse.getTotalFee());
                return ResponseEntity.ok(response);
            } else {
                response.put("errorMessage", ghnResponse.getErrorMessage());
                response.put("errorCode", ghnResponse.getErrorCode());
                response.put("message", "Failed to calculate shipping fee");
                log.warn("Shipping fee calculation failed: {}", ghnResponse.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error in shipping fee calculation: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("fee", BigDecimal.ZERO);
            errorResponse.put("errorMessage", e.getMessage());
            errorResponse.put("isManualOverride", true);
            errorResponse.put("message", "Shipping fee calculation failed due to system error");

            return ResponseEntity.internalServerError().body(errorResponse);
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



    // ==================== GHN ADDRESS API ENDPOINTS ====================

    /**
     * Get all provinces from GHN API
     * Returns provinces in frontend-compatible format
     */
    @GetMapping("/ghn/provinces")
    public ResponseEntity<Map<String, Object>> getGHNProvinces() {
        try {
            log.info("Fetching GHN provinces for frontend");

            // Get all provinces from GHN address service
            List<Map<String, Object>> provinces = ghnAddressService.getAllProvinces();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", provinces);
            response.put("message", "GHN provinces loaded successfully");

            log.info("Successfully fetched {} GHN provinces", provinces.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching GHN provinces: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("message", "Failed to fetch GHN provinces: " + e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get districts for a specific province from GHN API
     * Returns districts in frontend-compatible format
     */
    @GetMapping("/ghn/districts/{provinceId}")
    public ResponseEntity<Map<String, Object>> getGHNDistricts(@PathVariable Integer provinceId) {
        try {
            log.info("Fetching GHN districts for province ID: {}", provinceId);

            // Validate province ID
            if (provinceId == null || provinceId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("data", Map.of("districts", new ArrayList<>()));
                errorResponse.put("message", "Invalid province ID");

                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Get districts from GHN address service
            List<Map<String, Object>> districts = ghnAddressService.getDistrictsForProvince(provinceId);

            Map<String, Object> data = new HashMap<>();
            data.put("districts", districts);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "GHN districts loaded successfully");

            log.info("Successfully fetched {} GHN districts for province {}", districts.size(), provinceId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching GHN districts for province {}: {}", provinceId, e.getMessage(), e);

            Map<String, Object> data = new HashMap<>();
            data.put("districts", new ArrayList<>());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", data);
            errorResponse.put("message", "Failed to fetch GHN districts: " + e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get wards for a specific district from GHN API
     * Returns wards in frontend-compatible format
     */
    @GetMapping("/ghn/wards/{districtId}")
    public ResponseEntity<Map<String, Object>> getGHNWards(@PathVariable Integer districtId) {
        try {
            log.info("Fetching GHN wards for district ID: {}", districtId);

            // Validate district ID
            if (districtId == null || districtId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("data", Map.of("wards", new ArrayList<>()));
                errorResponse.put("message", "Invalid district ID");

                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Get wards from GHN address service
            List<Map<String, Object>> wards = ghnAddressService.getWardsForDistrict(districtId);

            Map<String, Object> data = new HashMap<>();
            data.put("wards", wards);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("message", "GHN wards loaded successfully");

            log.info("Successfully fetched {} GHN wards for district {}", wards.size(), districtId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching GHN wards for district {}: {}", districtId, e.getMessage(), e);

            Map<String, Object> data = new HashMap<>();
            data.put("wards", new ArrayList<>());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("data", data);
            errorResponse.put("message", "Failed to fetch GHN wards: " + e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }
}
