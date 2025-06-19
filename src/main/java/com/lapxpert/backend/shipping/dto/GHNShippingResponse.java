package com.lapxpert.backend.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * GHN shipping fee calculation response DTO
 * Maps to GHN API v2 shipping-order/fee endpoint response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNShippingResponse {
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private GHNShippingData data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNShippingData {
        
        @JsonProperty("total")
        private BigDecimal total;
        
        @JsonProperty("service_fee")
        private BigDecimal serviceFee;
        
        @JsonProperty("insurance_fee")
        private BigDecimal insuranceFee;
        
        @JsonProperty("pick_station_fee")
        private BigDecimal pickStationFee;
        
        @JsonProperty("coupon_value")
        private BigDecimal couponValue;
        
        @JsonProperty("r2s_fee")
        private BigDecimal r2sFee;
        
        @JsonProperty("return_again")
        private BigDecimal returnAgain;
        
        @JsonProperty("document_return")
        private BigDecimal documentReturn;
        
        @JsonProperty("double_check")
        private BigDecimal doubleCheck;
        
        @JsonProperty("cod_fee")
        private BigDecimal codFee;
        
        @JsonProperty("pick_remote_areas_fee")
        private BigDecimal pickRemoteAreasFee;
        
        @JsonProperty("deliver_remote_areas_fee")
        private BigDecimal deliverRemoteAreasFee;
        
        @JsonProperty("cod_failed_fee")
        private BigDecimal codFailedFee;
    }
    
    /**
     * Check if the response indicates success
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    /**
     * Get the main shipping fee (service_fee)
     */
    public BigDecimal getShippingFee() {
        if (data != null && data.getServiceFee() != null) {
            return data.getServiceFee();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get the total fee including all charges
     */
    public BigDecimal getTotalFee() {
        if (data != null && data.getTotal() != null) {
            return data.getTotal();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get insurance fee
     */
    public BigDecimal getInsuranceFee() {
        if (data != null && data.getInsuranceFee() != null) {
            return data.getInsuranceFee();
        }
        return BigDecimal.ZERO;
    }
}
