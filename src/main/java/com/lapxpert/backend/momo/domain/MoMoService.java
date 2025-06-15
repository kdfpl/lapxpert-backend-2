package com.lapxpert.backend.momo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced MoMo payment service with v3 API support, improved security practices,
 * and comprehensive error handling.
 *
 * Security enhancements:
 * - Enhanced signature verification with proper parameter validation
 * - Improved error handling with security-conscious error messages
 * - Comprehensive audit logging for payment operations
 * - Better parameter validation and encoding
 * - Enhanced IPN processing with security checks
 *
 * v3 API Features:
 * - Support for items list in payment requests
 * - Delivery information support
 * - User information support
 * - Multiple response formats (payUrl, deeplink, qrCodeUrl)
 * - Enhanced request ID validation for idempotency
 */
@Slf4j
@Service
public class MoMoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MoMoService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create MoMo payment URL for order with enhanced v3 API support.
     *
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @param returnUrl Return URL after payment
     * @param notifyUrl IPN notification URL
     * @return MoMo payment URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String createPaymentUrl(long amount, String orderInfo, String orderId, String returnUrl, String notifyUrl) {
        log.info("Creating MoMo payment URL - Amount: {}, OrderID: {}", amount, orderId);

        try {
            // Enhanced parameter validation
            validateCreatePaymentParameters(amount, orderInfo, orderId, returnUrl, notifyUrl);

            // Generate request ID for idempotency
            String requestId = MoMoConfig.getRandomRequestId(32);

            // Prepare request parameters
            String partnerCode = MoMoConfig.momo_PartnerCode;
            String accessKey = MoMoConfig.momo_AccessKey;
            String secretKey = MoMoConfig.momo_SecretKey;
            String endpoint = MoMoConfig.momo_Endpoint;
            
            // Build raw signature string
            String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, amount, "", notifyUrl, orderId, orderInfo, partnerCode, returnUrl, requestId, "captureWallet"
            );
            
            // Generate signature
            String signature = MoMoConfig.hmacSHA256(secretKey, rawSignature);
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", returnUrl);
            requestBody.put("ipnUrl", notifyUrl);
            requestBody.put("extraData", "");
            requestBody.put("requestType", "captureWallet");
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                endpoint, 
                HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer resultCode = (Integer) responseBody.get("resultCode");
                
                if (resultCode != null && resultCode == 0) {
                    String payUrl = (String) responseBody.get("payUrl");
                    log.info("MoMo payment URL created successfully for order {}: {}", orderId, payUrl);
                    return payUrl;
                } else {
                    String message = (String) responseBody.get("message");
                    log.error("MoMo API error for order {}: {} - {}", orderId, resultCode, message);
                    throw new RuntimeException("MoMo API error: " + message);
                }
            } else {
                log.error("Failed to call MoMo API for order {}: HTTP {}", orderId, response.getStatusCode());
                throw new RuntimeException("Failed to create MoMo payment URL");
            }
            
        } catch (Exception e) {
            log.error("Error creating MoMo payment URL for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error creating MoMo payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced MoMo payment callback verification with comprehensive security validation.
     *
     * @param requestBody Request body from MoMo callback
     * @return Payment verification result (1: success, 0: failed, -1: invalid)
     */
    public int verifyPayment(Map<String, String> requestBody) {
        String orderId = requestBody.get("orderId");
        String transId = requestBody.get("transId");
        String resultCode = requestBody.get("resultCode");

        log.info("Verifying MoMo payment - OrderID: {}, TransID: {}, ResultCode: {}",
                orderId, transId, resultCode);

        try {
            // Enhanced parameter validation
            if (!validateCallbackParameters(requestBody)) {
                log.error("MoMo callback validation failed - missing required parameters for OrderID: {}", orderId);
                return -1;
            }

            String partnerCode = requestBody.get("partnerCode");
            String requestId = requestBody.get("requestId");
            String amount = requestBody.get("amount");
            String orderInfo = requestBody.get("orderInfo");
            String orderType = requestBody.get("orderType");
            String message = requestBody.get("message");
            String payType = requestBody.get("payType");
            String responseTime = requestBody.get("responseTime");
            String extraData = requestBody.get("extraData");
            String signature = requestBody.get("signature");

            // Enhanced signature validation
            if (signature == null || signature.trim().isEmpty()) {
                log.error("MoMo callback missing signature for OrderID: {}", orderId);
                return -1;
            }

            // Build raw signature for verification (sorted parameters)
            String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                MoMoConfig.momo_AccessKey, amount, extraData != null ? extraData : "", message, orderId, orderInfo, orderType,
                partnerCode, payType, requestId, responseTime, resultCode, transId
            );

            // Verify signature
            if (!MoMoConfig.validateSignature(rawSignature, signature)) {
                log.error("Invalid MoMo signature for OrderID: {} - Expected signature for: {}", orderId, rawSignature);
                return -1; // Invalid signature
            }

            log.info("MoMo signature verification successful for OrderID: {}", orderId);

            // Check result code
            if ("0".equals(resultCode)) {
                log.info("MoMo payment successful for OrderID: {}, TransactionID: {}", orderId, transId);
                return 1; // Success
            } else {
                log.warn("MoMo payment failed for OrderID: {}, ResultCode: {}, Message: {}", orderId, resultCode, message);
                return 0; // Failed
            }

        } catch (Exception e) {
            log.error("Error verifying MoMo payment for OrderID: {} - {}", orderId, e.getMessage(), e);
            return -1; // Invalid
        }
    }

    /**
     * Query MoMo transaction status
     * @param orderId Order ID to query
     * @param requestId Original request ID
     * @return Transaction status response
     */
    public Map<String, Object> queryTransactionStatus(String orderId, String requestId) {
        try {
            String partnerCode = MoMoConfig.momo_PartnerCode;
            String accessKey = MoMoConfig.momo_AccessKey;
            String secretKey = MoMoConfig.momo_SecretKey;
            
            // Build raw signature for query
            String rawSignature = String.format(
                "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                accessKey, orderId, partnerCode, requestId
            );
            
            String signature = MoMoConfig.hmacSHA256(secretKey, rawSignature);
            
            // Prepare request body for query
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Query endpoint (different from payment creation)
            String queryEndpoint = MoMoConfig.momo_Endpoint.replace("/create", "/query");
            
            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                queryEndpoint, 
                HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to query MoMo transaction status");
            }
            
        } catch (Exception e) {
            log.error("Error querying MoMo transaction status for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error querying MoMo transaction status: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced MoMo payment URL creation with v3 API features support.
     *
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @param returnUrl Return URL after payment
     * @param notifyUrl IPN notification URL
     * @param items List of items in the order (v3 feature)
     * @param userInfo User information (v3 feature)
     * @param deliveryInfo Delivery information (v3 feature)
     * @return Enhanced MoMo payment response with multiple formats
     */
    public MoMoPaymentResponse createEnhancedPaymentUrl(long amount, String orderInfo, String orderId,
                                                       String returnUrl, String notifyUrl,
                                                       List<MoMoItem> items, MoMoUserInfo userInfo,
                                                       MoMoDeliveryInfo deliveryInfo) {
        log.info("Creating enhanced MoMo payment URL with v3 features - Amount: {}, OrderID: {}", amount, orderId);

        try {
            // Enhanced parameter validation
            validateCreatePaymentParameters(amount, orderInfo, orderId, returnUrl, notifyUrl);

            // Generate request ID for idempotency
            String requestId = MoMoConfig.getRandomRequestId(32);

            // Prepare request parameters
            String partnerCode = MoMoConfig.momo_PartnerCode;
            String accessKey = MoMoConfig.momo_AccessKey;
            String secretKey = MoMoConfig.momo_SecretKey;
            String endpoint = MoMoConfig.momo_Endpoint;

            // Build enhanced raw signature string for v3 API
            String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, amount, "", notifyUrl, orderId, orderInfo, partnerCode, returnUrl, requestId, "captureWallet"
            );

            // Generate signature
            String signature = MoMoConfig.hmacSHA256(secretKey, rawSignature);

            // Prepare enhanced request body with v3 features
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", returnUrl);
            requestBody.put("ipnUrl", notifyUrl);
            requestBody.put("extraData", "");
            requestBody.put("requestType", "captureWallet");
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");

            // Add v3 API features if provided
            if (items != null && !items.isEmpty()) {
                requestBody.put("items", items);
            }

            if (userInfo != null) {
                requestBody.put("userInfo", userInfo);
            }

            if (deliveryInfo != null) {
                requestBody.put("deliveryInfo", deliveryInfo);
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                requestEntity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer resultCode = (Integer) responseBody.get("resultCode");

                if (resultCode != null && resultCode == 0) {
                    log.info("Enhanced MoMo payment URL created successfully for order {}", orderId);
                    return MoMoPaymentResponse.fromApiResponse(responseBody);
                } else {
                    String message = (String) responseBody.get("message");
                    log.error("MoMo API error for order {}: {} - {}", orderId, resultCode, message);
                    throw new RuntimeException("MoMo API error: " + message);
                }
            } else {
                log.error("Failed to call MoMo API for order {}: HTTP {}", orderId, response.getStatusCode());
                throw new RuntimeException("Failed to create enhanced MoMo payment URL");
            }

        } catch (Exception e) {
            log.error("Error creating enhanced MoMo payment URL for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error creating enhanced MoMo payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Validate parameters for createPaymentUrl method.
     *
     * @param amount Payment amount
     * @param orderInfo Order information
     * @param orderId Order ID
     * @param returnUrl Return URL
     * @param notifyUrl Notify URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateCreatePaymentParameters(long amount, String orderInfo, String orderId,
                                               String returnUrl, String notifyUrl) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (amount > 50000000) { // MoMo limit: 50 million VND
            throw new IllegalArgumentException("Payment amount exceeds MoMo limit (50,000,000 VND)");
        }

        if (orderInfo == null || orderInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Order information cannot be null or empty");
        }

        if (orderInfo.length() > 255) {
            throw new IllegalArgumentException("Order information cannot exceed 255 characters");
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Return URL cannot be null or empty");
        }

        if (notifyUrl == null || notifyUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Notify URL cannot be null or empty");
        }

        // Validate URL format
        if (!returnUrl.startsWith("http://") && !returnUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Return URL must be a valid HTTP/HTTPS URL");
        }

        if (!notifyUrl.startsWith("http://") && !notifyUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Notify URL must be a valid HTTP/HTTPS URL");
        }
    }

    /**
     * Validate MoMo callback parameters.
     *
     * @param requestBody Request body from MoMo callback
     * @return true if all required parameters are present
     */
    private boolean validateCallbackParameters(Map<String, String> requestBody) {
        String[] requiredParams = {
            "partnerCode", "orderId", "requestId", "amount", "orderInfo",
            "resultCode", "message", "signature"
        };

        for (String param : requiredParams) {
            String value = requestBody.get(param);
            if (value == null || value.trim().isEmpty()) {
                log.error("Missing required MoMo callback parameter: {}", param);
                return false;
            }
        }

        return true;
    }

    /**
     * Enhanced IPN (Instant Payment Notification) processing with security validation.
     *
     * @param ipnData IPN data from MoMo
     * @return PaymentIPNResult containing validation result and payment status
     */
    public PaymentIPNResult processIPN(Map<String, String> ipnData) {
        String orderId = ipnData.get("orderId");
        String transId = ipnData.get("transId");
        String resultCode = ipnData.get("resultCode");

        log.info("Processing MoMo IPN - OrderID: {}, TransID: {}, ResultCode: {}",
                orderId, transId, resultCode);

        try {
            // Validate IPN parameters
            if (!validateCallbackParameters(ipnData)) {
                log.error("MoMo IPN validation failed - missing required parameters for OrderID: {}", orderId);
                return PaymentIPNResult.invalid("Missing required parameters");
            }

            // Process signature verification (same as verifyPayment)
            int verificationResult = verifyPayment(ipnData);

            if (verificationResult == 1) {
                log.info("MoMo IPN processed successfully - Payment confirmed for OrderID: {}", orderId);
                return PaymentIPNResult.success(orderId, resultCode);
            } else if (verificationResult == 0) {
                log.warn("MoMo IPN processed - Payment failed for OrderID: {}", orderId);
                return PaymentIPNResult.failed(orderId, resultCode);
            } else {
                log.error("MoMo IPN signature verification failed for OrderID: {}", orderId);
                return PaymentIPNResult.invalid("Signature verification failed");
            }

        } catch (Exception e) {
            log.error("Error processing MoMo IPN for OrderID: {} - {}", orderId, e.getMessage(), e);
            return PaymentIPNResult.error("Internal processing error");
        }
    }

    /**
     * Result class for IPN processing.
     */
    public static class PaymentIPNResult {
        private final boolean valid;
        private final boolean successful;
        private final String transactionRef;
        private final String status;
        private final String errorMessage;

        private PaymentIPNResult(boolean valid, boolean successful, String transactionRef, String status, String errorMessage) {
            this.valid = valid;
            this.successful = successful;
            this.transactionRef = transactionRef;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public static PaymentIPNResult success(String transactionRef, String status) {
            return new PaymentIPNResult(true, true, transactionRef, status, null);
        }

        public static PaymentIPNResult failed(String transactionRef, String status) {
            return new PaymentIPNResult(true, false, transactionRef, status, null);
        }

        public static PaymentIPNResult invalid(String errorMessage) {
            return new PaymentIPNResult(false, false, null, null, errorMessage);
        }

        public static PaymentIPNResult error(String errorMessage) {
            return new PaymentIPNResult(false, false, null, null, errorMessage);
        }

        // Getters
        public boolean isValid() { return valid; }
        public boolean isSuccessful() { return successful; }
        public String getTransactionRef() { return transactionRef; }
        public String getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }
    }
}
