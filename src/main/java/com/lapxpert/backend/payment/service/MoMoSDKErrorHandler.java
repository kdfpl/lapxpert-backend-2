package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.payment.momo.shared.exception.MoMoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Error handler for MoMo SDK exceptions and error mapping
 * Maps SDK exceptions to Spring Boot patterns and maintains Vietnamese business terminology
 */
@Slf4j
@Component
public class MoMoSDKErrorHandler {

    /**
     * Handle SDK exceptions and map to PaymentVerificationResult
     * @param e Exception from SDK
     * @param orderId Order ID for context
     * @return Mapped payment verification result
     */
    public PaymentGatewayService.PaymentVerificationResult handleSDKException(Exception e, String orderId) {
        log.error("Handling MoMo SDK exception for order {}: {}", orderId, e.getMessage(), e);
        
        if (e instanceof MoMoException) {
            return handleMoMoException((MoMoException) e, orderId);
        } else if (e instanceof IllegalArgumentException) {
            return handleValidationException(e, orderId);
        } else if (e instanceof RuntimeException) {
            return handleRuntimeException((RuntimeException) e, orderId);
        } else {
            return handleGeneralException(e, orderId);
        }
    }
    
    /**
     * Handle specific MoMo SDK exceptions
     */
    private PaymentGatewayService.PaymentVerificationResult handleMoMoException(MoMoException e, String orderId) {
        String errorMessage = mapMoMoErrorToVietnamese(e.getMessage());
        log.warn("MoMo SDK exception for order {}: {}", orderId, errorMessage);
        return PaymentGatewayService.PaymentVerificationResult.failure(null, orderId, errorMessage);
    }
    
    /**
     * Handle validation exceptions
     */
    private PaymentGatewayService.PaymentVerificationResult handleValidationException(Exception e, String orderId) {
        String errorMessage = "Dữ liệu thanh toán MoMo không hợp lệ: " + e.getMessage();
        log.warn("MoMo validation error for order {}: {}", orderId, errorMessage);
        return PaymentGatewayService.PaymentVerificationResult.invalid(errorMessage);
    }
    
    /**
     * Handle runtime exceptions
     */
    private PaymentGatewayService.PaymentVerificationResult handleRuntimeException(RuntimeException e, String orderId) {
        String errorMessage = "Lỗi xử lý thanh toán MoMo: " + e.getMessage();
        log.error("MoMo runtime error for order {}: {}", orderId, errorMessage);
        return PaymentGatewayService.PaymentVerificationResult.invalid(errorMessage);
    }
    
    /**
     * Handle general exceptions
     */
    private PaymentGatewayService.PaymentVerificationResult handleGeneralException(Exception e, String orderId) {
        String errorMessage = "Lỗi hệ thống khi xử lý thanh toán MoMo";
        log.error("MoMo system error for order {}: {}", orderId, e.getMessage(), e);
        return PaymentGatewayService.PaymentVerificationResult.invalid(errorMessage);
    }
    
    /**
     * Map MoMo error messages to Vietnamese business terminology
     */
    private String mapMoMoErrorToVietnamese(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return "Lỗi không xác định từ MoMo";
        }
        
        String lowerMessage = originalMessage.toLowerCase();
        
        // Common MoMo error mappings
        if (lowerMessage.contains("invalid signature") || lowerMessage.contains("signature")) {
            return "Lỗi xác thực chữ ký MoMo";
        }
        if (lowerMessage.contains("invalid amount") || lowerMessage.contains("amount")) {
            return "Số tiền thanh toán không hợp lệ";
        }
        if (lowerMessage.contains("invalid order") || lowerMessage.contains("order")) {
            return "Thông tin đơn hàng không hợp lệ";
        }
        if (lowerMessage.contains("timeout") || lowerMessage.contains("time")) {
            return "Hết thời gian xử lý thanh toán MoMo";
        }
        if (lowerMessage.contains("network") || lowerMessage.contains("connection")) {
            return "Lỗi kết nối với hệ thống MoMo";
        }
        if (lowerMessage.contains("insufficient") || lowerMessage.contains("balance")) {
            return "Số dư tài khoản MoMo không đủ";
        }
        if (lowerMessage.contains("blocked") || lowerMessage.contains("suspended")) {
            return "Tài khoản MoMo bị tạm khóa";
        }
        if (lowerMessage.contains("limit") || lowerMessage.contains("exceed")) {
            return "Vượt quá hạn mức giao dịch MoMo";
        }
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("exists")) {
            return "Giao dịch MoMo đã tồn tại";
        }
        if (lowerMessage.contains("not found") || lowerMessage.contains("missing")) {
            return "Không tìm thấy thông tin giao dịch MoMo";
        }
        
        // Return original message with Vietnamese prefix if no mapping found
        return "Lỗi MoMo: " + originalMessage;
    }
    
    /**
     * Create audit information for error scenarios
     * @param operation Operation that failed
     * @param orderId Order ID
     * @param exception Exception that occurred
     * @return Audit information map
     */
    public Map<String, Object> createErrorAuditInfo(String operation, String orderId, Exception exception) {
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("operation", operation);
        auditInfo.put("orderId", orderId);
        auditInfo.put("errorType", exception.getClass().getSimpleName());
        auditInfo.put("errorMessage", exception.getMessage());
        auditInfo.put("gateway", "MoMo");
        auditInfo.put("status", "ERROR");
        
        if (exception instanceof MoMoException) {
            auditInfo.put("sdkException", true);
            auditInfo.put("vietnameseMessage", mapMoMoErrorToVietnamese(exception.getMessage()));
        }
        
        return auditInfo;
    }
    
    /**
     * Determine if an exception is recoverable
     * @param exception Exception to check
     * @return true if the operation can be retried
     */
    public boolean isRecoverableError(Exception exception) {
        if (exception instanceof MoMoException) {
            String message = exception.getMessage().toLowerCase();
            // Network errors and timeouts are typically recoverable
            return message.contains("timeout") || 
                   message.contains("network") || 
                   message.contains("connection") ||
                   message.contains("temporary");
        }
        
        // Runtime exceptions might be recoverable depending on the cause
        if (exception instanceof RuntimeException) {
            String message = exception.getMessage().toLowerCase();
            return message.contains("timeout") || message.contains("temporary");
        }
        
        // Validation errors are typically not recoverable
        return false;
    }
    
    /**
     * Get error severity level for monitoring and alerting
     * @param exception Exception to evaluate
     * @return Severity level (CRITICAL, HIGH, MEDIUM, LOW)
     */
    public String getErrorSeverity(Exception exception) {
        if (exception instanceof MoMoException) {
            String message = exception.getMessage().toLowerCase();
            if (message.contains("signature") || message.contains("security")) {
                return "CRITICAL";
            }
            if (message.contains("network") || message.contains("timeout")) {
                return "HIGH";
            }
            return "MEDIUM";
        }
        
        if (exception instanceof IllegalArgumentException) {
            return "LOW";
        }
        
        return "HIGH";
    }
}
