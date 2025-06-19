package com.lapxpert.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Event for coordinated cache invalidation and WebSocket notifications.
 * Ensures cache invalidation occurs before WebSocket notifications are sent.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatedCacheInvalidationEvent {
    
    /**
     * Type of event (PRICE_CHANGE, INVENTORY_UPDATE, VOUCHER_CHANGE, ORDER_CHANGE)
     */
    private String eventType;
    
    /**
     * Entity ID that triggered the event
     */
    private Long entityId;
    
    /**
     * Additional event data specific to the event type
     */
    private Map<String, Object> eventData;
    
    /**
     * User who triggered the change
     */
    private String nguoiThucHien;
    
    /**
     * Reason for the change
     */
    private String lyDoThayDoi;
    
    /**
     * Timestamp when event was created
     */
    private Instant timestamp;
    
    /**
     * Priority level for event processing (HIGH, NORMAL, LOW)
     */
    @Builder.Default
    private String priority = "NORMAL";
    
    /**
     * Whether this event requires immediate cache invalidation
     */
    @Builder.Default
    private boolean requiresImmediateInvalidation = true;
    
    /**
     * Whether this event should trigger WebSocket notifications
     */
    @Builder.Default
    private boolean requiresWebSocketNotification = true;
    
    /**
     * Cache patterns to invalidate (optional, for specific cache targeting)
     */
    private String[] cachePatterns;
    
    /**
     * WebSocket topics to notify (optional, for specific topic targeting)
     */
    private String[] webSocketTopics;
    
    /**
     * Create a price change coordinated event
     */
    public static CoordinatedCacheInvalidationEvent createPriceChangeEvent(Long variantId, Map<String, Object> priceData, String nguoiThucHien) {
        return CoordinatedCacheInvalidationEvent.builder()
                .eventType("PRICE_CHANGE")
                .entityId(variantId)
                .eventData(priceData)
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi("Thay đổi giá sản phẩm")
                .timestamp(Instant.now())
                .priority("HIGH")
                .cachePatterns(new String[]{"searchResults", "popularProducts"})
                .webSocketTopics(new String[]{"/topic/gia-san-pham/" + variantId})
                .build();
    }
    
    /**
     * Create an inventory update coordinated event
     */
    public static CoordinatedCacheInvalidationEvent createInventoryUpdateEvent(Long variantId, Map<String, Object> inventoryData, String nguoiThucHien) {
        return CoordinatedCacheInvalidationEvent.builder()
                .eventType("INVENTORY_UPDATE")
                .entityId(variantId)
                .eventData(inventoryData)
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi("Cập nhật tồn kho")
                .timestamp(Instant.now())
                .priority("HIGH")
                .cachePatterns(new String[]{"productData"})
                .webSocketTopics(new String[]{"/topic/ton-kho/" + variantId, "/topic/ton-kho/all"})
                .build();
    }
    
    /**
     * Create a voucher change coordinated event
     */
    public static CoordinatedCacheInvalidationEvent createVoucherChangeEvent(Long voucherId, Map<String, Object> voucherData, String nguoiThucHien) {
        return CoordinatedCacheInvalidationEvent.builder()
                .eventType("VOUCHER_CHANGE")
                .entityId(voucherId)
                .eventData(voucherData)
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi("Thay đổi voucher")
                .timestamp(Instant.now())
                .priority("NORMAL")
                .build();
    }
    
    /**
     * Create an order change coordinated event
     */
    public static CoordinatedCacheInvalidationEvent createOrderChangeEvent(Long hoaDonId, Map<String, Object> orderData, String nguoiThucHien) {
        return CoordinatedCacheInvalidationEvent.builder()
                .eventType("ORDER_CHANGE")
                .entityId(hoaDonId)
                .eventData(orderData)
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi("Thay đổi hóa đơn")
                .timestamp(Instant.now())
                .priority("HIGH")
                .cachePatterns(new String[]{"hoaDon:*"})
                .webSocketTopics(new String[]{"/topic/hoa-don/" + hoaDonId, "/topic/hoa-don/all"})
                .build();
    }
    
    /**
     * Check if this is a high priority event
     */
    public boolean isHighPriority() {
        return "HIGH".equals(priority);
    }
    
    /**
     * Check if this is a low priority event
     */
    public boolean isLowPriority() {
        return "LOW".equals(priority);
    }
    
    /**
     * Get event age in milliseconds
     */
    public long getEventAgeMs() {
        return Instant.now().toEpochMilli() - timestamp.toEpochMilli();
    }
}
