package com.lapxpert.backend.sanpham.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when a product variant price changes.
 * Used for real-time notifications and audit trail creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangeEvent {
    
    /**
     * Product variant ID
     */
    private Long variantId;
    
    /**
     * Product variant SKU for identification
     */
    private String sku;
    
    /**
     * Product name for display
     */
    private String productName;
    
    /**
     * Old price before change
     */
    private BigDecimal oldPrice;
    
    /**
     * New price after change
     */
    private BigDecimal newPrice;
    
    /**
     * Old promotional price (if any)
     */
    private BigDecimal oldPromotionalPrice;
    
    /**
     * New promotional price (if any)
     */
    private BigDecimal newPromotionalPrice;
    
    /**
     * User who made the change
     */
    private String nguoiThucHien;
    
    /**
     * Reason for price change
     */
    private String lyDoThayDoi;
    
    /**
     * Timestamp when change occurred
     */
    private Instant timestamp;
    
    /**
     * Check if regular price changed
     */
    public boolean hasRegularPriceChanged() {
        if (oldPrice == null && newPrice == null) return false;
        if (oldPrice == null || newPrice == null) return true;
        return oldPrice.compareTo(newPrice) != 0;
    }
    
    /**
     * Check if promotional price changed
     */
    public boolean hasPromotionalPriceChanged() {
        if (oldPromotionalPrice == null && newPromotionalPrice == null) return false;
        if (oldPromotionalPrice == null || newPromotionalPrice == null) return true;
        return oldPromotionalPrice.compareTo(newPromotionalPrice) != 0;
    }
    
    /**
     * Get effective old price (promotional if available, otherwise regular)
     */
    public BigDecimal getEffectiveOldPrice() {
        return oldPromotionalPrice != null ? oldPromotionalPrice : oldPrice;
    }
    
    /**
     * Get effective new price (promotional if available, otherwise regular)
     */
    public BigDecimal getEffectiveNewPrice() {
        return newPromotionalPrice != null ? newPromotionalPrice : newPrice;
    }
    
    /**
     * Check if any price changed
     */
    public boolean hasPriceChanged() {
        return hasRegularPriceChanged() || hasPromotionalPriceChanged();
    }
}
