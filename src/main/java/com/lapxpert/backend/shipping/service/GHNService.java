package com.lapxpert.backend.shipping.service;

import com.lapxpert.backend.shipping.config.GHNConfig;
import com.lapxpert.backend.shipping.dto.GHNShippingRequest;
import com.lapxpert.backend.shipping.dto.GHNShippingResponse;
import com.lapxpert.backend.shipping.dto.ShippingRequest;
import com.lapxpert.backend.shipping.dto.ShippingFeeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * GHN (GiaoHangNhanh) shipping service implementation
 * Provides shipping fee calculation using GHN API v2
 */
@Slf4j
@Service
public class GHNService extends ShippingCalculatorService {

    private final RestTemplate restTemplate;

    @Autowired
    private GHNAddressService ghnAddressService;

    public GHNService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public ShippingFeeResponse calculateShippingFee(ShippingRequest request) {
        log.info("Calculating shipping fee using GHN for request: {}", request);
        
        // Validate configuration
        if (!GHNConfig.validateConfiguration()) {
            log.error("GHN configuration is invalid");
            return createFallbackResponse("GHN service is not properly configured");
        }
        
        // Validate request
        if (!validateRequest(request)) {
            String error = getValidationError(request);
            log.error("Invalid shipping request: {}", error);
            return ShippingFeeResponse.error("INVALID_REQUEST", error, getProviderName());
        }
        
        try {
            // Fill in default values if not provided
            fillDefaultValues(request);
            
            // Build GHN API request
            GHNShippingRequest ghnRequest = buildGHNRequest(request);
            
            // Create headers
            HttpHeaders headers = createHeaders();
            
            // Make API call
            HttpEntity<GHNShippingRequest> entity = new HttpEntity<>(ghnRequest, headers);
            ResponseEntity<GHNShippingResponse> response = restTemplate.exchange(
                GHNConfig.getShippingFeeUrl(), HttpMethod.POST, entity, GHNShippingResponse.class
            );
            
            // Parse response
            return parseResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee using GHN: {}", e.getMessage(), e);
            return createFallbackResponse("GHN API call failed: " + e.getMessage());
        }
    }
    
    @Override
    public String getProviderName() {
        return "GHN";
    }
    
    @Override
    public boolean isAvailable() {
        return GHNConfig.validateConfiguration();
    }
    
    /**
     * Fill default values for GHN request
     */
    private void fillDefaultValues(ShippingRequest request) {
        if (request.getPickProvince() == null || request.getPickProvince().trim().isEmpty()) {
            // GHN uses district and ward codes instead of province names
            // We'll use the configured default values
        }
        
        if (request.getPickDistrict() == null || request.getPickDistrict().trim().isEmpty()) {
            // Will be handled in buildGHNRequest using default district ID
        }
        
        if (request.getTransport() == null || request.getTransport().trim().isEmpty()) {
            request.setTransport("road"); // Default transport method
        }
        
        if (request.getDeliverOption() == null || request.getDeliverOption().trim().isEmpty()) {
            request.setDeliverOption("standard"); // Default delivery option
        }
    }
    
