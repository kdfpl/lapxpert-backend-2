package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;

/**
 * Payment Gateway Service Interface
 * Provides abstraction for different payment providers
 */
public interface PaymentGatewayService {
    
    /**
     * Create payment URL for a specific order
     */
    String createPaymentUrl(Long orderId, int amount, String orderInfo, String baseUrl, String clientIp);
    
    /**
     * Verify payment callback/return
     */
    PaymentVerificationResult verifyPayment(String transactionRef, String secureHash, String transactionStatus);
    
    /**
     * Get supported payment method
     */
    PhuongThucThanhToan getSupportedPaymentMethod();
    
    /**
     * Check if this gateway supports the payment method
     */
    boolean supports(PhuongThucThanhToan paymentMethod);
    
    /**
     * Payment verification result
     */
    class PaymentVerificationResult {
        private final boolean valid;
        private final boolean successful;
        private final String transactionId;
        private final String orderId;
        private final String errorMessage;
        
        public PaymentVerificationResult(boolean valid, boolean successful, String transactionId, String orderId, String errorMessage) {
            this.valid = valid;
            this.successful = successful;
            this.transactionId = transactionId;
            this.orderId = orderId;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public boolean isSuccessful() { return successful; }
        public String getTransactionId() { return transactionId; }
        public String getOrderId() { return orderId; }
        public String getErrorMessage() { return errorMessage; }
        
        // Static factory methods
        public static PaymentVerificationResult success(String transactionId, String orderId) {
            return new PaymentVerificationResult(true, true, transactionId, orderId, null);
        }
        
        public static PaymentVerificationResult failure(String transactionId, String orderId, String errorMessage) {
            return new PaymentVerificationResult(true, false, transactionId, orderId, errorMessage);
        }
        
        public static PaymentVerificationResult invalid(String errorMessage) {
            return new PaymentVerificationResult(false, false, null, null, errorMessage);
        }
    }
}
