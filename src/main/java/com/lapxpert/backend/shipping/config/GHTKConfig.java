package com.lapxpert.backend.shipping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GHTK (Giao Hàng Tiết Kiệm) configuration class
 * Manages API credentials and endpoints for GHTK shipping service
 *
 * IMPORTANT: GHTK API requires business account registration
 * - Register at: https://khachhang.giaohangtietkiem.vn/web
 * - Obtain API token from: https://khachhang.giaohangtietkiem.vn/web/thong-tin-shop/cau-hinh-api
 * - Business verification may be required for account activation
 *
 * Follows the same pattern as VNPayConfig and MoMoConfig
 */
@Slf4j
@Component
public class GHTKConfig {
    
    // Static fields for global access
    public static String ghtk_ApiToken;
    public static String ghtk_BaseUrl;
    public static String ghtk_PartnerCode;
    public static String ghtk_DefaultDeliverOption;
    public static String ghtk_DefaultTransport;
    
    // Default pickup location
    public static String ghtk_DefaultPickProvince;
    public static String ghtk_DefaultPickDistrict;
    public static String ghtk_DefaultPickWard;
    public static String ghtk_DefaultPickAddress;
    
    // Use setter injection for static fields
    @Value("${ghtk.api-token}")
    public void setGhtkApiToken(String ghtkApiToken) {
        GHTKConfig.ghtk_ApiToken = ghtkApiToken;
    }
    
    @Value("${ghtk.base-url}")
    public void setGhtkBaseUrl(String ghtkBaseUrl) {
        GHTKConfig.ghtk_BaseUrl = ghtkBaseUrl;
    }
    
    @Value("${ghtk.partner-code:}")
    public void setGhtkPartnerCode(String ghtkPartnerCode) {
        GHTKConfig.ghtk_PartnerCode = ghtkPartnerCode;
    }
    
    @Value("${ghtk.default-deliver-option}")
    public void setGhtkDefaultDeliverOption(String ghtkDefaultDeliverOption) {
        GHTKConfig.ghtk_DefaultDeliverOption = ghtkDefaultDeliverOption;
    }
    
    @Value("${ghtk.default-transport:road}")
    public void setGhtkDefaultTransport(String ghtkDefaultTransport) {
        GHTKConfig.ghtk_DefaultTransport = ghtkDefaultTransport;
    }
    
    @Value("${ghtk.default-pick-province}")
    public void setGhtkDefaultPickProvince(String ghtkDefaultPickProvince) {
        GHTKConfig.ghtk_DefaultPickProvince = ghtkDefaultPickProvince;
    }
    
    @Value("${ghtk.default-pick-district}")
    public void setGhtkDefaultPickDistrict(String ghtkDefaultPickDistrict) {
        GHTKConfig.ghtk_DefaultPickDistrict = ghtkDefaultPickDistrict;
    }
    
    @Value("${ghtk.default-pick-ward:}")
    public void setGhtkDefaultPickWard(String ghtkDefaultPickWard) {
        GHTKConfig.ghtk_DefaultPickWard = ghtkDefaultPickWard;
    }
    
    @Value("${ghtk.default-pick-address:}")
    public void setGhtkDefaultPickAddress(String ghtkDefaultPickAddress) {
        GHTKConfig.ghtk_DefaultPickAddress = ghtkDefaultPickAddress;
    }
    
    /**
     * Get the shipping fee calculation endpoint URL
     */
    public static String getShippingFeeUrl() {
        return ghtk_BaseUrl + "/services/shipment/fee";
    }
    
    /**
     * Validate GHTK configuration on startup
     */
    public static boolean validateConfiguration() {
        boolean isValid = true;
        
        if (ghtk_ApiToken == null || ghtk_ApiToken.trim().isEmpty()) {
            log.error("GHTK API Token is not configured");
            isValid = false;
        }
        
        if (ghtk_BaseUrl == null || ghtk_BaseUrl.trim().isEmpty()) {
            log.error("GHTK Base URL is not configured");
            isValid = false;
        }
        
        if (ghtk_DefaultDeliverOption == null || ghtk_DefaultDeliverOption.trim().isEmpty()) {
            log.error("GHTK Default Deliver Option is not configured");
            isValid = false;
        }
        
        if (ghtk_DefaultPickProvince == null || ghtk_DefaultPickProvince.trim().isEmpty()) {
            log.error("GHTK Default Pick Province is not configured");
            isValid = false;
        }
        
        if (ghtk_DefaultPickDistrict == null || ghtk_DefaultPickDistrict.trim().isEmpty()) {
            log.error("GHTK Default Pick District is not configured");
            isValid = false;
        }
        
        if (isValid) {
            log.info("GHTK configuration validation successful");
        } else {
            log.error("GHTK configuration validation failed");
        }
        
        return isValid;
    }
}
