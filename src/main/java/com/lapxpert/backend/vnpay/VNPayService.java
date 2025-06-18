package com.lapxpert.backend.vnpay;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Enhanced VNPay service implementation with improved security practices,
 * comprehensive error handling, and proper audit logging.
 *
 * Security enhancements:
 * - Enhanced signature verification with proper parameter validation
 * - Improved error handling with security-conscious error messages
 * - Comprehensive audit logging for payment operations
 * - Better IP address handling and validation
 * - Enhanced IPN processing with security checks
 */
@Slf4j
@Service
public class VNPayService {

    /**
     * Create VNPay payment URL with enhanced security and validation.
     *
     * @param total Payment amount in VND
     * @param orderInfor Order information (max 255 characters)
     * @param urlReturn Return URL after payment
     * @return VNPay payment URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String createOrder(int total, String orderInfor, String urlReturn) {
        log.info("Creating VNPay payment order - Amount: {}, OrderInfo: {}", total, orderInfor);

        // Enhanced parameter validation
        validateCreateOrderParameters(total, orderInfor, urlReturn);

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1"; // Default IP, should be overridden by createOrderWithOrderId
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(total*100));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        urlReturn += VNPayConfig.vnp_Returnurl;
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
        return paymentUrl;
    }

    /**
     * Enhanced VNPay payment return processing with comprehensive security validation.
     *
     * @param request HTTP request containing VNPay response parameters
     * @return 1 for successful payment, 0 for failed payment, -1 for invalid signature
     */
    public int orderReturn(HttpServletRequest request) {
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String clientIp = VNPayConfig.getIpAddress(request);

        log.info("Processing VNPay return - TxnRef: {}, Status: {}, ResponseCode: {}, ClientIP: {}",
                vnp_TxnRef, vnp_TransactionStatus, vnp_ResponseCode, clientIp);

        try {
            // Enhanced parameter validation
            if (!validateReturnParameters(request)) {
                log.error("VNPay return validation failed - missing required parameters for TxnRef: {}", vnp_TxnRef);
                return -1;
            }

            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.trim().isEmpty()) {
                log.error("VNPay return missing secure hash for TxnRef: {}", vnp_TxnRef);
                return -1;
            }

            // Remove hash-related fields before signature verification
            if (fields.containsKey("vnp_SecureHashType")) {
                fields.remove("vnp_SecureHashType");
            }
            if (fields.containsKey("vnp_SecureHash")) {
                fields.remove("vnp_SecureHash");
            }

            // Enhanced signature verification
            String signValue = VNPayConfig.hashAllFields(fields);
            if (signValue.equals(vnp_SecureHash)) {
                log.info("VNPay signature verification successful for TxnRef: {}", vnp_TxnRef);

                if ("00".equals(vnp_TransactionStatus)) {
                    log.info("VNPay payment successful for TxnRef: {}", vnp_TxnRef);
                    return 1;
                } else {
                    log.warn("VNPay payment failed for TxnRef: {}, Status: {}, ResponseCode: {}",
                            vnp_TxnRef, vnp_TransactionStatus, vnp_ResponseCode);
                    return 0;
                }
            } else {
                log.error("VNPay signature verification failed for TxnRef: {} - Expected: {}, Received: {}",
                         vnp_TxnRef, signValue, vnp_SecureHash);
                return -1;
            }

        } catch (Exception e) {
            log.error("Error processing VNPay return for TxnRef: {} - {}", vnp_TxnRef, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Create VNPay payment URL with specific order ID as transaction reference.
     * Enhanced version with improved security, validation, and error handling.
     *
     * @param total Payment amount in VND
     * @param orderInfo Order information (max 255 characters)
     * @param urlReturn Return URL after payment
     * @param orderId Order ID for transaction correlation
     * @param clientIp Client IP address for security logging
     * @return VNPay payment URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String createOrderWithOrderId(int total, String orderInfo, String urlReturn, String orderId, String clientIp) {
        log.info("Creating VNPay payment with OrderID - Amount: {}, OrderID: {}, ClientIP: {}",
                total, orderId, clientIp);

        // Enhanced parameter validation
        validateCreateOrderWithIdParameters(total, orderInfo, urlReturn, orderId);

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = orderId; // Use actual order ID instead of random number
        String vnp_IpAddr = (clientIp != null && !clientIp.trim().isEmpty()) ? clientIp : "127.0.0.1";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(total*100));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        urlReturn += VNPayConfig.vnp_Returnurl;
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        log.info("VNPay payment URL created successfully for OrderID: {}", orderId);
        return paymentUrl;
    }

    /**
     * Validate parameters for createOrder method.
     *
     * @param total Payment amount
     * @param orderInfo Order information
     * @param urlReturn Return URL
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateCreateOrderParameters(int total, String orderInfo, String urlReturn) {
        if (total <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (total > 999999999) { // VNPay limit
            throw new IllegalArgumentException("Payment amount exceeds VNPay limit (999,999,999 VND)");
        }

        if (orderInfo == null || orderInfo.trim().isEmpty()) {
            throw new IllegalArgumentException("Order information cannot be null or empty");
        }

        if (orderInfo.length() > 255) {
            throw new IllegalArgumentException("Order information cannot exceed 255 characters");
        }

        if (urlReturn == null || urlReturn.trim().isEmpty()) {
            throw new IllegalArgumentException("Return URL cannot be null or empty");
        }

        // Validate URL format
        if (!urlReturn.startsWith("http://") && !urlReturn.startsWith("https://")) {
            throw new IllegalArgumentException("Return URL must be a valid HTTP/HTTPS URL");
        }
    }

    /**
     * Validate parameters for createOrderWithOrderId method.
     *
     * @param total Payment amount
     * @param orderInfo Order information
     * @param urlReturn Return URL
     * @param orderId Order ID
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateCreateOrderWithIdParameters(int total, String orderInfo, String urlReturn, String orderId) {
        validateCreateOrderParameters(total, orderInfo, urlReturn);

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        // Validate order ID format (should be numeric for VNPay)
        try {
            Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Order ID must be a valid numeric value");
        }
    }

    /**
     * Validate VNPay return parameters.
     *
     * @param request HTTP request containing VNPay response
     * @return true if all required parameters are present
     */
    private boolean validateReturnParameters(HttpServletRequest request) {
        String[] requiredParams = {
            "vnp_TxnRef", "vnp_TransactionStatus", "vnp_ResponseCode",
            "vnp_SecureHash", "vnp_Amount", "vnp_TmnCode"
        };

        for (String param : requiredParams) {
            String value = request.getParameter(param);
            if (value == null || value.trim().isEmpty()) {
                log.error("Missing required VNPay parameter: {}", param);
                return false;
            }
        }

        return true;
    }

    /**
     * Enhanced IPN (Instant Payment Notification) processing with security validation.
     *
     * @param request HTTP request containing VNPay IPN data
     * @return PaymentIPNResult containing validation result and payment status
     */
    public PaymentIPNResult processIPN(HttpServletRequest request) {
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String clientIp = VNPayConfig.getIpAddress(request);

        log.info("Processing VNPay IPN - TxnRef: {}, Status: {}, ResponseCode: {}, ClientIP: {}",
                vnp_TxnRef, vnp_TransactionStatus, vnp_ResponseCode, clientIp);

        try {
            // Validate IPN parameters
            if (!validateIPNParameters(request)) {
                log.error("VNPay IPN validation failed - missing required parameters for TxnRef: {}", vnp_TxnRef);
                return PaymentIPNResult.invalid("Missing required parameters");
            }

            // Process signature verification (same as orderReturn)
            int verificationResult = orderReturn(request);

            if (verificationResult == 1) {
                log.info("VNPay IPN processed successfully - Payment confirmed for TxnRef: {}", vnp_TxnRef);
                return PaymentIPNResult.success(vnp_TxnRef, vnp_TransactionStatus);
            } else if (verificationResult == 0) {
                log.warn("VNPay IPN processed - Payment failed for TxnRef: {}", vnp_TxnRef);
                return PaymentIPNResult.failed(vnp_TxnRef, vnp_TransactionStatus);
            } else {
                log.error("VNPay IPN signature verification failed for TxnRef: {}", vnp_TxnRef);
                return PaymentIPNResult.invalid("Signature verification failed");
            }

        } catch (Exception e) {
            log.error("Error processing VNPay IPN for TxnRef: {} - {}", vnp_TxnRef, e.getMessage(), e);
            return PaymentIPNResult.error("Internal processing error");
        }
    }

    /**
     * Validate VNPay IPN parameters.
     *
     * @param request HTTP request containing VNPay IPN data
     * @return true if all required parameters are present
     */
    private boolean validateIPNParameters(HttpServletRequest request) {
        String[] requiredParams = {
            "vnp_TxnRef", "vnp_TransactionStatus", "vnp_ResponseCode",
            "vnp_SecureHash", "vnp_Amount", "vnp_TmnCode", "vnp_TransactionNo"
        };

        for (String param : requiredParams) {
            String value = request.getParameter(param);
            if (value == null || value.trim().isEmpty()) {
                log.error("Missing required VNPay IPN parameter: {}", param);
                return false;
            }
        }

        return true;
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