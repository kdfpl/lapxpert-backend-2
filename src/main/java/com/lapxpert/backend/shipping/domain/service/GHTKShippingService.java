package com.lapxpert.backend.shipping.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.ghtk.domain.GHTKConfig;
import com.lapxpert.backend.shipping.domain.dto.ShippingRequest;
import com.lapxpert.backend.shipping.domain.dto.ShippingFeeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

/**
 * GHTK (Giao Hàng Tiết Kiệm) shipping service implementation
 * Provides shipping fee calculation using GHTK API
 */
@Slf4j
@Service
@Primary
public class GHTKShippingService extends ShippingCalculatorService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public GHTKShippingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ShippingFeeResponse calculateShippingFee(ShippingRequest request) {
        log.info("Calculating shipping fee using GHTK for request: {}", request);
        
        // Validate configuration
        if (!GHTKConfig.validateConfiguration()) {
            log.error("GHTK configuration is invalid");
            return createFallbackResponse("GHTK service is not properly configured");
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
            
            // Build API request URL
            URI apiUrl = buildApiUrl(request);
            
            // Create headers
            HttpHeaders headers = createHeaders();
            
            // Make API call
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.GET, entity, String.class
            );
            
            // Parse response
            return parseResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee using GHTK: {}", e.getMessage(), e);
            return createFallbackResponse("GHTK API call failed: " + e.getMessage());
        }
    }
    
    @Override
    public String getProviderName() {
        return "GHTK";
    }
    
    @Override
    public boolean isAvailable() {
        return GHTKConfig.validateConfiguration();
    }
    
    @Override
    public int getPriority() {
        return 1; // High priority as primary shipping provider
    }
    
    /**
     * Fill in default values for missing request parameters
     */
    private void fillDefaultValues(ShippingRequest request) {
        if (request.getPickProvince() == null || request.getPickProvince().trim().isEmpty()) {
            request.setPickProvince(GHTKConfig.ghtk_DefaultPickProvince);
        }
        if (request.getPickDistrict() == null || request.getPickDistrict().trim().isEmpty()) {
            request.setPickDistrict(GHTKConfig.ghtk_DefaultPickDistrict);
        }
        if (request.getPickWard() == null || request.getPickWard().trim().isEmpty()) {
            request.setPickWard(GHTKConfig.ghtk_DefaultPickWard);
        }
        if (request.getPickAddress() == null || request.getPickAddress().trim().isEmpty()) {
            request.setPickAddress(GHTKConfig.ghtk_DefaultPickAddress);
        }
        if (request.getDeliverOption() == null || request.getDeliverOption().trim().isEmpty()) {
            request.setDeliverOption(GHTKConfig.ghtk_DefaultDeliverOption);
        }
        if (request.getTransport() == null || request.getTransport().trim().isEmpty()) {
            request.setTransport(GHTKConfig.ghtk_DefaultTransport);
        }
    }
    
    /**
     * Build GHTK API URL with query parameters
     */
    private URI buildApiUrl(ShippingRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(GHTKConfig.getShippingFeeUrl())
            .queryParam("pick_province", request.getPickProvince())
            .queryParam("pick_district", request.getPickDistrict())
            .queryParam("province", request.getProvince())
            .queryParam("district", request.getDistrict())
            .queryParam("weight", request.getWeight())
            .queryParam("deliver_option", request.getDeliverOption())
            .queryParam("transport", request.getTransport());
        
        // Add optional parameters if present
        if (request.getPickWard() != null && !request.getPickWard().trim().isEmpty()) {
            builder.queryParam("pick_ward", request.getPickWard());
        }
        if (request.getPickAddress() != null && !request.getPickAddress().trim().isEmpty()) {
            builder.queryParam("pick_address", request.getPickAddress());
        }
        if (request.getWard() != null && !request.getWard().trim().isEmpty()) {
            builder.queryParam("ward", request.getWard());
        }
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            builder.queryParam("address", request.getAddress());
        }
        if (request.getValue() != null) {
            builder.queryParam("value", request.getValue().intValue());
        }
        
        return builder.build().toUri();
    }
    
    /**
     * Create HTTP headers for GHTK API
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", GHTKConfig.ghtk_ApiToken);
        if (GHTKConfig.ghtk_PartnerCode != null && !GHTKConfig.ghtk_PartnerCode.trim().isEmpty()) {
            headers.set("X-Client-Source", GHTKConfig.ghtk_PartnerCode);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    /**
     * Parse GHTK API response
     */
    private ShippingFeeResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            boolean success = root.path("success").asBoolean(false);
            String message = root.path("message").asText("");
            String logId = root.path("log_id").asText("");
            
            if (!success) {
                String errorMessage = message.isEmpty() ? "Unknown GHTK API error" : message;
                return ShippingFeeResponse.error("GHTK_API_ERROR", errorMessage, getProviderName());
            }
            
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                return ShippingFeeResponse.error("INVALID_RESPONSE", "Missing data in GHTK response", getProviderName());
            }
            
            // Extract shipping fee from response
            // GHTK API response structure may vary, so we try different possible field names
            BigDecimal shippingFee = extractShippingFee(data);
            if (shippingFee == null) {
                return ShippingFeeResponse.error("INVALID_RESPONSE", "Could not extract shipping fee from GHTK response", getProviderName());
            }
            
            // Extract additional information if available
            BigDecimal insuranceFee = extractBigDecimal(data, "insurance_fee");
            String serviceName = data.path("service_name").asText("");
            
            ShippingFeeResponse response = ShippingFeeResponse.success(shippingFee, insuranceFee, serviceName, getProviderName());
            response.setLogId(logId);
            response.setMessage(message);
            
            log.info("Successfully calculated GHTK shipping fee: {} VND", shippingFee);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to parse GHTK response: {}", e.getMessage(), e);
            return ShippingFeeResponse.error("PARSE_ERROR", "Failed to parse GHTK response: " + e.getMessage(), getProviderName());
        }
    }
    
    /**
     * Extract shipping fee from GHTK response data
     * Tries multiple possible field names
     */
    private BigDecimal extractShippingFee(JsonNode data) {
        // Try common field names for shipping fee
        String[] possibleFields = {"fee", "shipping_fee", "total_fee", "ship_money", "phi_van_chuyen"};
        
        for (String field : possibleFields) {
            BigDecimal fee = extractBigDecimal(data, field);
            if (fee != null) {
                return fee;
            }
        }
        
        return null;
    }
    
    /**
     * Extract BigDecimal value from JSON node
     */
    private BigDecimal extractBigDecimal(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (!fieldNode.isMissingNode() && !fieldNode.isNull()) {
            try {
                return new BigDecimal(fieldNode.asText());
            } catch (NumberFormatException e) {
                log.warn("Could not parse {} as BigDecimal: {}", fieldName, fieldNode.asText());
            }
        }
        return null;
    }
}
