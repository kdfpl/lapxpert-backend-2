package com.lapxpert.backend.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GHN Ward API response DTO
 * Maps to GHN API master-data/ward endpoint response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNWardResponse {
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private List<GHNWard> data;
    
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
    public static class GHNWard {
        
        @JsonProperty("WardCode")
        private String wardCode;
        
        @JsonProperty("DistrictID")
        private Integer districtId;
        
        @JsonProperty("WardName")
        private String wardName;
        
        @JsonProperty("NameExtension")
        private List<String> nameExtension;
        
        @JsonProperty("CanUpdateCOD")
        private Boolean canUpdateCOD;
        
        @JsonProperty("SupportType")
        private Integer supportType;
        
        @JsonProperty("PickType")
        private Integer pickType;
        
        @JsonProperty("DeliverType")
        private Integer deliverType;
    }
}
