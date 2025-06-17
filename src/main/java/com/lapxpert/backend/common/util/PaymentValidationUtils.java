package com.lapxpert.backend.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Common validation utilities for payment gateways
 * Provides shared validation logic for VNPay, VietQR, and MoMo
 */
@Slf4j
public class PaymentValidationUtils {

    /**
     * Validate payment amount
     * @param amount Payment amount
     * @param maxAmount Maximum allowed amount
     * @throws IllegalArgumentException if amount is invalid
     */
    public static void validateAmount(long amount, long maxAmount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        
        if (amount > maxAmount) {
            throw new IllegalArgumentException("Payment amount exceeds maximum limit (" + maxAmount + " VND)");
        }
    }

    /**
     * Validate order information
     * @param orderInfo Order information
     * @throws IllegalArgumentException if order info is invalid
     */
    public static void validateOrderInfo(String orderInfo) {
        if (orderInfo == null || orderInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Order information cannot be null or empty");
        }
        
        if (orderInfo.length() > 255) {
            throw new IllegalArgumentException("Order information cannot exceed 255 characters");
        }
    }

    /**
     * Validate order ID
     * @param orderId Order ID
     * @throws IllegalArgumentException if order ID is invalid
     */
    public static void validateOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
    }

    /**
     * Validate URL
     * @param url URL to validate
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if URL is invalid
     */
    public static void validateUrl(String url, String paramName) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException(paramName + " must be a valid HTTP/HTTPS URL");
        }
    }

    /**
     * Validate IP address format (basic validation)
     * @param ipAddress IP address to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        // Basic IPv4 validation
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
