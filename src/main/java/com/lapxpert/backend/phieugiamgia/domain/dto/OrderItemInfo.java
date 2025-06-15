package com.lapxpert.backend.phieugiamgia.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemInfo {
    private Long productId;
    private String productName;
    private String category;
    private String brand;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal totalPrice;
}
