package com.lapxpert.backend.vietqr.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * VietQR payment gateway configuration and utility class
 * Provides configuration management and utilities for VietQR bank transfer integration
 */
@Component
public class VietQRConfig {
    
    public static String vietqr_BankId;
    public static String vietqr_AccountNo;
    public static String vietqr_Template;
    public static String vietqr_Amount;
    public static String vietqr_Description;
    public static String vietqr_AccountName;
    public static String vietqr_ReturnUrl;
    public static String vietqr_NotifyUrl;

    // Use setter injection for static fields
    @Value("${vietqr.bank-id}")
    public void setVietQRBankId(String vietqrBankId) {
        VietQRConfig.vietqr_BankId = vietqrBankId;
    }

    @Value("${vietqr.account-no}")
    public void setVietQRAccountNo(String vietqrAccountNo) {
        VietQRConfig.vietqr_AccountNo = vietqrAccountNo;
    }

    @Value("${vietqr.template}")
    public void setVietQRTemplate(String vietqrTemplate) {
        VietQRConfig.vietqr_Template = vietqrTemplate;
    }

    @Value("${vietqr.amount}")
    public void setVietQRAmount(String vietqrAmount) {
        VietQRConfig.vietqr_Amount = vietqrAmount;
    }

    @Value("${vietqr.description}")
    public void setVietQRDescription(String vietqrDescription) {
        VietQRConfig.vietqr_Description = vietqrDescription;
    }

    @Value("${vietqr.account-name}")
    public void setVietQRAccountName(String vietqrAccountName) {
        VietQRConfig.vietqr_AccountName = vietqrAccountName;
    }

    @Value("${vietqr.return-url}")
    public void setVietQRReturnUrl(String vietqrReturnUrl) {
        VietQRConfig.vietqr_ReturnUrl = vietqrReturnUrl;
    }

    @Value("${vietqr.notify-url}")
    public void setVietQRNotifyUrl(String vietqrNotifyUrl) {
        VietQRConfig.vietqr_NotifyUrl = vietqrNotifyUrl;
    }

    /**
     * Generate VietQR payment URL
     * @param bankId Bank ID (e.g., 970415 for Vietinbank)
     * @param accountNo Bank account number
     * @param amount Payment amount
     * @param description Payment description
     * @param template QR template (compact, compact2, qr_only, print)
     * @return VietQR URL
     */
    public static String generateVietQRUrl(String bankId, String accountNo, long amount, String description, String template) {
        try {
            // VietQR API URL format
            String baseUrl = "https://img.vietqr.io/image";
            
            // Build VietQR URL with parameters
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("/").append(bankId).append("-").append(accountNo);
            urlBuilder.append("-").append(template).append(".png");
            urlBuilder.append("?amount=").append(amount);
            urlBuilder.append("&addInfo=").append(java.net.URLEncoder.encode(description, StandardCharsets.UTF_8));
            urlBuilder.append("&accountName=").append(java.net.URLEncoder.encode(vietqr_AccountName, StandardCharsets.UTF_8));
            
            return urlBuilder.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating VietQR URL", e);
        }
    }

    /**
     * Generate payment reference for VietQR transaction
     * @param orderId Order ID
     * @return Payment reference
     */
    public static String generatePaymentReference(String orderId) {
        // Create a unique payment reference using order ID and timestamp
        long timestamp = System.currentTimeMillis();
        return "VQRPAY" + orderId + timestamp % 100000;
    }

    /**
     * Generate random transaction ID for VietQR payments
     * @param length Length of the random string
     * @return Random alphanumeric string
     */
    public static String getRandomTransactionId(int length) {
        Random rnd = new Random();
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate SHA256 hash for transaction verification
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
     * Validate VietQR payment reference format
     * @param paymentRef Payment reference to validate
     * @return true if valid format, false otherwise
     */
    public static boolean validatePaymentReference(String paymentRef) {
        if (paymentRef == null || paymentRef.trim().isEmpty()) {
            return false;
        }
        
        // VietQR payment reference should start with VQRPAY and contain order ID
        return paymentRef.startsWith("VQRPAY") && paymentRef.length() >= 10;
    }

    /**
     * Extract order ID from VietQR payment reference
     * @param paymentRef Payment reference
     * @return Order ID
     */
    public static String extractOrderIdFromPaymentRef(String paymentRef) {
        if (!validatePaymentReference(paymentRef)) {
            return null;
        }
        
        try {
            // Remove VQRPAY prefix and timestamp suffix to get order ID
            String withoutPrefix = paymentRef.substring(6); // Remove "VQRPAY"
            // Remove last 5 digits (timestamp suffix)
            if (withoutPrefix.length() > 5) {
                return withoutPrefix.substring(0, withoutPrefix.length() - 5);
            }
            return withoutPrefix;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format VietQR payment description with order information
     * @param orderId Order ID
     * @param customerName Customer name (optional)
     * @return Formatted description
     */
    public static String formatPaymentDescription(String orderId, String customerName) {
        StringBuilder description = new StringBuilder("Thanh toan don hang ");
        description.append(orderId);
        
        if (customerName != null && !customerName.trim().isEmpty()) {
            description.append(" - ").append(customerName.trim());
        }
        
        return description.toString();
    }
}
