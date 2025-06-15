package com.lapxpert.backend.momo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MoMo payment service for handling payment URL generation and verification
 * Implements MoMo API integration following Vietnamese business requirements
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
     * Create MoMo payment URL for order
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @param returnUrl Return URL after payment
     * @param notifyUrl IPN notification URL
     * @return MoMo payment URL
     */
    public String createPaymentUrl(long amount, String orderInfo, String orderId, String returnUrl, String notifyUrl) {
        try {
            // Generate request ID
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
     * Verify MoMo payment callback
     * @param requestBody Request body from MoMo callback
     * @return Payment verification result (1: success, 0: failed, -1: invalid)
     */
    public int verifyPayment(Map<String, String> requestBody) {
        try {
            String partnerCode = requestBody.get("partnerCode");
            String orderId = requestBody.get("orderId");
            String requestId = requestBody.get("requestId");
            String amount = requestBody.get("amount");
            String orderInfo = requestBody.get("orderInfo");
            String orderType = requestBody.get("orderType");
            String transId = requestBody.get("transId");
            String resultCode = requestBody.get("resultCode");
            String message = requestBody.get("message");
            String payType = requestBody.get("payType");
            String responseTime = requestBody.get("responseTime");
            String extraData = requestBody.get("extraData");
            String signature = requestBody.get("signature");
            
            // Build raw signature for verification
            String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                MoMoConfig.momo_AccessKey, amount, extraData, message, orderId, orderInfo, orderType, 
                partnerCode, payType, requestId, responseTime, resultCode, transId
            );
            
            // Verify signature
            if (!MoMoConfig.validateSignature(rawSignature, signature)) {
                log.error("Invalid MoMo signature for order {}", orderId);
                return -1; // Invalid signature
            }
            
            // Check result code
            if ("0".equals(resultCode)) {
                log.info("MoMo payment successful for order {}, transaction ID: {}", orderId, transId);
                return 1; // Success
            } else {
                log.warn("MoMo payment failed for order {}: {} - {}", orderId, resultCode, message);
                return 0; // Failed
            }
            
        } catch (Exception e) {
            log.error("Error verifying MoMo payment: {}", e.getMessage(), e);
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
}
