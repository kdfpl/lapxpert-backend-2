package com.lapxpert.backend.giohang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for cart-level serial number reservations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartReservationRequest {
    
    @NotNull(message = "Product variant ID is required")
    private Long sanPhamChiTietId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer soLuong;
    
    @NotNull(message = "Tab ID is required")
    private String tabId;
    
    /**
     * Optional: Specific serial numbers to reserve
     * If not provided, system will auto-select available serial numbers
     */
    private java.util.List<String> serialNumbers;
}
