package com.lapxpert.backend.momo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MoMo v3 API Payment Response class for enhanced payment requests.
 * Contains multiple payment URL formats and response information.
 */
public class MoMoPaymentResponse {
    
    @JsonProperty("partnerCode")
    private String partnerCode;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("responseTime")
    private Long responseTime;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("resultCode")
    private Integer resultCode;
    
    @JsonProperty("payUrl")
    private String payUrl;
    
    @JsonProperty("deeplink")
    private String deeplink;
    
    @JsonProperty("qrCodeUrl")
    private String qrCodeUrl;
    
    @JsonProperty("deeplinkMiniApp")
    private String deeplinkMiniApp;
    
    // Default constructor
    public MoMoPaymentResponse() {
    }
    
    // Constructor with essential fields
    public MoMoPaymentResponse(String partnerCode, String orderId, String requestId, Long amount, 
                              Integer resultCode, String message, String payUrl) {
        this.partnerCode = partnerCode;
        this.orderId = orderId;
        this.requestId = requestId;
        this.amount = amount;
        this.resultCode = resultCode;
        this.message = message;
        this.payUrl = payUrl;
    }
    
    /**
     * Create MoMoPaymentResponse from API response map
     */
    public static MoMoPaymentResponse fromApiResponse(Map<String, Object> responseBody) {
        MoMoPaymentResponse response = new MoMoPaymentResponse();
        
        response.partnerCode = (String) responseBody.get("partnerCode");
        response.orderId = (String) responseBody.get("orderId");
        response.requestId = (String) responseBody.get("requestId");
        response.amount = responseBody.get("amount") != null ? 
            Long.valueOf(responseBody.get("amount").toString()) : null;
        response.responseTime = responseBody.get("responseTime") != null ? 
            Long.valueOf(responseBody.get("responseTime").toString()) : null;
        response.message = (String) responseBody.get("message");
        response.resultCode = (Integer) responseBody.get("resultCode");
        response.payUrl = (String) responseBody.get("payUrl");
        response.deeplink = (String) responseBody.get("deeplink");
        response.qrCodeUrl = (String) responseBody.get("qrCodeUrl");
        response.deeplinkMiniApp = (String) responseBody.get("deeplinkMiniApp");
        
        return response;
    }
    
    // Getters and Setters
    public String getPartnerCode() {
        return partnerCode;
    }
    
    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public Long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getResultCode() {
        return resultCode;
    }
    
    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }
    
    public String getPayUrl() {
        return payUrl;
    }
    
    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }
    
    public String getDeeplink() {
        return deeplink;
    }
    
    public void setDeeplink(String deeplink) {
        this.deeplink = deeplink;
    }
    
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }
    
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
    
    public String getDeeplinkMiniApp() {
        return deeplinkMiniApp;
    }
    
    public void setDeeplinkMiniApp(String deeplinkMiniApp) {
        this.deeplinkMiniApp = deeplinkMiniApp;
    }
    
    /**
     * Check if the payment response indicates success
     */
    public boolean isSuccess() {
        return resultCode != null && resultCode == 0;
    }
    
    /**
     * Get the primary payment URL (payUrl is preferred)
     */
    public String getPrimaryPaymentUrl() {
        if (payUrl != null && !payUrl.trim().isEmpty()) {
            return payUrl;
        } else if (deeplink != null && !deeplink.trim().isEmpty()) {
            return deeplink;
        } else if (qrCodeUrl != null && !qrCodeUrl.trim().isEmpty()) {
            return qrCodeUrl;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "MoMoPaymentResponse{" +
                "partnerCode='" + partnerCode + '\'' +
                ", orderId='" + orderId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", amount=" + amount +
                ", resultCode=" + resultCode +
                ", message='" + message + '\'' +
                ", payUrl='" + payUrl + '\'' +
                ", deeplink='" + deeplink + '\'' +
                ", qrCodeUrl='" + qrCodeUrl + '\'' +
                '}';
    }
}
