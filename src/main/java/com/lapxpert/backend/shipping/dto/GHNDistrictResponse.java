package com.lapxpert.backend.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GHN District API response DTO
 * Maps to GHN API master-data/district endpoint response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNDistrictResponse {
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private List<GHNDistrict> data;
    
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
    public static class GHNDistrict {
        
        @JsonProperty("DistrictID")
        private Integer districtId;
        
        @JsonProperty("ProvinceID")
        private Integer provinceId;
        
        @JsonProperty("DistrictName")
        private String districtName;
        
        @JsonProperty("Code")
        private String code;
        
        @JsonProperty("Type")
        private Integer type;
        
        @JsonProperty("SupportType")
        private Integer supportType;
    }
}
