package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.common.service.WebSocketIntegrationService;
import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.payment.momo.config.Environment;
import com.lapxpert.backend.payment.momo.enums.RequestType;
import com.lapxpert.backend.payment.momo.models.PaymentResponse;
import com.lapxpert.backend.payment.momo.processor.CreateOrderMoMo;
import com.lapxpert.backend.payment.momo.shared.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MoMo implementation of PaymentGatewayService using official MoMo SDK
 * Provides MoMo e-wallet payment integration following Vietnamese business requirements
 * Maintains 100% backward compatibility while leveraging official SDK capabilities
 *
 * This service is conditionally registered based on the momo.sdk.enabled property.
 * When disabled, the service will not be available for dependency injection,
 * providing safe migration and rollback capability.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "momo.sdk.enabled", havingValue = "true", matchIfMissing = false)
public class MoMoGatewayService implements PaymentGatewayService {

    @Value("${momo.sdk.enabled:false}")
    private boolean sdkEnabled;

    @Value("${momo.sdk.environment:dev}")
    private String environment;

    private final MoMoSDKErrorHandler errorHandler;
    private final WebSocketIntegrationService webSocketIntegrationService;

    public MoMoGatewayService(MoMoSDKErrorHandler errorHandler, WebSocketIntegrationService webSocketIntegrationService) {
        this.errorHandler = errorHandler;
        this.webSocketIntegrationService = webSocketIntegrationService;

        // Initialize MoMo SDK LogUtils to prevent NullPointerException
        LogUtils.init();

        log.info("MoMo Gateway Service initialized with official SDK integration, error handling, and WebSocket notifications");
    }
    
    @Override
    public String createPaymentUrl(Long orderId, int amount, String orderInfo, String baseUrl, String clientIp) {
        if (!sdkEnabled) {
            log.warn("MoMo SDK is disabled for order {}", orderId);
            throw new RuntimeException("MoMo SDK is currently disabled");
        }

        try {
            // Create SDK environment
            Environment env = Environment.selectEnv(environment);
            if (env == null) {
                throw new RuntimeException("Không thể khởi tạo môi trường MoMo SDK");
            }

            // Generate unique request ID and MoMo order ID
            String requestId = generateRequestId();
            String momoOrderId = generateMoMoOrderId(orderId);

            // Build return and notify URLs
            String returnUrl = baseUrl + "/api/payment/momo-payment";
            String notifyUrl = baseUrl + "/api/payment/momo-ipn";

            // Create payment using SDK with PAY_WITH_ATM as default method
            PaymentResponse response = CreateOrderMoMo.process(
                env,
                momoOrderId,
                requestId,
                String.valueOf(amount),
                orderInfo,
                returnUrl,
                notifyUrl,
                "", // extraData
                RequestType.PAY_WITH_ATM,
                null // autoCapture not needed for ATM payments
            );

            if (response == null) {
                throw new RuntimeException("Không thể tạo thanh toán MoMo: Phản hồi từ SDK là null");
            }

            if (response.getResultCode() != 0) {
                throw new RuntimeException("Không thể tạo thanh toán MoMo: " + response.getMessage());
            }

            String paymentUrl = response.getPayUrl();
            if (paymentUrl == null || paymentUrl.trim().isEmpty()) {
                throw new RuntimeException("Không thể tạo thanh toán MoMo: URL thanh toán không hợp lệ");
            }

            // Create success audit log
            Map<String, Object> successAuditInfo = new HashMap<>();
            successAuditInfo.put("requestId", requestId);
            successAuditInfo.put("paymentUrl", paymentUrl);
            successAuditInfo.put("resultCode", response.getResultCode());
            successAuditInfo.put("status", "SUCCESS");
            createAuditLog("CREATE_PAYMENT_SUCCESS", orderId.toString(), (long) amount, null, successAuditInfo);

            // Send WebSocket notification for payment initiation
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("requestId", requestId);
            paymentData.put("amount", amount);
            paymentData.put("orderInfo", orderInfo);
            paymentData.put("resultCode", response.getResultCode());
            sendPaymentNotification(orderId.toString(), "PAYMENT_INITIATED", paymentData);

            log.info("MoMo payment URL created successfully for order {} with request ID {}: {}",
                orderId, requestId, paymentUrl);
            return paymentUrl;

        } catch (Exception e) {
            // Create audit log for error
            Map<String, Object> errorAuditInfo = errorHandler.createErrorAuditInfo("CREATE_PAYMENT_URL", orderId.toString(), e);
            createAuditLog("CREATE_PAYMENT_ERROR", orderId.toString(), (long) amount, null, errorAuditInfo);

            // Handle SDK exception with proper error mapping
            PaymentVerificationResult errorResult = errorHandler.handleSDKException(e, orderId.toString());

            // Send WebSocket notification for payment failure
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("amount", amount);
            errorData.put("orderInfo", orderInfo);
            errorData.put("errorMessage", errorResult.getErrorMessage());
            errorData.put("severity", errorHandler.getErrorSeverity(e));
            sendPaymentNotification(orderId.toString(), "PAYMENT_FAILED", errorData);

            log.error("Failed to create MoMo payment URL for order {}: {} (Severity: {})",
                orderId, errorResult.getErrorMessage(), errorHandler.getErrorSeverity(e));

            throw new RuntimeException(errorResult.getErrorMessage(), e);
        }
    }
    
