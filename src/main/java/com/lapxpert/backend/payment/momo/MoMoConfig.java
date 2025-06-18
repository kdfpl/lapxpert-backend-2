package com.lapxpert.backend.payment.momo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Enhanced MoMo payment gateway configuration and utility class with improved security practices.
 *
 * Security enhancements:
 * - Secure random number generation
 * - Enhanced HMAC-SHA256 implementation with proper error handling
 * - Better parameter validation and encoding
 * - Comprehensive logging for security auditing
 * - Configuration validation methods
 */
@Slf4j
@Component
public class MoMoConfig {
    
    public static String momo_PartnerCode;
    public static String momo_AccessKey;
    public static String momo_SecretKey;
    public static String momo_Endpoint;
    public static String momo_ReturnUrl;
    public static String momo_NotifyUrl;

    // Use setter injection for static fields
    @Value("${momo.partner-code}")
    public void setMomoPartnerCode(String momoPartnerCode) {
        MoMoConfig.momo_PartnerCode = momoPartnerCode;
    }

    @Value("${momo.access-key}")
    public void setMomoAccessKey(String momoAccessKey) {
        MoMoConfig.momo_AccessKey = momoAccessKey;
    }

    @Value("${momo.secret-key}")
    public void setMomoSecretKey(String momoSecretKey) {
        MoMoConfig.momo_SecretKey = momoSecretKey;
    }

    @Value("${momo.endpoint}")
    public void setMomoEndpoint(String momoEndpoint) {
        MoMoConfig.momo_Endpoint = momoEndpoint;
    }

    @Value("${momo.return-url}")
    public void setMomoReturnUrl(String momoReturnUrl) {
        MoMoConfig.momo_ReturnUrl = momoReturnUrl;
    }

    @Value("${momo.notify-url}")
    public void setMomoNotifyUrl(String momoNotifyUrl) {
        MoMoConfig.momo_NotifyUrl = momoNotifyUrl;
    }

    /**
     * Enhanced HMAC SHA256 signature generation with proper error handling and validation.
     *
     * @param key Secret key for signing
     * @param data Data to be signed
     * @return HMAC SHA256 signature in lowercase hexadecimal format
     * @throws IllegalArgumentException if key or data is null/empty
     */
    public static String hmacSHA256(final String key, final String data) {
        if (key == null || key.trim().isEmpty()) {
            log.error("HMAC-SHA256 key is null or empty");
            throw new IllegalArgumentException("HMAC key cannot be null or empty");
        }

        if (data == null || data.trim().isEmpty()) {
            log.error("HMAC-SHA256 data is null or empty");
            throw new IllegalArgumentException("HMAC data cannot be null or empty");
        }

        try {
            final Mac hmac256 = Mac.getInstance("HmacSHA256");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
            hmac256.init(secretKey);

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac256.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }

            String signature = sb.toString();
            log.debug("HMAC-SHA256 signature generated successfully");
            return signature;

        } catch (Exception ex) {
            log.error("Error generating HMAC-SHA256 signature: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", ex);
        }
    }

    /**
     * Generate SHA256 hash
     * @param message Message to hash
     * @return SHA256 hash
     */
    public static String sha256(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * Generate cryptographically secure random request ID for MoMo transactions.
     *
     * @param length Length of the random string
     * @return Random alphanumeric string
     * @throws IllegalArgumentException if length is invalid
     */
    public static String getRandomRequestId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        if (length > 100) {
            throw new IllegalArgumentException("Length cannot exceed 100 characters");
        }

        try {
            SecureRandom secureRandom = new SecureRandom();
            String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }

            String requestId = sb.toString();
            log.debug("Generated secure random request ID of length: {}", length);
            return requestId;

        } catch (Exception e) {
            log.error("Error generating secure random request ID: {}", e.getMessage(), e);
            // Fallback to regular Random if SecureRandom fails
            Random rnd = new Random();
            String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            return sb.toString();
        }
    }

    /**
     * Generate cryptographically secure random order ID for MoMo transactions.
     *
     * @param length Length of the random string
     * @return Random numeric string
     * @throws IllegalArgumentException if length is invalid
     */
    public static String getRandomOrderId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        if (length > 50) {
            throw new IllegalArgumentException("Length cannot exceed 50 characters");
        }

        try {
            SecureRandom secureRandom = new SecureRandom();
            String chars = "0123456789";
            StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }

            String orderId = sb.toString();
            log.debug("Generated secure random order ID of length: {}", length);
            return orderId;

        } catch (Exception e) {
            log.error("Error generating secure random order ID: {}", e.getMessage(), e);
            // Fallback to regular Random if SecureRandom fails
            Random rnd = new Random();
            String chars = "0123456789";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            return sb.toString();
        }
    }

    /**
     * Enhanced MoMo signature validation with comprehensive error handling.
     *
     * @param rawSignature Raw signature string before hashing
     * @param signature Signature to validate
     * @return true if signature is valid, false otherwise
     */
    public static boolean validateSignature(String rawSignature, String signature) {
        if (rawSignature == null || rawSignature.trim().isEmpty()) {
            log.error("Raw signature is null or empty");
            return false;
        }

        if (signature == null || signature.trim().isEmpty()) {
            log.error("Signature is null or empty");
            return false;
        }

        if (momo_SecretKey == null || momo_SecretKey.trim().isEmpty()) {
            log.error("MoMo secret key is not configured");
            return false;
        }

        try {
            String computedSignature = hmacSHA256(momo_SecretKey, rawSignature);
            boolean isValid = computedSignature.equals(signature);

            if (isValid) {
                log.debug("MoMo signature validation successful");
            } else {
                log.warn("MoMo signature validation failed - Expected: {}, Received: {}",
                        computedSignature, signature);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating MoMo signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate MoMo configuration on startup.
     *
     * @return true if configuration is valid
     */
    public static boolean validateConfiguration() {
        boolean isValid = true;

        if (momo_PartnerCode == null || momo_PartnerCode.trim().isEmpty()) {
            log.error("MoMo Partner Code is not configured");
            isValid = false;
        }

        if (momo_AccessKey == null || momo_AccessKey.trim().isEmpty()) {
            log.error("MoMo Access Key is not configured");
            isValid = false;
        }

        if (momo_SecretKey == null || momo_SecretKey.trim().isEmpty()) {
            log.error("MoMo Secret Key is not configured");
            isValid = false;
        }

        if (momo_Endpoint == null || momo_Endpoint.trim().isEmpty()) {
            log.error("MoMo Endpoint is not configured");
            isValid = false;
        }

        if (momo_ReturnUrl == null || momo_ReturnUrl.trim().isEmpty()) {
            log.error("MoMo Return URL is not configured");
            isValid = false;
        }

        if (momo_NotifyUrl == null || momo_NotifyUrl.trim().isEmpty()) {
            log.error("MoMo Notify URL is not configured");
            isValid = false;
        }

        if (isValid) {
            log.info("MoMo configuration validation successful");
        } else {
            log.error("MoMo configuration validation failed");
        }

        return isValid;
    }

    /**
     * Get MoMo API version information.
     *
     * @return API version string
     */
    public static String getApiVersion() {
        return "v3";
    }

    /**
     * Check if endpoint is using v3 API.
     *
     * @return true if using v3 API
     */
    public static boolean isV3Api() {
        return momo_Endpoint != null && momo_Endpoint.contains("/v2/gateway/api/create");
    }
}
