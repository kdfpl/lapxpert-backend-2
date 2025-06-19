package com.lapxpert.backend.giohang.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for applied discounts and vouchers in cart conversion
 * Tracks discount information for order creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedDiscountDto {

    /**
     * Discount identification
     */
    private Long discountId;
    private String discountType; // "VOUCHER", "CAMPAIGN", "PRODUCT_DISCOUNT"
    private String discountCode;
    private String discountName;

    /**
     * Discount amounts
     */
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private BigDecimal maxDiscountAmount;

    /**
     * Application scope
     */
    private String applicableScope; // "ORDER", "PRODUCT", "CATEGORY"
    private List<Long> applicableProductIds;

    /**
     * Discount status
     */
    private boolean isApplied;
    private String applicationMessage;

    /**
     * Check if discount is percentage-based
     * @return true if discount uses percentage calculation
     */
    public boolean isPercentageDiscount() {
        return discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if discount is fixed amount
     * @return true if discount uses fixed amount
     */
    public boolean isFixedAmountDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
