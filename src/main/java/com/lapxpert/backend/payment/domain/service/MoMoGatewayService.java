package com.lapxpert.backend.payment.domain.service;

import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.momo.domain.MoMoService;
import com.lapxpert.backend.momo.domain.MoMoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * MoMo implementation of PaymentGatewayService
 * Provides MoMo e-wallet payment integration following Vietnamese business requirements
 */
@Slf4j
@Service
public class MoMoGatewayService implements PaymentGatewayService {

    private final MoMoService moMoService;

    public MoMoGatewayService(MoMoService moMoService) {
        this.moMoService = moMoService;
    }
    
    @Override
    public String createPaymentUrl(Long orderId, int amount, String orderInfo, String baseUrl, String clientIp) {
        try {
            // Build return and notify URLs
            String returnUrl = baseUrl + MoMoConfig.momo_ReturnUrl;
            String notifyUrl = baseUrl + MoMoConfig.momo_NotifyUrl;
            
            // Create MoMo payment URL using order ID as correlation
            String paymentUrl = moMoService.createPaymentUrl(
                amount, 
                orderInfo, 
                orderId.toString(), 
                returnUrl, 
                notifyUrl
            );
            
            log.info("MoMo payment URL created for order {} with amount {}: {}", orderId, amount, paymentUrl);
            return paymentUrl;
            
        } catch (Exception e) {
            log.error("Failed to create MoMo payment URL for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo liên kết thanh toán MoMo: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PaymentVerificationResult verifyPayment(String transactionRef, String secureHash, String transactionStatus) {
        try {
            // For MoMo, we need to parse the callback parameters differently
            // This method will be called from the controller with proper MoMo callback data
            
            // Parse transaction status (MoMo uses "0" for success)
            if ("0".equals(transactionStatus)) {
                log.info("MoMo payment verification successful for transaction: {}", transactionRef);
                return PaymentVerificationResult.success(transactionRef, extractOrderIdFromTransactionRef(transactionRef));
            } else {
                log.warn("MoMo payment verification failed for transaction: {} with status: {}", transactionRef, transactionStatus);
                return PaymentVerificationResult.failure(transactionRef, extractOrderIdFromTransactionRef(transactionRef), 
                    "Thanh toán MoMo thất bại với mã lỗi: " + transactionStatus);
            }
            
        } catch (Exception e) {
            log.error("Error verifying MoMo payment for transaction {}: {}", transactionRef, e.getMessage(), e);
            return PaymentVerificationResult.invalid("Lỗi xác thực thanh toán MoMo: " + e.getMessage());
        }
    }
    
    /**
     * Verify MoMo payment with full callback data
     * @param callbackData Full callback data from MoMo
     * @return Payment verification result
     */
    public PaymentVerificationResult verifyPaymentWithCallback(Map<String, String> callbackData) {
        try {
            int verificationResult = moMoService.verifyPayment(callbackData);
            String orderId = callbackData.get("orderId");
            String transId = callbackData.get("transId");
            
            switch (verificationResult) {
                case 1: // Success
                    log.info("MoMo payment successful for order {}, transaction ID: {}", orderId, transId);
                    return PaymentVerificationResult.success(transId, orderId);
                    
                case 0: // Failed
                    String message = callbackData.get("message");
                    log.warn("MoMo payment failed for order {}: {}", orderId, message);
                    return PaymentVerificationResult.failure(transId, orderId, "Thanh toán MoMo thất bại: " + message);
                    
                case -1: // Invalid
                default:
                    log.error("Invalid MoMo payment callback for order {}", orderId);
                    return PaymentVerificationResult.invalid("Dữ liệu callback MoMo không hợp lệ");
            }
            
        } catch (Exception e) {
            log.error("Error verifying MoMo payment callback: {}", e.getMessage(), e);
            return PaymentVerificationResult.invalid("Lỗi xác thực callback MoMo: " + e.getMessage());
        }
    }
    
    @Override
    public PhuongThucThanhToan getSupportedPaymentMethod() {
        return PhuongThucThanhToan.MOMO;
    }
    
    @Override
    public boolean supports(PhuongThucThanhToan paymentMethod) {
        return PhuongThucThanhToan.MOMO.equals(paymentMethod);
    }
    
    /**
     * Extract order ID from transaction reference
     * For MoMo, the order ID is typically the same as transaction reference
     * @param transactionRef Transaction reference
     * @return Order ID
     */
    private String extractOrderIdFromTransactionRef(String transactionRef) {
        // For MoMo integration, we use order ID as transaction reference
        // So we can return the transaction reference as order ID
        return transactionRef;
    }
    
    /**
     * Query MoMo transaction status
     * @param orderId Order ID
     * @param requestId Request ID
     * @return Transaction status
     */
    public Map<String, Object> queryTransactionStatus(String orderId, String requestId) {
        try {
            return moMoService.queryTransactionStatus(orderId, requestId);
        } catch (Exception e) {
            log.error("Error querying MoMo transaction status for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("resultCode", -1);
            errorResult.put("message", "Lỗi truy vấn trạng thái giao dịch MoMo: " + e.getMessage());
            return errorResult;
        }
    }
}
