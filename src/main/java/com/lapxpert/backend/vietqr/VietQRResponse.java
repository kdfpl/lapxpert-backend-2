package com.lapxpert.backend.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Enhanced VietQR response class compliant with NAPAS standards and VietQR Version 2.13.
 * Supports both Quick Link and Full API v2 responses.
 */
public class VietQRResponse {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("desc")
    private String desc;
    
    @JsonProperty("data")
    private VietQRData data;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("qrCode")
    private String qrCode;
    
    @JsonProperty("qrDataURL")
    private String qrDataURL;
    
    @JsonProperty("quickLink")
    private String quickLink;
    
    // Default constructor
    public VietQRResponse() {
    }
    
    // Constructor for successful response
    public VietQRResponse(String code, String desc, VietQRData data, String orderId) {
        this.code = code;
        this.desc = desc;
        this.data = data;
        this.orderId = orderId;
    }
    
    /**
     * Create VietQRResponse from Full API v2 response.
     * 
     * @param apiResponse API response from VietQR Full API v2
     * @param orderId Order ID for correlation
     * @return VietQRResponse instance
     */
    public static VietQRResponse fromApiResponse(Map<String, Object> apiResponse, String orderId) {
        VietQRResponse response = new VietQRResponse();
        response.setCode((String) apiResponse.get("code"));
        response.setDesc((String) apiResponse.get("desc"));
        response.setOrderId(orderId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) apiResponse.get("data");
        if (dataMap != null) {
            VietQRData data = new VietQRData();
            data.setQrCode((String) dataMap.get("qrCode"));
            data.setQrDataURL((String) dataMap.get("qrDataURL"));
            response.setData(data);
            response.setQrCode(data.getQrCode());
            response.setQrDataURL(data.getQrDataURL());
        }
        
        return response;
    }
    
    /**
     * Create VietQRResponse from Quick Link URL.
     * 
     * @param quickLinkUrl Quick Link URL
     * @param orderId Order ID for correlation
     * @return VietQRResponse instance
     */
    public static VietQRResponse fromQuickLink(String quickLinkUrl, String orderId) {
        VietQRResponse response = new VietQRResponse();
        response.setCode("00");
        response.setDesc("Success");
        response.setOrderId(orderId);
        response.setQuickLink(quickLinkUrl);
        response.setQrDataURL(quickLinkUrl); // Quick Link serves as QR data URL
        
        return response;
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public void setDesc(String desc) {
        this.desc = desc;
    }
    
    public VietQRData getData() {
        return data;
    }
    
    public void setData(VietQRData data) {
        this.data = data;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    public String getQrDataURL() {
        return qrDataURL;
    }
    
    public void setQrDataURL(String qrDataURL) {
        this.qrDataURL = qrDataURL;
    }
    
    public String getQuickLink() {
        return quickLink;
    }
    
    public void setQuickLink(String quickLink) {
        this.quickLink = quickLink;
    }
    
    /**
     * Check if the response indicates success.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return "00".equals(code);
    }
    
    @Override
    public String toString() {
        return "VietQRResponse{" +
                "code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                ", orderId='" + orderId + '\'' +
                ", qrCode='" + (qrCode != null ? "[QR_CODE_DATA]" : null) + '\'' +
                ", qrDataURL='" + qrDataURL + '\'' +
                ", quickLink='" + quickLink + '\'' +
                '}';
    }
    
    /**
     * VietQR data structure for Full API v2 responses.
     */
    public static class VietQRData {
        
        @JsonProperty("qrCode")
        private String qrCode;
        
        @JsonProperty("qrDataURL")
        private String qrDataURL;
        
        // Default constructor
        public VietQRData() {
        }
        
        // Constructor
        public VietQRData(String qrCode, String qrDataURL) {
            this.qrCode = qrCode;
            this.qrDataURL = qrDataURL;
        }
        
        // Getters and Setters
        public String getQrCode() {
            return qrCode;
        }
        
        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }
        
        public String getQrDataURL() {
            return qrDataURL;
        }
        
        public void setQrDataURL(String qrDataURL) {
            this.qrDataURL = qrDataURL;
        }
        
        @Override
        public String toString() {
            return "VietQRData{" +
                    "qrCode='" + (qrCode != null ? "[QR_CODE_DATA]" : null) + '\'' +
                    ", qrDataURL='" + qrDataURL + '\'' +
                    '}';
        }
    }
}
