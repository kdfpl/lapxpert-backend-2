package com.lapxpert.backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Centralized configuration for all payment gateway integrations.
 * Consolidates configuration patterns from VNPayConfig, MoMoConfig, VietQRConfig
 * to provide unified payment gateway management and validation.
 * 
 * Supports multiple payment methods:
 * - VNPay: Vietnamese payment gateway with comprehensive API support
 * - MoMo: Mobile money payment with v3 API integration
 * - VietQR: QR code payment compliant with NAPAS standards
 * 
 * Features:
 * - Centralized configuration validation
 * - Environment-specific settings (sandbox/production)
 * - Consistent error handling and logging
 * - Vietnamese business terminology support
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Validated
@Slf4j
public class PaymentGatewayConfig {

    /**
     * VNPay payment gateway configuration.
     * Sandbox configuration for development and testing.
     */
    @NotNull
    private VNPaySettings vnpay = new VNPaySettings();

    /**
     * MoMo payment gateway configuration.
     * Supports v3 API with enhanced security features.
     */
    @NotNull
    private MoMoSettings momo = new MoMoSettings();

    /**
     * VietQR payment configuration.
     * Compliant with NAPAS standards and VietQR Version 2.13.
     */
    @NotNull
    private VietQRSettings vietqr = new VietQRSettings();

    /**
     * General payment configuration settings.
     */
    @NotNull
    private GeneralSettings general = new GeneralSettings();

    // Getters and setters
    public VNPaySettings getVnpay() { return vnpay; }
    public void setVnpay(VNPaySettings vnpay) { this.vnpay = vnpay; }

    public MoMoSettings getMomo() { return momo; }
    public void setMomo(MoMoSettings momo) { this.momo = momo; }

    public VietQRSettings getVietqr() { return vietqr; }
    public void setVietqr(VietQRSettings vietqr) { this.vietqr = vietqr; }

    public GeneralSettings getGeneral() { return general; }
    public void setGeneral(GeneralSettings general) { this.general = general; }

    /**
     * VNPay specific configuration settings.
     */
    public static class VNPaySettings {
        @NotBlank(message = "VNPay TMN Code is required")
        private String tmnCode;
        
        @NotBlank(message = "VNPay Hash Secret is required")
        private String hashSecret;
        
        @NotBlank(message = "VNPay Pay URL is required")
        private String payUrl;
        
        @NotBlank(message = "VNPay API URL is required")
        private String apiUrl;
        
        @NotBlank(message = "VNPay Return URL is required")
        private String returnUrl;

        // Getters and setters
        public String getTmnCode() { return tmnCode; }
        public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }

        public String getHashSecret() { return hashSecret; }
        public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }

        public String getPayUrl() { return payUrl; }
        public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    }

    /**
     * MoMo specific configuration settings.
     */
    public static class MoMoSettings {
        @NotBlank(message = "MoMo Partner Code is required")
        private String partnerCode;
        
        @NotBlank(message = "MoMo Access Key is required")
        private String accessKey;
        
        @NotBlank(message = "MoMo Secret Key is required")
        private String secretKey;
        
        @NotBlank(message = "MoMo Endpoint is required")
        private String endpoint;
        
        @NotBlank(message = "MoMo Return URL is required")
        private String returnUrl;
        
        @NotBlank(message = "MoMo Notify URL is required")
        private String notifyUrl;

        // Getters and setters
        public String getPartnerCode() { return partnerCode; }
        public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    }

    /**
     * VietQR specific configuration settings.
     */
    public static class VietQRSettings {
        @NotBlank(message = "VietQR Bank ID is required")
        private String bankId;
        
        @NotBlank(message = "VietQR Account Number is required")
        private String accountNo;
        
        @NotBlank(message = "VietQR Account Name is required")
        private String accountName;
        
        private String template = "compact2";
        private String clientId;
        private String apiKey;
        private String apiBaseUrl;

        // Getters and setters
        public String getBankId() { return bankId; }
        public void setBankId(String bankId) { this.bankId = bankId; }

        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    }

    /**
     * General payment configuration settings.
     */
    public static class GeneralSettings {
        private boolean enableAuditLogging = true;
        private boolean enableSecurityValidation = true;
        private int defaultTimeoutSeconds = 30;
        private String defaultCurrency = "VND";
        private long maxPaymentAmount = 100_000_000L; // 100 million VND

        // Getters and setters
        public boolean isEnableAuditLogging() { return enableAuditLogging; }
        public void setEnableAuditLogging(boolean enableAuditLogging) { this.enableAuditLogging = enableAuditLogging; }

        public boolean isEnableSecurityValidation() { return enableSecurityValidation; }
        public void setEnableSecurityValidation(boolean enableSecurityValidation) { this.enableSecurityValidation = enableSecurityValidation; }

        public int getDefaultTimeoutSeconds() { return defaultTimeoutSeconds; }
        public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) { this.defaultTimeoutSeconds = defaultTimeoutSeconds; }

        public String getDefaultCurrency() { return defaultCurrency; }
        public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

        public long getMaxPaymentAmount() { return maxPaymentAmount; }
        public void setMaxPaymentAmount(long maxPaymentAmount) { this.maxPaymentAmount = maxPaymentAmount; }
    }

    // Configuration validation removed for flexibility in different environments
    // Payment gateway configurations will be validated when actually used

    // Validation methods removed for flexibility in different environments
    // Individual payment services can validate their specific configurations when needed
}