    /**
     * Build GHN API request from generic shipping request
     */
    private GHNShippingRequest buildGHNRequest(ShippingRequest request) {
        // Resolve destination address using GHN Address Service
        Integer toProvinceId = ghnAddressService.findProvinceId(request.getProvince());
        Integer toDistrictId = null;
        String toWardCode = null;

        if (toProvinceId != null) {
            toDistrictId = ghnAddressService.findDistrictId(request.getDistrict(), toProvinceId);

            if (toDistrictId != null && request.getWard() != null && !request.getWard().trim().isEmpty()) {
                toWardCode = ghnAddressService.findWardCode(request.getWard(), toDistrictId);
            }
        }

        // Use fallback values if address resolution fails
        if (toDistrictId == null) {
            log.warn("Could not resolve destination district '{}' in province '{}', using fallback district ID",
                request.getDistrict(), request.getProvince());
            toDistrictId = 1; // Fallback district ID
        }

        if (toWardCode == null) {
            log.warn("Could not resolve destination ward '{}' in district '{}', using fallback ward code",
                request.getWard(), request.getDistrict());
            toWardCode = "1A0101"; // Fallback ward code
        }

        log.info("GHN address resolution: Province '{}' -> {}, District '{}' -> {}, Ward '{}' -> {}",
            request.getProvince(), toProvinceId, request.getDistrict(), toDistrictId,
            request.getWard(), toWardCode);

        return GHNShippingRequest.builder()
            .serviceId(Integer.parseInt(GHNConfig.ghn_DefaultServiceId))
            .serviceTypeId(Integer.parseInt(GHNConfig.ghn_DefaultServiceTypeId))
            .fromDistrictId(Integer.parseInt(GHNConfig.ghn_DefaultFromDistrictId))
            .fromWardCode(GHNConfig.ghn_DefaultFromWardCode)
            .toDistrictId(toDistrictId)
            .toWardCode(toWardCode)
            .weight(request.getWeight())
            .length(20) // Default package dimensions
            .width(15)
            .height(10)
            .insuranceValue(request.getValue())
            .items(Collections.singletonList(
                GHNShippingRequest.GHNItem.builder()
                    .name("Sản phẩm")
                    .quantity(1)
                    .weight(request.getWeight())
                    .length(20)
                    .width(15)
                    .height(10)
                    .build()
            ))
            .build();
    }
    
    /**
     * Create HTTP headers for GHN API
     * Updated to match GHN API documentation format
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", GHNConfig.ghn_ApiToken);  // lowercase as per GHN docs
        headers.set("ShopId", GHNConfig.ghn_ShopId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    /**
     * Parse GHN API response
     */
    private ShippingFeeResponse parseResponse(GHNShippingResponse response) {
        try {
            if (response == null) {
                return ShippingFeeResponse.error("NULL_RESPONSE", "GHN API returned null response", getProviderName());
            }
            
            if (!response.isSuccess()) {
                String errorMessage = response.getMessage() != null ? response.getMessage() : "Unknown GHN API error";
                return ShippingFeeResponse.error("GHN_API_ERROR", errorMessage, getProviderName());
            }
            
            if (response.getData() == null) {
                return ShippingFeeResponse.error("INVALID_RESPONSE", "Missing data in GHN response", getProviderName());
            }
            
            BigDecimal shippingFee = response.getShippingFee();
            BigDecimal insuranceFee = response.getInsuranceFee();
            
            ShippingFeeResponse result = ShippingFeeResponse.success(shippingFee, insuranceFee, "GHN Standard", getProviderName());
            result.setMessage(response.getMessage());
            
            log.info("Successfully calculated GHN shipping fee: {} VND", shippingFee);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse GHN response: {}", e.getMessage(), e);
            return ShippingFeeResponse.error("PARSE_ERROR", "Failed to parse GHN response: " + e.getMessage(), getProviderName());
        }
    }

    /**
     * Create fallback response when GHN service is unavailable
     */
    @Override
    protected ShippingFeeResponse createFallbackResponse(String reason) {
        log.warn("Creating fallback response for GHN: {}", reason);
        return ShippingFeeResponse.error("SERVICE_UNAVAILABLE", reason, getProviderName());
    }

    /**
     * Validate shipping request for GHN requirements
     */
    @Override
    protected boolean validateRequest(ShippingRequest request) {
        return request != null &&
               request.getWeight() != null && request.getWeight() > 0 &&
               request.getProvince() != null && !request.getProvince().trim().isEmpty() &&
               request.getDistrict() != null && !request.getDistrict().trim().isEmpty();
    }

    /**
     * Get validation error message
     */
    @Override
    protected String getValidationError(ShippingRequest request) {
        if (request == null) {
            return "Shipping request is null";
        }
        if (request.getWeight() == null || request.getWeight() <= 0) {
            return "Weight must be greater than 0";
        }
        if (request.getProvince() == null || request.getProvince().trim().isEmpty()) {
            return "Delivery province is required";
        }
        if (request.getDistrict() == null || request.getDistrict().trim().isEmpty()) {
            return "Delivery district is required";
        }
        return "Invalid request";
    }
}