    @Override
    public PaymentVerificationResult verifyPayment(String transactionRef, String secureHash, String transactionStatus) {
        if (!sdkEnabled) {
            log.warn("MoMo SDK is disabled for transaction verification: {}", transactionRef);
            return PaymentVerificationResult.invalid("MoMo SDK is currently disabled");
        }

        try {
            // For MoMo, we need to parse the callback parameters differently
            // This method will be called from the controller with proper MoMo callback data

            // Parse transaction status (MoMo uses "0" for success)
            String orderId = extractOrderIdFromTransactionRef(transactionRef);
            if ("0".equals(transactionStatus)) {
                // Send WebSocket notification for successful payment
                Map<String, Object> successData = new HashMap<>();
                successData.put("transactionRef", transactionRef);
                successData.put("transactionStatus", transactionStatus);
                sendPaymentNotification(orderId, "PAYMENT_SUCCESS", successData);

                log.info("MoMo payment verification successful for transaction: {}", transactionRef);
                return PaymentVerificationResult.success(transactionRef, orderId);
            } else {
                // Send WebSocket notification for failed payment
                Map<String, Object> failureData = new HashMap<>();
                failureData.put("transactionRef", transactionRef);
                failureData.put("transactionStatus", transactionStatus);
                failureData.put("errorMessage", "Thanh toán MoMo thất bại với mã lỗi: " + transactionStatus);
                sendPaymentNotification(orderId, "PAYMENT_FAILED", failureData);

                log.warn("MoMo payment verification failed for transaction: {} with status: {}", transactionRef, transactionStatus);
                return PaymentVerificationResult.failure(transactionRef, orderId,
                    "Thanh toán MoMo thất bại với mã lỗi: " + transactionStatus);
            }

        } catch (Exception e) {
            // Create audit log for error
            Map<String, Object> errorAuditInfo = errorHandler.createErrorAuditInfo("VERIFY_PAYMENT", transactionRef, e);
            createAuditLog("VERIFY_PAYMENT_ERROR", transactionRef, null, null, errorAuditInfo);

            // Handle SDK exception with proper error mapping
            return errorHandler.handleSDKException(e, transactionRef);
        }
    }
    
