package com.lapxpert.backend.common.event;

import com.lapxpert.backend.common.cache.CacheInvalidationService;
import com.lapxpert.backend.sanpham.event.PriceChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event listener for automatic cache invalidation and WebSocket broadcasting.
 * Handles all business entity change events to maintain real-time consistency.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationEventListener {

    private final CacheInvalidationService cacheInvalidationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== PRICE CHANGE EVENTS ====================

    /**
     * Handle price change events - already implemented in PriceChangeNotificationService
     * This listener focuses on additional cache invalidation beyond what's already done
     */
    @EventListener
    @Transactional
    public void handlePriceChangeEvent(PriceChangeEvent event) {
        try {
            log.debug("Processing additional cache invalidation for price change: variant {}", event.getVariantId());
            
            // Invalidate search results that might contain this product
            cacheInvalidationService.invalidateCache("searchResults");
            
            // Invalidate popular products if this might affect rankings
            cacheInvalidationService.invalidateCache("popularProducts");
            
            log.debug("Additional cache invalidation completed for price change: variant {}", event.getVariantId());
            
        } catch (Exception e) {
            log.error("Failed to process additional cache invalidation for price change: {}", e.getMessage(), e);
        }
    }

    // ==================== INVENTORY UPDATE EVENTS ====================

    /**
     * Handle inventory update events
     */
    @EventListener
    @Transactional
    public void handleInventoryUpdateEvent(InventoryUpdateEvent event) {
        try {
            log.info("Processing inventory update event for variant {}: {} -> {}", 
                event.getVariantId(), event.getSoLuongTonKhoCu(), event.getSoLuongTonKhoMoi());
            
            // Invalidate product-related caches
            cacheInvalidationService.invalidateProductData(null); // Invalidate all product caches
            
            // Send WebSocket notification for inventory changes
            sendInventoryUpdateNotification(event);
            
            log.info("Inventory update event processing completed for variant {}", event.getVariantId());
            
        } catch (Exception e) {
            log.error("Failed to process inventory update event for variant {}: {}", 
                event.getVariantId(), e.getMessage(), e);
        }
    }

    // ==================== VOUCHER CHANGE EVENTS ====================

    /**
     * Handle voucher change events
     */
    @EventListener
    @Transactional
    public void handleVoucherChangeEvent(VoucherChangeEvent event) {
        try {
            log.info("Processing voucher change event for voucher {}: {}", 
                event.getVoucherId(), event.getLoaiThayDoi());
            
            // Invalidate voucher-related caches
            if ("PHIEU_GIAM_GIA".equals(event.getLoaiVoucher())) {
                cacheInvalidationService.invalidateByPattern("phieuGiamGia:*");
            } else if ("DOT_GIAM_GIA".equals(event.getLoaiVoucher())) {
                cacheInvalidationService.invalidateByPattern("dotGiamGia:*");
            }
            
            // Invalidate cart data if voucher status changed (affects pricing)
            if (event.hasStatusChanged()) {
                cacheInvalidationService.invalidateCache("cartData");
            }
            
            // Send WebSocket notification for voucher changes
            sendVoucherChangeNotification(event);
            
            log.info("Voucher change event processing completed for voucher {}", event.getVoucherId());
            
        } catch (Exception e) {
            log.error("Failed to process voucher change event for voucher {}: {}", 
                event.getVoucherId(), e.getMessage(), e);
        }
    }

    // ==================== ORDER CHANGE EVENTS ====================

    /**
     * Handle order change events
     */
    @EventListener
    @Transactional
    public void handleOrderChangeEvent(OrderChangeEvent event) {
        try {
            log.info("Processing order change event for order {}: {}", 
                event.getHoaDonId(), event.getLoaiThayDoi());
            
            // Invalidate order-related caches
            cacheInvalidationService.invalidateByPattern("hoaDon:*");
            
            // If order completed or cancelled, might affect product popularity
            if (event.isOrderCompleted() || event.isOrderCancelled()) {
                cacheInvalidationService.invalidateCache("popularProducts");
            }
            
            // Send WebSocket notification for order changes
            sendOrderChangeNotification(event);
            
            log.info("Order change event processing completed for order {}", event.getHoaDonId());
            
        } catch (Exception e) {
            log.error("Failed to process order change event for order {}: {}", 
                event.getHoaDonId(), e.getMessage(), e);
        }
    }

    // ==================== WEBSOCKET NOTIFICATION METHODS ====================

    /**
     * Send WebSocket notification for inventory updates
     */
    private void sendInventoryUpdateNotification(InventoryUpdateEvent event) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "INVENTORY_UPDATE");
            notification.put("variantId", event.getVariantId());
            notification.put("sku", event.getSku());
            notification.put("tenSanPham", event.getTenSanPham());
            notification.put("soLuongTonKhoMoi", event.getSoLuongTonKhoMoi());
            notification.put("loaiThayDoi", event.getLoaiThayDoi());
            notification.put("isOutOfStock", event.isOutOfStock());
            notification.put("isBackInStock", event.isBackInStock());
            notification.put("timestamp", Instant.now());
            
            // Send to specific variant topic
            messagingTemplate.convertAndSend("/topic/ton-kho/" + event.getVariantId(), notification);
            
            // Send to general inventory topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/ton-kho/all", notification);
            
            log.debug("Sent inventory update WebSocket notification for variant {}", event.getVariantId());
            
        } catch (Exception e) {
            log.error("Failed to send inventory update WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send WebSocket notification for voucher changes
     */
    private void sendVoucherChangeNotification(VoucherChangeEvent event) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "VOUCHER_CHANGE");
            notification.put("voucherId", event.getVoucherId());
            notification.put("maVoucher", event.getMaVoucher());
            notification.put("tenVoucher", event.getTenVoucher());
            notification.put("loaiVoucher", event.getLoaiVoucher());
            notification.put("trangThaiMoi", event.getTrangThaiMoi());
            notification.put("loaiThayDoi", event.getLoaiThayDoi());
            notification.put("isVoucherActivated", event.isVoucherActivated());
            notification.put("isVoucherDeactivated", event.isVoucherDeactivated());
            notification.put("timestamp", Instant.now());
            
            // Send to voucher-specific topic
            String topicPrefix = "PHIEU_GIAM_GIA".equals(event.getLoaiVoucher()) ? "phieu-giam-gia" : "dot-giam-gia";
            messagingTemplate.convertAndSend("/topic/" + topicPrefix + "/" + event.getVoucherId(), notification);
            
            // Send to general voucher topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/voucher/all", notification);
            
            log.debug("Sent voucher change WebSocket notification for voucher {}", event.getVoucherId());
            
        } catch (Exception e) {
            log.error("Failed to send voucher change WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send WebSocket notification for order changes
     */
    private void sendOrderChangeNotification(OrderChangeEvent event) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "ORDER_CHANGE");
            notification.put("hoaDonId", event.getHoaDonId());
            notification.put("maHoaDon", event.getMaHoaDon());
            notification.put("khachHangId", event.getKhachHangId());
            notification.put("trangThaiMoi", event.getTrangThaiMoi());
            notification.put("loaiThayDoi", event.getLoaiThayDoi());
            notification.put("isOrderCompleted", event.isOrderCompleted());
            notification.put("isOrderCancelled", event.isOrderCancelled());
            notification.put("isPendingPayment", event.isPendingPayment());
            notification.put("timestamp", Instant.now());
            
            // Send to order-specific topic
            messagingTemplate.convertAndSend("/topic/hoa-don/" + event.getHoaDonId(), notification);
            
            // Send to general order topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/hoa-don/all", notification);
            
            // Send to customer-specific topic if customer exists
            if (event.getKhachHangId() != null) {
                messagingTemplate.convertAndSend("/topic/khach-hang/" + event.getKhachHangId() + "/orders", notification);
            }
            
            log.debug("Sent order change WebSocket notification for order {}", event.getHoaDonId());
            
        } catch (Exception e) {
            log.error("Failed to send order change WebSocket notification: {}", e.getMessage(), e);
        }
    }
}
