package com.lapxpert.backend.payment.vietqr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced VietQR payment service compliant with NAPAS standards and VietQR Version 2.13 specifications.
 *
 * NAPAS Compliance Features:
 * - VietQR Version 2.13 specification compliance
 * - State Bank of Vietnam requirements adherence
 * - Enhanced security with proper authentication
 * - Comprehensive error handling and validation
 * - Support for both Quick Link and Full API v2
 *
 * Security enhancements:
 * - API authentication with x-client-id and x-api-key
 * - Enhanced parameter validation
 * - Comprehensive audit logging for payment operations
 * - Better error handling with security-conscious error messages
 * - Enhanced transaction verification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQRService {

    // Use dependency injection for centralized beans from CommonBeansConfig
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create VietQR payment URL compliant with NAPAS standards and VietQR Version 2.13.
     *
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @return VietQR image URL for QR code display
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String createPaymentUrl(long amount, String orderInfo, String orderId) {
        log.info("Creating VietQR payment URL - Amount: {}, OrderID: {}", amount, orderId);

        try {
            // Enhanced parameter validation for NAPAS compliance
            validateCreatePaymentParameters(amount, orderInfo, orderId);

            // Generate payment reference for tracking
            String paymentRef = VietQRConfig.generatePaymentReference(orderId);

            // Format payment description with order information (NAPAS compliant)
            String description = VietQRConfig.formatPaymentDescription(orderId, orderInfo);

            // Generate VietQR URL using configured bank details with Version 2.13 compliance
            String qrUrl = VietQRConfig.generateVietQRUrl(
                VietQRConfig.vietqr_BankId,
                VietQRConfig.vietqr_AccountNo,
                amount,
                description,
                VietQRConfig.vietqr_Template
            );

            log.info("VietQR payment URL created successfully for OrderID: {} - PaymentRef: {}",
                    orderId, paymentRef);
            return qrUrl;

        } catch (IllegalArgumentException e) {
            log.warn("VietQR payment URL creation validation failed for OrderID: {} - {}",
                    orderId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error creating VietQR payment URL for OrderID: {} - {}",
                     orderId, e.getMessage(), e);
            throw new RuntimeException("Error creating VietQR payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced VietQR payment verification compliant with NAPAS standards.
     * Supports both bank webhooks and manual confirmation with comprehensive validation.
     *
     * @param transactionData Transaction data from bank or manual input
     * @return Payment verification result (1: success, 0: failed, -1: invalid)
     */
    public int verifyPayment(Map<String, String> transactionData) {
        String orderId = transactionData.get("orderId");
        String amount = transactionData.get("amount");
        String transactionId = transactionData.get("transactionId");
        String bankTransactionId = transactionData.get("bankTransactionId");
        String status = transactionData.get("status");

        log.info("Verifying VietQR payment - OrderID: {}, TransactionID: {}, BankTransactionID: {}, Status: {}",
                orderId, transactionId, bankTransactionId, status);

        try {
            // Enhanced validation for NAPAS compliance
            if (!validateTransactionData(transactionData)) {
                log.error("VietQR transaction validation failed for OrderID: {}", orderId);
                return -1; // Invalid
            }

            // Additional NAPAS compliance checks
            if (!validateNAPASCompliance(transactionData)) {
                log.error("VietQR transaction NAPAS compliance validation failed for OrderID: {}", orderId);
                return -1; // Invalid
            }

            // Check transaction status with enhanced status validation
            if ("SUCCESS".equals(status) || "COMPLETED".equals(status) || "PAID".equals(status)) {
                log.info("VietQR payment successful for OrderID: {}, BankTransactionID: {}",
                        orderId, bankTransactionId);
                return 1; // Success
            } else if ("FAILED".equals(status) || "CANCELLED".equals(status) || "REJECTED".equals(status)) {
                log.warn("VietQR payment failed for OrderID: {}, Status: {}", orderId, status);
                return 0; // Failed
            } else if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                log.info("VietQR payment pending for OrderID: {}, Status: {}", orderId, status);
                return 0; // Treat pending as failed for now
            } else {
                log.warn("Unknown VietQR payment status for OrderID: {}, Status: {}", orderId, status);
                return 0; // Failed
            }

        } catch (Exception e) {
            log.error("Error verifying VietQR payment for OrderID: {} - {}", orderId, e.getMessage(), e);
            return -1; // Invalid
        }
    }

    /**
     * Check VietQR payment status (manual verification helper)
     * @param orderId Order ID to check
     * @param expectedAmount Expected payment amount
     * @param bankTransactionId Bank transaction ID for verification
     * @return Payment status check result
     */
    public Map<String, Object> checkPaymentStatus(String orderId, long expectedAmount, String bankTransactionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // In a real implementation, this would query the bank API or database
            // For now, we'll return a structure that can be used for manual verification
            
            result.put("orderId", orderId);
            result.put("expectedAmount", expectedAmount);
            result.put("bankTransactionId", bankTransactionId);
            result.put("status", "PENDING_VERIFICATION");
            result.put("message", "VietQR payment requires manual verification through bank statement");
            result.put("verificationInstructions", 
                "Please verify bank transfer with amount " + expectedAmount + " VND for order " + orderId);
            
            log.info("VietQR payment status check for order {}: PENDING_VERIFICATION", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("Error checking VietQR payment status for order {}: {}", orderId, e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", "Error checking payment status: " + e.getMessage());
            return result;
        }
    }

    /**
     * Generate VietQR payment instructions for customer
     * @param orderId Order ID
     * @param amount Payment amount
     * @param qrUrl QR code URL
     * @return Payment instructions
     */
    public Map<String, Object> generatePaymentInstructions(String orderId, long amount, String qrUrl) {
        Map<String, Object> instructions = new HashMap<>();
        
        try {
            instructions.put("qrUrl", qrUrl);
            instructions.put("bankId", VietQRConfig.vietqr_BankId);
            instructions.put("accountNo", VietQRConfig.vietqr_AccountNo);
            instructions.put("accountName", VietQRConfig.vietqr_AccountName);
            instructions.put("amount", amount);
            instructions.put("orderId", orderId);
            instructions.put("description", VietQRConfig.formatPaymentDescription(orderId, ""));
            
            // Payment instructions in Vietnamese
            instructions.put("instructions", new String[]{
                "1. Mở ứng dụng ngân hàng trên điện thoại",
                "2. Quét mã QR hoặc chuyển khoản thủ công",
                "3. Kiểm tra thông tin chuyển khoản",
                "4. Xác nhận giao dịch",
                "5. Lưu lại biên lai để đối chiếu"
            });
            
            instructions.put("manualTransferInfo", Map.of(
                "bankName", "Ngân hàng theo mã " + VietQRConfig.vietqr_BankId,
                "accountNumber", VietQRConfig.vietqr_AccountNo,
                "accountName", VietQRConfig.vietqr_AccountName,
                "amount", amount + " VND",
                "content", VietQRConfig.formatPaymentDescription(orderId, "")
            ));
            
            instructions.put("timeout", "15 phút"); // Payment timeout
            instructions.put("note", "Vui lòng chuyển khoản đúng số tiền và nội dung để được xử lý nhanh chóng");
            
            return instructions;
            
        } catch (Exception e) {
            log.error("Error generating VietQR payment instructions for order {}: {}", orderId, e.getMessage(), e);
            instructions.put("error", "Không thể tạo hướng dẫn thanh toán");
            return instructions;
        }
    }

    /**
     * Validate VietQR transaction data
     * @param transactionData Transaction data to validate
     * @return true if valid, false otherwise
     */
    public boolean validateTransactionData(Map<String, String> transactionData) {
        try {
            // Check required fields
            String orderId = transactionData.get("orderId");
            String amount = transactionData.get("amount");
            String transactionId = transactionData.get("transactionId");
            
            if (orderId == null || orderId.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing orderId");
                return false;
            }
            
            if (amount == null || amount.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing amount");
                return false;
            }
            
            if (transactionId == null || transactionId.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing transactionId");
                return false;
            }
            
            // Validate amount is numeric
            try {
                Long.parseLong(amount);
            } catch (NumberFormatException e) {
                log.error("VietQR transaction validation failed: invalid amount format: {}", amount);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating VietQR transaction data: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create enhanced VietQR using Full API v2 with authentication (NAPAS compliant).
     *
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @param template QR template (compact, compact2, qr_only, print)
     * @return Enhanced VietQR response with QR code and data URL
     */
    public VietQRResponse createEnhancedVietQR(long amount, String orderInfo, String orderId, String template) {
        log.info("Creating enhanced VietQR with Full API v2 - Amount: {}, OrderID: {}, Template: {}",
                amount, orderId, template);

        try {
            // Enhanced parameter validation for NAPAS compliance
            validateCreatePaymentParameters(amount, orderInfo, orderId);
            validateTemplate(template);

            // Check if API credentials are configured for Full API v2
            if (!VietQRConfig.isFullApiConfigured()) {
                log.warn("Full API v2 credentials not configured, falling back to Quick Link for OrderID: {}", orderId);
                String quickLinkUrl = createPaymentUrl(amount, orderInfo, orderId);
                return VietQRResponse.fromQuickLink(quickLinkUrl, orderId);
            }

            // Prepare Full API v2 request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountNo", VietQRConfig.vietqr_AccountNo);
            requestBody.put("accountName", VietQRConfig.vietqr_AccountName);
            requestBody.put("acqId", Integer.parseInt(VietQRConfig.vietqr_BankId));
            requestBody.put("amount", amount);
            requestBody.put("addInfo", VietQRConfig.formatPaymentDescription(orderId, orderInfo));
            requestBody.put("format", "text");
            requestBody.put("template", template != null ? template : VietQRConfig.vietqr_Template);

            // Set headers with authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", VietQRConfig.vietqr_ClientId);
            headers.set("x-api-key", VietQRConfig.vietqr_ApiKey);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make API call to VietQR Full API v2
            String apiUrl = VietQRConfig.vietqr_ApiBaseUrl + "/v2/generate";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String code = (String) responseBody.get("code");

                if ("00".equals(code)) {
                    log.info("Enhanced VietQR created successfully for OrderID: {}", orderId);
                    return VietQRResponse.fromApiResponse(responseBody, orderId);
                } else {
                    String desc = (String) responseBody.get("desc");
                    log.error("VietQR Full API v2 error for OrderID: {} - Code: {}, Desc: {}",
                             orderId, code, desc);
                    throw new RuntimeException("VietQR API error: " + desc);
                }
            } else {
                log.error("Failed to call VietQR Full API v2 for OrderID: {} - HTTP {}",
                         orderId, response.getStatusCode());
                throw new RuntimeException("Failed to create enhanced VietQR");
            }

        } catch (IllegalArgumentException e) {
            log.warn("Enhanced VietQR creation validation failed for OrderID: {} - {}",
                    orderId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Error creating enhanced VietQR for OrderID: {} - {}",
                     orderId, e.getMessage(), e);
            throw new RuntimeException("Error creating enhanced VietQR: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced IPN (Instant Payment Notification) processing with NAPAS compliance.
     *
     * @param ipnData IPN data from bank or payment system
     * @return PaymentIPNResult containing validation result and payment status
     */
    public PaymentIPNResult processIPN(Map<String, String> ipnData) {
        String orderId = ipnData.get("orderId");
        String transactionId = ipnData.get("transactionId");
        String status = ipnData.get("status");

        log.info("Processing VietQR IPN - OrderID: {}, TransactionID: {}, Status: {}",
                orderId, transactionId, status);

        try {
            // Validate IPN parameters with NAPAS compliance
            if (!validateTransactionData(ipnData)) {
                log.error("VietQR IPN validation failed - missing required parameters for OrderID: {}", orderId);
                return PaymentIPNResult.invalid("Missing required parameters");
            }

            // Additional NAPAS compliance validation
            if (!validateNAPASCompliance(ipnData)) {
                log.error("VietQR IPN NAPAS compliance validation failed for OrderID: {}", orderId);
                return PaymentIPNResult.invalid("NAPAS compliance validation failed");
            }

            // Process payment verification
            int verificationResult = verifyPayment(ipnData);

            if (verificationResult == 1) {
                log.info("VietQR IPN processed successfully - Payment confirmed for OrderID: {}", orderId);
                return PaymentIPNResult.success(orderId, status);
            } else if (verificationResult == 0) {
                log.warn("VietQR IPN processed - Payment failed for OrderID: {}", orderId);
                return PaymentIPNResult.failed(orderId, status);
            } else {
                log.error("VietQR IPN verification failed for OrderID: {}", orderId);
                return PaymentIPNResult.invalid("Payment verification failed");
            }

        } catch (Exception e) {
            log.error("Error processing VietQR IPN for OrderID: {} - {}", orderId, e.getMessage(), e);
            return PaymentIPNResult.error("Internal processing error");
        }
    }

    /**
     * Validate parameters for createPaymentUrl method with NAPAS compliance.
     *
     * @param amount Payment amount
     * @param orderInfo Order information
     * @param orderId Order ID
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateCreatePaymentParameters(long amount, String orderInfo, String orderId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (amount > 999999999999L) { // VietQR limit: 13 digits
            throw new IllegalArgumentException("Payment amount exceeds VietQR limit (999,999,999,999 VND)");
        }

        if (orderInfo == null || orderInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Order information cannot be null or empty");
        }

        if (orderInfo.length() > 25) { // NAPAS VietQR limit for addInfo
            throw new IllegalArgumentException("Order information cannot exceed 25 characters for NAPAS compliance");
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        // Validate order ID format (should be numeric for VietQR)
        try {
            Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Order ID must be a valid numeric value");
        }

        // NAPAS compliance: Check for special characters in order info
        if (!orderInfo.matches("^[a-zA-Z0-9\\s]+$")) {
            throw new IllegalArgumentException("Order information contains invalid characters. Only alphanumeric characters and spaces are allowed for NAPAS compliance");
        }
    }

    /**
     * Validate VietQR template parameter.
     *
     * @param template Template to validate
     * @throws IllegalArgumentException if template is invalid
     */
    private void validateTemplate(String template) {
        if (template != null && !template.trim().isEmpty()) {
            String[] validTemplates = {"compact", "compact2", "qr_only", "print"};
            boolean isValid = false;
            for (String validTemplate : validTemplates) {
                if (validTemplate.equals(template)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new IllegalArgumentException("Invalid template. Valid templates are: compact, compact2, qr_only, print");
            }
        }
    }

    /**
     * Validate NAPAS compliance for transaction data.
     *
     * @param transactionData Transaction data to validate
     * @return true if NAPAS compliant, false otherwise
     */
    private boolean validateNAPASCompliance(Map<String, String> transactionData) {
        try {
            String amount = transactionData.get("amount");
            String addInfo = transactionData.get("addInfo");
            String orderId = transactionData.get("orderId");

            // NAPAS amount validation
            if (amount != null) {
                try {
                    long amountValue = Long.parseLong(amount);
                    if (amountValue <= 0 || amountValue > 999999999999L) {
                        log.error("NAPAS compliance validation failed: invalid amount range");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    log.error("NAPAS compliance validation failed: invalid amount format");
                    return false;
                }
            }

            // NAPAS addInfo validation (max 25 characters, no special characters)
            if (addInfo != null && (addInfo.length() > 25 || !addInfo.matches("^[a-zA-Z0-9\\s]*$"))) {
                log.error("NAPAS compliance validation failed: invalid addInfo format");
                return false;
            }

            // NAPAS order ID validation
            if (orderId != null) {
                try {
                    Long.parseLong(orderId);
                } catch (NumberFormatException e) {
                    log.error("NAPAS compliance validation failed: invalid order ID format");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating NAPAS compliance: {}", e.getMessage(), e);
            return false;
        }
    }
}