    /**
     * Verify MoMo payment with full callback data
     * @param callbackData Full callback data from MoMo
     * @return Payment verification result
     */
    public PaymentVerificationResult verifyPaymentWithCallback(Map<String, String> callbackData) {
        if (!sdkEnabled) {
            String orderId = callbackData.get("orderId");
            log.warn("MoMo SDK is disabled for callback verification: {}", orderId);
            return PaymentVerificationResult.invalid("MoMo SDK is currently disabled");
        }

        try {
            String orderId = callbackData.get("orderId");
            String transId = callbackData.get("transId");
            String resultCode = callbackData.get("resultCode");
            String message = callbackData.get("message");

            // Parse result code (MoMo uses "0" for success)
            if ("0".equals(resultCode)) {
                // Send WebSocket notification for successful payment callback
                Map<String, Object> successData = new HashMap<>();
                successData.put("transactionId", transId);
                successData.put("resultCode", resultCode);
                successData.put("message", message);
                successData.put("callbackData", callbackData);
                sendPaymentNotification(orderId, "PAYMENT_SUCCESS", successData);

                log.info("MoMo payment successful for order {}, transaction ID: {}", orderId, transId);
                return PaymentVerificationResult.success(transId, orderId);
            } else {
                // Send WebSocket notification for failed payment callback
                Map<String, Object> failureData = new HashMap<>();
                failureData.put("transactionId", transId);
                failureData.put("resultCode", resultCode);
                failureData.put("message", message);
                failureData.put("errorMessage", "Thanh toán MoMo thất bại: " + message);
                sendPaymentNotification(orderId, "PAYMENT_FAILED", failureData);

                log.warn("MoMo payment failed for order {}: {}", orderId, message);
                return PaymentVerificationResult.failure(transId, orderId, "Thanh toán MoMo thất bại: " + message);
            }

        } catch (Exception e) {
            String orderId = callbackData.get("orderId");
            // Create audit log for error
            Map<String, Object> errorAuditInfo = errorHandler.createErrorAuditInfo("VERIFY_CALLBACK", orderId, e);
            createAuditLog("VERIFY_CALLBACK_ERROR", orderId, null, null, errorAuditInfo);

            // Handle SDK exception with proper error mapping
            return errorHandler.handleSDKException(e, orderId);
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
     * Query MoMo transaction status
     * @param orderId Order ID
     * @param requestId Request ID
     * @return Transaction status
     */
    public Map<String, Object> queryTransactionStatus(String orderId, String requestId) {
        if (!sdkEnabled) {
            log.warn("MoMo SDK is disabled for transaction status query: {}", orderId);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("resultCode", -1);
            errorResult.put("message", "MoMo SDK is currently disabled");
            return errorResult;
        }

        try {
            // TODO: Implement using SDK QueryTransactionStatus processor
            // For now, return a basic implementation
            log.warn("MoMo transaction status query not yet implemented for order: {}", orderId);
            Map<String, Object> result = new HashMap<>();
            result.put("resultCode", -1);
            result.put("message", "Chức năng truy vấn trạng thái giao dịch MoMo chưa được triển khai");

            // Send WebSocket notification for status check
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("requestId", requestId);
            statusData.put("queryResult", result);
            sendPaymentNotification(orderId, "PAYMENT_STATUS_CHECKED", statusData);

            return result;
        } catch (Exception e) {
            log.error("Error querying MoMo transaction status for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("resultCode", -1);
            errorResult.put("message", "Lỗi truy vấn trạng thái giao dịch MoMo: " + e.getMessage());

            // Send WebSocket notification for status check error
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("requestId", requestId);
            errorData.put("errorMessage", e.getMessage());
            sendPaymentNotification(orderId, "PAYMENT_FAILED", errorData);

            return errorResult;
        }
    }

    /**
     * Send payment notification via WebSocket
     * Vietnamese topic: /topic/hoa-don/{orderId}
     * @param orderId Order ID
     * @param status Payment status (PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_FAILED, etc.)
     * @param paymentData Payment data to include in notification
     */
    private void sendPaymentNotification(String orderId, String status, Object paymentData) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("orderId", orderId);
            notification.put("paymentStatus", status);
            notification.put("paymentMethod", "MOMO");
            notification.put("timestamp", Instant.now());
            notification.put("message", getVietnameseStatusMessage(status));

            if (paymentData != null) {
                notification.put("data", paymentData);
            }

            // Send WebSocket notification using order update method
            webSocketIntegrationService.sendOrderUpdate(orderId, status, notification);

            log.debug("Sent MoMo payment notification for order {} with status: {}", orderId, status);

        } catch (Exception e) {
            log.error("Failed to send MoMo payment notification for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Get Vietnamese status message for payment notifications
     * @param status Payment status
     * @return Vietnamese message
     */
    private String getVietnameseStatusMessage(String status) {
        switch (status) {
            case "PAYMENT_INITIATED":
                return "Đã khởi tạo thanh toán MoMo";
            case "PAYMENT_SUCCESS":
                return "Thanh toán MoMo thành công";
            case "PAYMENT_FAILED":
                return "Thanh toán MoMo thất bại";
            case "PAYMENT_STATUS_CHECKED":
                return "Đã kiểm tra trạng thái thanh toán MoMo";
            default:
                return "Cập nhật trạng thái thanh toán MoMo";
        }
    }

    /**
     * Generate unique request ID for MoMo transactions
     */
    private String generateRequestId() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Generate unique MoMo order ID to avoid duplication errors.
     * Format: {orderId}-{timestamp} to ensure global uniqueness while maintaining correlation.
     *
     * @param orderId Original HoaDon order ID
     * @return Unique MoMo order ID
     */
    private String generateMoMoOrderId(Long orderId) {
        return orderId + "-" + System.currentTimeMillis();
    }

    /**
     * Extract original order ID from MoMo order ID.
     * Handles the format: {orderId}-{timestamp}
     *
     * @param momoOrderId MoMo order ID from callback
     * @return Original HoaDon order ID
     * @throws NumberFormatException if the format is invalid
     */
    public Long extractOrderIdFromMoMoOrderId(String momoOrderId) {
        if (momoOrderId == null || momoOrderId.trim().isEmpty()) {
            throw new IllegalArgumentException("MoMo order ID cannot be null or empty");
        }

        // Handle both old format (direct order ID) and new format (orderId-timestamp)
        if (momoOrderId.contains("-")) {
            // New format: orderId-timestamp
            String[] parts = momoOrderId.split("-");
            if (parts.length >= 2) {
                return Long.parseLong(parts[0]);
            }
        }

        // Fallback: try to parse as direct order ID (for backward compatibility)
        return Long.parseLong(momoOrderId);
    }

    /**
     * Extract order ID from transaction reference
     * For MoMo integration, we use order ID as transaction reference
     * @param transactionRef Transaction reference
     * @return Order ID
     */
    private String extractOrderIdFromTransactionRef(String transactionRef) {
        // For MoMo integration, we use order ID as transaction reference
        // So we can return the transaction reference as order ID
        return transactionRef;
    }

    /**
     * Create audit log entry for MoMo payment operations
     * @param operation Operation name
     * @param orderId Order ID
     * @param amount Payment amount
     * @param clientIp Client IP address
     * @param additionalInfo Additional information for audit
     */
    private void createAuditLog(String operation, String orderId, Long amount,
                              String clientIp, Map<String, Object> additionalInfo) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("gateway", "MoMo");
            auditData.put("operation", operation);
            auditData.put("orderId", orderId);
            auditData.put("amount", amount);
            auditData.put("clientIp", clientIp);
            auditData.put("timestamp", LocalDateTime.now());
            auditData.put("sdkEnabled", sdkEnabled);
            auditData.put("environment", environment);

            if (additionalInfo != null) {
                auditData.putAll(additionalInfo);
            }

            log.info("MoMo audit log - Operation: {}, OrderId: {}, Amount: {}, IP: {}",
                    operation, orderId, amount, clientIp);

            // TODO: Implement proper audit logging to database or audit service
            // This could be enhanced to store in audit_log table or send to audit service

        } catch (Exception e) {
            log.error("Failed to create MoMo audit log for operation: {}, orderId: {}",
                     operation, orderId, e);
        }
    }
}
