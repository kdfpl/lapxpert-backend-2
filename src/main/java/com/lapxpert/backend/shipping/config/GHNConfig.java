package com.lapxpert.backend.shipping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GHN (GiaoHangNhanh) configuration class
 * Manages API credentials and endpoints for GHN shipping service
 *
 * IMPORTANT: GHN API requires business account registration
 * - Register at: https://5sao.ghn.dev/
 * - Obtain API token from GHN dashboard
 * - Business verification may be required for account activation
 *
 * Follows the same pattern as GHTKConfig and VNPayConfig
 */
@Slf4j
@Component
public class GHNConfig {
    
    // Static fields for global access
    public static String ghn_ApiToken;
    public static String ghn_BaseUrl;
    public static String ghn_ShopId;
    public static String ghn_DefaultServiceId;
    public static String ghn_DefaultServiceTypeId;
    
    // Default pickup location
    public static String ghn_DefaultFromDistrictId;
    public static String ghn_DefaultFromWardCode;
    public static String ghn_DefaultFromAddress;
    
    // Use setter injection for static fields
    @Value("${ghn.api-token}")
    public void setGhnApiToken(String ghnApiToken) {
        GHNConfig.ghn_ApiToken = ghnApiToken;
    }
    
    @Value("${ghn.base-url}")
    public void setGhnBaseUrl(String ghnBaseUrl) {
        GHNConfig.ghn_BaseUrl = ghnBaseUrl;
    }
    
    @Value("${ghn.shop-id}")
    public void setGhnShopId(String ghnShopId) {
        GHNConfig.ghn_ShopId = ghnShopId;
    }
    
    @Value("${ghn.default-service-id:53320}")
    public void setGhnDefaultServiceId(String ghnDefaultServiceId) {
        GHNConfig.ghn_DefaultServiceId = ghnDefaultServiceId;
    }
    
    @Value("${ghn.default-service-type-id:2}")
    public void setGhnDefaultServiceTypeId(String ghnDefaultServiceTypeId) {
        GHNConfig.ghn_DefaultServiceTypeId = ghnDefaultServiceTypeId;
    }
    
    @Value("${ghn.default-from-district-id}")
    public void setGhnDefaultFromDistrictId(String ghnDefaultFromDistrictId) {
        GHNConfig.ghn_DefaultFromDistrictId = ghnDefaultFromDistrictId;
    }
    
    @Value("${ghn.default-from-ward-code}")
    public void setGhnDefaultFromWardCode(String ghnDefaultFromWardCode) {
        GHNConfig.ghn_DefaultFromWardCode = ghnDefaultFromWardCode;
    }
    
    @Value("${ghn.default-from-address:}")
    public void setGhnDefaultFromAddress(String ghnDefaultFromAddress) {
        GHNConfig.ghn_DefaultFromAddress = ghnDefaultFromAddress;
    }
    
    /**
     * Get the shipping fee calculation endpoint URL
     */
    public static String getShippingFeeUrl() {
        return ghn_BaseUrl + "/shiip/public-api/v2/shipping-order/fee";
    }
    
    /**
     * Get the available services endpoint URL
     */
    public static String getAvailableServicesUrl() {
        return ghn_BaseUrl + "/shiip/public-api/v2/shipping-order/available-services";
    }
    
    /**
     * Validate GHN configuration on startup
     */
    public static boolean validateConfiguration() {
        boolean isValid = true;
        
        if (ghn_ApiToken == null || ghn_ApiToken.trim().isEmpty()) {
            log.error("GHN API Token is not configured");
            isValid = false;
        }
        
        if (ghn_BaseUrl == null || ghn_BaseUrl.trim().isEmpty()) {
            log.error("GHN Base URL is not configured");
            isValid = false;
        }
        
        if (ghn_ShopId == null || ghn_ShopId.trim().isEmpty()) {
            log.error("GHN Shop ID is not configured");
            isValid = false;
        }
        
        if (ghn_DefaultFromDistrictId == null || ghn_DefaultFromDistrictId.trim().isEmpty()) {
            log.error("GHN Default From District ID is not configured");
            isValid = false;
        }
        
        if (ghn_DefaultFromWardCode == null || ghn_DefaultFromWardCode.trim().isEmpty()) {
            log.error("GHN Default From Ward Code is not configured");
            isValid = false;
        }
        
        if (isValid) {
            log.info("GHN configuration validation successful");
        } else {
            log.error("GHN configuration validation failed");
        }
        
        return isValid;
    }
}
