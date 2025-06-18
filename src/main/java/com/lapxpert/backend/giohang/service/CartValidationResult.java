package com.lapxpert.backend.giohang.service;

import com.lapxpert.backend.giohang.entity.GioHangChiTiet;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result class for cart validation during cart-to-order conversion
 * Contains information about validation issues and calculated totals
 */
@Data
public class CartValidationResult {
    
    private List<GioHangChiTiet> unavailableItems = new ArrayList<>();
    private Map<GioHangChiTiet, BigDecimal> priceChangedItems = new HashMap<>();
    private BigDecimal calculatedTotal = BigDecimal.ZERO;
    private boolean totalMismatch = false;
    private BigDecimal expectedTotal;
    private BigDecimal providedTotal;
    
    /**
     * Add an unavailable item to the validation result
     * @param cartItem the unavailable cart item
     */
    public void addUnavailableItem(GioHangChiTiet cartItem) {
        unavailableItems.add(cartItem);
    }
    
    /**
     * Add a price changed item to the validation result
     * @param cartItem the cart item with price change
     * @param currentPrice the current price of the item
     */
    public void addPriceChangedItem(GioHangChiTiet cartItem, BigDecimal currentPrice) {
        priceChangedItems.put(cartItem, currentPrice);
    }
    
    /**
     * Check if there are any unavailable items
     * @return true if there are unavailable items
     */
    public boolean hasUnavailableItems() {
        return !unavailableItems.isEmpty();
    }
    
    /**
     * Check if there are any price changes
     * @return true if there are price changes
     */
    public boolean hasPriceChanges() {
        return !priceChangedItems.isEmpty();
    }
    
    /**
     * Check if validation passed without any issues
     * @return true if no validation issues found
     */
    public boolean isValid() {
        return !hasUnavailableItems() && !hasPriceChanges() && !totalMismatch;
    }
    
    /**
     * Get count of unavailable items
     * @return number of unavailable items
     */
    public int getUnavailableItemCount() {
        return unavailableItems.size();
    }
    
    /**
     * Get count of items with price changes
     * @return number of items with price changes
     */
    public int getPriceChangedItemCount() {
        return priceChangedItems.size();
    }
    
    /**
     * Get total price difference due to price changes
     * @return total price difference (positive if prices increased, negative if decreased)
     */
    public BigDecimal getTotalPriceDifference() {
        BigDecimal totalDifference = BigDecimal.ZERO;
        
        for (Map.Entry<GioHangChiTiet, BigDecimal> entry : priceChangedItems.entrySet()) {
            GioHangChiTiet cartItem = entry.getKey();
            BigDecimal currentPrice = entry.getValue();
            BigDecimal cartPrice = cartItem.getGiaTaiThoiDiemThem();
            BigDecimal itemDifference = currentPrice.subtract(cartPrice).multiply(BigDecimal.valueOf(cartItem.getSoLuong()));
            totalDifference = totalDifference.add(itemDifference);
        }
        
        return totalDifference;
    }
    
    /**
     * Get validation summary as a human-readable string
     * @return validation summary
     */
    public String getValidationSummary() {
        if (isValid()) {
            return "Cart validation passed successfully";
        }
        
        StringBuilder summary = new StringBuilder("Cart validation issues found:");
        
        if (hasUnavailableItems()) {
            summary.append(String.format("\n- %d unavailable items", getUnavailableItemCount()));
        }
        
        if (hasPriceChanges()) {
            BigDecimal priceDiff = getTotalPriceDifference();
            summary.append(String.format("\n- %d items with price changes (total difference: %s)", 
                    getPriceChangedItemCount(), priceDiff));
        }
        
        if (totalMismatch) {
            summary.append(String.format("\n- Total amount mismatch (expected: %s, provided: %s)", 
                    expectedTotal, providedTotal));
        }
        
        return summary.toString();
    }
}
