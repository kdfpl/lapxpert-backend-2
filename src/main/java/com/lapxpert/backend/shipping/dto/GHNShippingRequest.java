package com.lapxpert.backend.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * GHN shipping fee calculation request DTO
 * Maps to GHN API v2 shipping-order/fee endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNShippingRequest {
    
    @JsonProperty("service_id")
    private Integer serviceId;
    
    @JsonProperty("service_type_id")
    private Integer serviceTypeId;
    
    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    
    @JsonProperty("to_ward_code")
    private String toWardCode;
    
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;
    
    @JsonProperty("from_ward_code")
    private String fromWardCode;
    
    @JsonProperty("weight")
    private Integer weight; // in grams
    
    @JsonProperty("length")
    private Integer length; // in cm
    
    @JsonProperty("width")
    private Integer width; // in cm
    
    @JsonProperty("height")
    private Integer height; // in cm
    
    @JsonProperty("insurance_value")
    private BigDecimal insuranceValue;
    
    @JsonProperty("cod_failed_amount")
    private BigDecimal codFailedAmount;
    
    @JsonProperty("coupon")
    private String coupon;
    
    @JsonProperty("items")
    private List<GHNItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNItem {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("height")
        private Integer height;
        
        @JsonProperty("weight")
        private Integer weight;
        
        @JsonProperty("length")
        private Integer length;
        
        @JsonProperty("width")
        private Integer width;
    }
}
