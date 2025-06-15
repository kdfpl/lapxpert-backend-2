package com.lapxpert.backend.momo.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * MoMo payment gateway configuration and utility class
 * Provides configuration management and cryptographic utilities for MoMo integration
 */
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
     * Generate HMAC SHA256 signature for MoMo API requests
     * @param key Secret key for signing
     * @param data Data to be signed
     * @return HMAC SHA256 signature
     */
    public static String hmacSHA256(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key and data cannot be null");
            }
            
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
            return sb.toString();
            
        } catch (Exception ex) {
            throw new RuntimeException("Error generating HMAC SHA256 signature", ex);
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
     * Generate random request ID for MoMo transactions
     * @param length Length of the random string
     * @return Random alphanumeric string
     */
    public static String getRandomRequestId(int length) {
        Random rnd = new Random();
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random order ID for MoMo transactions
     * @param length Length of the random string
     * @return Random numeric string
     */
    public static String getRandomOrderId(int length) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Validate MoMo signature
     * @param rawSignature Raw signature string before hashing
     * @param signature Signature to validate
     * @return true if signature is valid, false otherwise
     */
    public static boolean validateSignature(String rawSignature, String signature) {
        if (rawSignature == null || signature == null) {
            return false;
        }
        
        String computedSignature = hmacSHA256(momo_SecretKey, rawSignature);
        return computedSignature.equals(signature);
    }
}
