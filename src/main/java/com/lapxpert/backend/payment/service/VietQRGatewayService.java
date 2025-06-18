package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.vietqr.VietQRService;
import com.lapxpert.backend.vietqr.VietQRConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * VietQR implementation of PaymentGatewayService
 * Provides VietQR bank transfer payment integration following Vietnamese business requirements
 */
@Slf4j
@Service
public class VietQRGatewayService implements PaymentGatewayService {

    private final VietQRService vietQRService;

    public VietQRGatewayService(VietQRService vietQRService) {
        this.vietQRService = vietQRService;
    }
    
    @Override
    public String createPaymentUrl(Long orderId, int amount, String orderInfo, String baseUrl, String clientIp) {
        try {
            // Create VietQR payment URL (QR code image URL)
            String qrUrl = vietQRService.createPaymentUrl(amount, orderInfo, orderId.toString());
            
            log.info("VietQR payment URL created for order {} with amount {}: {}", orderId, amount, qrUrl);
            return qrUrl;
            
        } catch (Exception e) {
            log.error("Failed to create VietQR payment URL for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo mã QR thanh toán VietQR: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PaymentVerificationResult verifyPayment(String transactionRef, String secureHash, String transactionStatus) {
        try {
            // For VietQR, transaction verification is typically done through bank callbacks
            // or manual verification. The transactionStatus indicates the payment result.

            if ("SUCCESS".equals(transactionStatus) || "COMPLETED".equals(transactionStatus)) {
                log.info("VietQR payment verification successful for transaction: {}", transactionRef);
                return PaymentVerificationResult.success(transactionRef, extractOrderIdFromTransactionRef(transactionRef));
            } else if ("FAILED".equals(transactionStatus) || "CANCELLED".equals(transactionStatus)) {
                log.warn("VietQR payment verification failed for transaction: {} with status: {}", transactionRef, transactionStatus);
                return PaymentVerificationResult.failure(transactionRef, extractOrderIdFromTransactionRef(transactionRef),
                    "Thanh toán VietQR thất bại với trạng thái: " + transactionStatus);
            } else {
                log.warn("Unknown VietQR payment status for transaction: {}", transactionRef);
                return PaymentVerificationResult.failure(transactionRef, extractOrderIdFromTransactionRef(transactionRef),
                    "Trạng thái thanh toán VietQR không xác định: " + transactionStatus);
            }

        } catch (Exception e) {
            log.error("Error verifying VietQR payment for transaction {}: {}", transactionRef, e.getMessage(), e);
            return PaymentVerificationResult.invalid("Lỗi xác thực thanh toán VietQR: " + e.getMessage());
        }
    }
    
    /**
     * Verify VietQR payment with full transaction data
     * @param transactionData Full transaction data from bank or manual input
     * @return Payment verification result
     */
    public PaymentVerificationResult verifyPaymentWithTransactionData(Map<String, String> transactionData) {
        try {
            // Validate transaction data first
            if (!vietQRService.validateTransactionData(transactionData)) {
                log.error("Invalid VietQR transaction data");
                return PaymentVerificationResult.invalid("Dữ liệu giao dịch VietQR không hợp lệ");
            }
            
            int verificationResult = vietQRService.verifyPayment(transactionData);
            String orderId = transactionData.get("orderId");
            String transactionId = transactionData.get("transactionId");
            
            switch (verificationResult) {
                case 1: // Success
                    log.info("VietQR payment successful for order {}, transaction ID: {}", orderId, transactionId);
                    return PaymentVerificationResult.success(transactionId, orderId);
                    
                case 0: // Failed
                    String status = transactionData.get("status");
                    log.warn("VietQR payment failed for order {}: {}", orderId, status);
                    return PaymentVerificationResult.failure(transactionId, orderId, "Thanh toán VietQR thất bại: " + status);
                    
                case -1: // Invalid
                default:
                    log.error("Invalid VietQR payment transaction for order {}", orderId);
                    return PaymentVerificationResult.invalid("Dữ liệu giao dịch VietQR không hợp lệ");
            }
            
        } catch (Exception e) {
            log.error("Error verifying VietQR payment transaction: {}", e.getMessage(), e);
            return PaymentVerificationResult.invalid("Lỗi xác thực giao dịch VietQR: " + e.getMessage());
        }
    }
    
    @Override
    public PhuongThucThanhToan getSupportedPaymentMethod() {
        return PhuongThucThanhToan.VIETQR;
    }
    
    @Override
    public boolean supports(PhuongThucThanhToan paymentMethod) {
        return PhuongThucThanhToan.VIETQR.equals(paymentMethod);
    }
    
    /**
     * Extract order ID from transaction reference
     * For VietQR, we use payment reference format to extract order ID
     * @param transactionRef Transaction reference
     * @return Order ID
     */
    private String extractOrderIdFromTransactionRef(String transactionRef) {
        try {
            // Try to extract order ID from VietQR payment reference format
            String orderId = VietQRConfig.extractOrderIdFromPaymentRef(transactionRef);
            if (orderId != null) {
                return orderId;
            }
            
            // Fallback: assume transaction reference is order ID
            return transactionRef;
            
        } catch (Exception e) {
            log.warn("Failed to extract order ID from transaction reference {}: {}", transactionRef, e.getMessage());
            return transactionRef;
        }
    }
    
    /**
     * Generate VietQR payment instructions for customer
     * @param orderId Order ID
     * @param amount Payment amount
     * @return Payment instructions
     */
    public Map<String, Object> generatePaymentInstructions(String orderId, long amount) {
        try {
            String qrUrl = vietQRService.createPaymentUrl(amount, "", orderId);
            return vietQRService.generatePaymentInstructions(orderId, amount, qrUrl);
        } catch (Exception e) {
            log.error("Error generating VietQR payment instructions for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Không thể tạo hướng dẫn thanh toán VietQR: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Check VietQR payment status
     * @param orderId Order ID
     * @param expectedAmount Expected payment amount
     * @param bankTransactionId Bank transaction ID
     * @return Payment status
     */
    public Map<String, Object> checkPaymentStatus(String orderId, long expectedAmount, String bankTransactionId) {
        try {
            return vietQRService.checkPaymentStatus(orderId, expectedAmount, bankTransactionId);
        } catch (Exception e) {
            log.error("Error checking VietQR payment status for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("message", "Lỗi kiểm tra trạng thái thanh toán VietQR: " + e.getMessage());
            return errorResult;
        }
    }
}
