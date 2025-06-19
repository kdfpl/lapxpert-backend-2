package com.lapxpert.backend.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GHN Province API response DTO
 * Maps to GHN API master-data/province endpoint response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNProvinceResponse {
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private List<GHNProvince> data;
    
    /**
     * Check if the response indicates success
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNProvince {
        
        @JsonProperty("ProvinceID")
        private Integer provinceId;
        
        @JsonProperty("ProvinceName")
        private String provinceName;
        
        @JsonProperty("CountryID")
        private Integer countryId;
        
        @JsonProperty("Code")
        private String code;
    }
}
