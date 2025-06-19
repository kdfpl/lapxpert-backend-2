package com.lapxpert.backend.payment.controller;

import com.lapxpert.backend.common.util.IpAddressUtils;
import com.lapxpert.backend.common.util.PaymentValidationUtils;
import com.lapxpert.backend.payment.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base controller for payment gateways
 * Provides common functionality for VNPay, VietQR, and MoMo controllers
 * 
 * @param <T> Payment gateway service type
 */
@Slf4j
public abstract class BasePaymentController<T extends PaymentGatewayService> {

    protected final T paymentGatewayService;

    protected BasePaymentController(T paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Get the payment gateway name for logging and audit purposes
     * @return Payment gateway name (e.g., "VNPay", "VietQR", "MoMo")
     */
    protected abstract String getGatewayName();

    /**
     * Get maximum allowed payment amount for this gateway
     * @return Maximum amount in VND
     */
    protected abstract long getMaxPaymentAmount();

    /**
     * Validate common payment parameters
     * @param amount Payment amount
     * @param orderInfo Order information
     * @param orderId Order ID
     * @param returnUrl Return URL
     * @throws IllegalArgumentException if validation fails
     */
    protected void validatePaymentParameters(long amount, String orderInfo, String orderId, String returnUrl) {
        log.debug("Validating payment parameters for gateway: {}", getGatewayName());
        
        PaymentValidationUtils.validateAmount(amount, getMaxPaymentAmount());
        PaymentValidationUtils.validateOrderInfo(orderInfo);
        PaymentValidationUtils.validateOrderId(orderId);
        PaymentValidationUtils.validateUrl(returnUrl, "Return URL");
        
        log.debug("Payment parameters validation successful for gateway: {}", getGatewayName());
    }

    /**
     * Get client IP address from request
     * @param request HTTP servlet request
     * @return Client IP address
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        String clientIp = IpAddressUtils.getClientIpAddress(request);
        log.debug("Client IP detected for {}: {}", getGatewayName(), 
                 IpAddressUtils.sanitizeIpForLogging(clientIp));
        return clientIp;
    }

    /**
     * Create audit log entry for payment operations
     * @param operation Operation name (e.g., "CREATE_PAYMENT", "PROCESS_CALLBACK")
     * @param orderId Order ID
     * @param amount Payment amount
     * @param clientIp Client IP address
     * @param additionalInfo Additional information for audit
     */
    protected void createAuditLog(String operation, String orderId, Long amount, 
                                String clientIp, Map<String, Object> additionalInfo) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("gateway", getGatewayName());
            auditData.put("operation", operation);
            auditData.put("orderId", orderId);
            auditData.put("amount", amount);
            auditData.put("clientIp", IpAddressUtils.sanitizeIpForLogging(clientIp));
            auditData.put("timestamp", LocalDateTime.now());
            
            if (additionalInfo != null) {
                auditData.putAll(additionalInfo);
            }
            
            log.info("Payment audit log - Gateway: {}, Operation: {}, OrderId: {}, Amount: {}, IP: {}", 
                    getGatewayName(), operation, orderId, amount, 
                    IpAddressUtils.sanitizeIpForLogging(clientIp));
            
            // TODO: Implement proper audit logging to database or audit service
            // This could be enhanced to store in audit_log table or send to audit service
            
        } catch (Exception e) {
            log.error("Failed to create audit log for gateway: {}, operation: {}, orderId: {}", 
                     getGatewayName(), operation, orderId, e);
        }
    }

    /**
     * Create standardized success response
     * @param data Response data
     * @param message Success message
     * @return ResponseEntity with success response
     */
    protected ResponseEntity<Map<String, Object>> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("gateway", getGatewayName());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create standardized error response
     * @param message Error message
     * @param errorCode Error code
     * @param httpStatus HTTP status
     * @return ResponseEntity with error response
     */
    protected ResponseEntity<Map<String, Object>> createErrorResponse(String message, String errorCode, HttpStatus httpStatus) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("errorCode", errorCode);
        response.put("gateway", getGatewayName());
        response.put("timestamp", LocalDateTime.now());
        
        log.error("Payment error response - Gateway: {}, Error: {}, Code: {}", 
                 getGatewayName(), message, errorCode);
        
        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Global exception handler for payment operations
     * @param e Exception
     * @return Error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Map<String, Object>> handleValidationException(IllegalArgumentException e) {
        return createErrorResponse(e.getMessage(), "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    /**
     * Global exception handler for general exceptions
     * @param e Exception
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("Unexpected error in {} payment gateway", getGatewayName(), e);
        return createErrorResponse("Internal server error", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Validate callback/IPN request
     * @param request HTTP servlet request
     * @return true if request is valid, false otherwise
     */
    protected boolean validateCallbackRequest(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        
        // Log callback attempt
        log.info("Callback request received for {} from IP: {}", 
                getGatewayName(), IpAddressUtils.sanitizeIpForLogging(clientIp));
        
        // TODO: Implement gateway-specific IP whitelist validation
        // Each gateway may have specific IP ranges for callbacks
        
        return true;
    }
}
