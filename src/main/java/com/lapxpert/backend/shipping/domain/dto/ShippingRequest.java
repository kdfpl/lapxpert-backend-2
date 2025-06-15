package com.lapxpert.backend.shipping.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Shipping fee calculation request DTO
 * Contains all necessary information for calculating shipping costs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRequest {
    
    // Pickup location
    private String pickAddressId;
    private String pickAddress;
    private String pickProvince;
    private String pickDistrict;
    private String pickWard;
    private String pickStreet;
    
    // Delivery location
    private String address;
    private String province;
    private String district;
    private String ward;
    private String street;
    
    // Package details
    private Integer weight; // in grams
    private BigDecimal value; // order value in VND for insurance
    private String transport; // "road" or "fly"
    private String deliverOption; // delivery service option
    
    // Additional parameters
    private String[] tags;
    
    /**
     * Generate hash code for caching purposes
     * Only includes essential fields that affect shipping calculation
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            pickProvince, pickDistrict, pickWard,
            province, district, ward,
            weight, transport, deliverOption
        );
    }
    
    /**
     * Validate required fields for GHTK API
     */
    public boolean isValid() {
        return pickProvince != null && !pickProvince.trim().isEmpty() &&
               pickDistrict != null && !pickDistrict.trim().isEmpty() &&
               province != null && !province.trim().isEmpty() &&
               district != null && !district.trim().isEmpty() &&
               weight != null && weight > 0 &&
               deliverOption != null && !deliverOption.trim().isEmpty();
    }
    
    /**
     * Get validation error message
     */
    public String getValidationError() {
        if (pickProvince == null || pickProvince.trim().isEmpty()) {
            return "Pick province is required";
        }
        if (pickDistrict == null || pickDistrict.trim().isEmpty()) {
            return "Pick district is required";
        }
        if (province == null || province.trim().isEmpty()) {
            return "Delivery province is required";
        }
        if (district == null || district.trim().isEmpty()) {
            return "Delivery district is required";
        }
        if (weight == null || weight <= 0) {
            return "Weight must be greater than 0";
        }
        if (deliverOption == null || deliverOption.trim().isEmpty()) {
            return "Deliver option is required";
        }
        return null;
    }
}
