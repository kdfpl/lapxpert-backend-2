package com.lapxpert.backend.shipping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * GHN (GiaoHangNhanh) configuration class
 * Manages API credentials and endpoints for GHN shipping service
 *
 * IMPORTANT: GHN API requires business account registration
 * - Register at: https://5sao.ghn.dev/
 * - Obtain API token from GHN dashboard
 * - Business verification may be required for account activation
 *
 * Follows the same pattern as VNPayConfig and MoMoConfig
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
    @Value("${ghn.api-token:be944cc0-3e4b-11f0-8570-4aeb0e37b2f0}")
    public void setGhnApiToken(String ghnApiToken) {
        log.debug("Setting GHN API Token: {}", ghnApiToken != null ? ghnApiToken.substring(0, Math.min(8, ghnApiToken.length())) + "..." : "null");
        GHNConfig.ghn_ApiToken = ghnApiToken;
    }

    @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn}")
    public void setGhnBaseUrl(String ghnBaseUrl) {
        GHNConfig.ghn_BaseUrl = ghnBaseUrl;
    }

    @Value("${ghn.shop-id:5811361}")
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
    
    @Value("${ghn.default-from-district-id:1542}")
    public void setGhnDefaultFromDistrictId(String ghnDefaultFromDistrictId) {
        GHNConfig.ghn_DefaultFromDistrictId = ghnDefaultFromDistrictId;
    }

    @Value("${ghn.default-from-ward-code:1A0107}")
    public void setGhnDefaultFromWardCode(String ghnDefaultFromWardCode) {
        GHNConfig.ghn_DefaultFromWardCode = ghnDefaultFromWardCode;
    }
    
    @Value("${ghn.default-from-address:}")
    public void setGhnDefaultFromAddress(String ghnDefaultFromAddress) {
        GHNConfig.ghn_DefaultFromAddress = ghnDefaultFromAddress;
    }

    /**
     * Initialize and validate configuration after all properties are injected
     */
    @PostConstruct
    public void init() {
        log.info("Initializing GHN configuration...");
        validateConfiguration();
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

        log.debug("Validating GHN configuration:");
        log.debug("API Token: {}", ghn_ApiToken != null ? ghn_ApiToken.substring(0, Math.min(8, ghn_ApiToken.length())) + "..." : "null");
        log.debug("Base URL: {}", ghn_BaseUrl);
        log.debug("Shop ID: {}", ghn_ShopId);
        log.debug("From District ID: {}", ghn_DefaultFromDistrictId);
        log.debug("From Ward Code: {}", ghn_DefaultFromWardCode);

        if (ghn_ApiToken == null || ghn_ApiToken.trim().isEmpty()) {
            log.error("GHN API Token is not configured (value: '{}')", ghn_ApiToken);
            isValid = false;
        }

        if (ghn_BaseUrl == null || ghn_BaseUrl.trim().isEmpty()) {
            log.error("GHN Base URL is not configured (value: '{}')", ghn_BaseUrl);
            isValid = false;
        }

        if (ghn_ShopId == null || ghn_ShopId.trim().isEmpty()) {
            log.error("GHN Shop ID is not configured (value: '{}')", ghn_ShopId);
            isValid = false;
        }

        if (ghn_DefaultFromDistrictId == null || ghn_DefaultFromDistrictId.trim().isEmpty()) {
            log.error("GHN Default From District ID is not configured (value: '{}')", ghn_DefaultFromDistrictId);
            isValid = false;
        }

        if (ghn_DefaultFromWardCode == null || ghn_DefaultFromWardCode.trim().isEmpty()) {
            log.error("GHN Default From Ward Code is not configured (value: '{}')", ghn_DefaultFromWardCode);
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
