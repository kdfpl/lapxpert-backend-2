package com.lapxpert.backend.common.event;

import com.lapxpert.backend.common.cache.CacheInvalidationService;
import com.lapxpert.backend.sanpham.event.PriceChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event listener for automatic cache invalidation and WebSocket broadcasting with transactional coordination.
 * Handles all business entity change events to maintain real-time consistency.
 * Uses @TransactionalEventListener with AFTER_COMMIT phase to ensure cache invalidation
 * occurs after database transaction commits and before WebSocket notifications.
 * Maintains Vietnamese business terminology for LapXpert system.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationEventListener {

    private final CacheInvalidationService cacheInvalidationService;
    private final SimpMessagingTemplate messagingTemplate;

    // Event processing metrics
    private static final String METRIC_PREFIX = "cache_invalidation_event";

    /**
     * Handle coordinated cache invalidation events that require specific ordering.
     * This method ensures cache invalidation happens before WebSocket notifications.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCoordinatedCacheInvalidationEvent(CoordinatedCacheInvalidationEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing coordinated cache invalidation event: type={}, entityId={}",
                    event.getEventType(), event.getEntityId());

            // Step 1: Perform cache invalidation first
            performCacheInvalidation(event);

            // Step 2: Send WebSocket notification after cache invalidation
            sendCoordinatedWebSocketNotification(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Coordinated cache invalidation event completed: type={}, entityId={} ({}ms)",
                    event.getEventType(), event.getEntityId(), executionTime);

        } catch (Exception e) {
            log.error("Failed to process coordinated cache invalidation event: type={}, entityId={}, error={}",
                    event.getEventType(), event.getEntityId(), e.getMessage(), e);
        }
    }

    /**
     * Perform cache invalidation based on event type and data with WebSocket coordination.
     * Vietnamese Business Context: Thực hiện vô hiệu hóa cache với điều phối WebSocket
     */
    private void performCacheInvalidation(CoordinatedCacheInvalidationEvent event) {
        try {
            log.debug("Performing coordinated cache invalidation for event type: {}", event.getEventType());

            // Step 1: Perform cache invalidation with metadata collection
            Map<String, Object> invalidationMetadata = performCacheInvalidationWithMetadata(event);

            // Step 2: Send cache invalidation signal to frontend BEFORE data updates
            sendCacheInvalidationSignal(event, invalidationMetadata);

            // Step 3: Small delay to ensure frontend processes invalidation signal first
            try {
                Thread.sleep(50); // 50ms delay for frontend cache invalidation processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.debug("Coordinated cache invalidation completed for event type: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Failed to perform coordinated cache invalidation for event {}: {}",
                event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * Perform cache invalidation and collect metadata for WebSocket coordination
     */
    private Map<String, Object> performCacheInvalidationWithMetadata(CoordinatedCacheInvalidationEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        List<String> invalidatedCaches = new ArrayList<>();

        switch (event.getEventType()) {
            case "PRICE_CHANGE":
                invalidatedCaches.add("searchResults");
                invalidatedCaches.add("popularProducts");
                cacheInvalidationService.invalidateCache("searchResults");
                cacheInvalidationService.invalidateCache("popularProducts");
                metadata.put("scope", "PRICING_DATA");
                break;

            case "INVENTORY_UPDATE":
                invalidatedCaches.add("productData");
                invalidatedCaches.add("inventory");
                cacheInvalidationService.invalidateProductData(null);
                metadata.put("scope", "INVENTORY_DATA");
                break;

            case "VOUCHER_CHANGE":
                String voucherType = (String) event.getEventData().get("loaiVoucher");
                if ("PHIEU_GIAM_GIA".equals(voucherType)) {
                    invalidatedCaches.add("phieuGiamGia");
                    cacheInvalidationService.invalidateByPattern("phieuGiamGia:*");
                } else if ("DOT_GIAM_GIA".equals(voucherType)) {
                    invalidatedCaches.add("dotGiamGia");
                    cacheInvalidationService.invalidateByPattern("dotGiamGia:*");
                }
                Boolean hasStatusChanged = (Boolean) event.getEventData().get("hasStatusChanged");
                if (Boolean.TRUE.equals(hasStatusChanged)) {
                    invalidatedCaches.add("cartData");
                    cacheInvalidationService.invalidateCache("cartData");
                }
                metadata.put("scope", "VOUCHER_DATA");
                metadata.put("voucherType", voucherType);
                break;

            case "ORDER_CHANGE":
                invalidatedCaches.add("hoaDon");
                cacheInvalidationService.invalidateByPattern("hoaDon:*");
                Boolean isCompleted = (Boolean) event.getEventData().get("isOrderCompleted");
                Boolean isCancelled = (Boolean) event.getEventData().get("isOrderCancelled");
                if (Boolean.TRUE.equals(isCompleted) || Boolean.TRUE.equals(isCancelled)) {
                    invalidatedCaches.add("popularProducts");
                    cacheInvalidationService.invalidateCache("popularProducts");
                }
                metadata.put("scope", "ORDER_DATA");
                break;

            default:
                log.warn("Unknown coordinated cache invalidation event type: {}", event.getEventType());
                metadata.put("scope", "GENERAL_DATA");
        }

        metadata.put("invalidatedCaches", invalidatedCaches);
        metadata.put("timestamp", Instant.now());
        metadata.put("eventType", event.getEventType());
        metadata.put("requiresRefresh", true);

        return metadata;
    }

    /**
     * Send cache invalidation signal to frontend BEFORE data updates
     * Vietnamese Business Context: Gửi tín hiệu vô hiệu hóa cache đến frontend TRƯỚC khi cập nhật dữ liệu
     */
    private void sendCacheInvalidationSignal(CoordinatedCacheInvalidationEvent event, Map<String, Object> metadata) {
        try {
            Map<String, Object> cacheSignal = new HashMap<>();
            cacheSignal.put("type", "CACHE_INVALIDATION_SIGNAL");
            cacheSignal.put("scope", metadata.get("scope"));
            cacheSignal.put("invalidatedCaches", metadata.get("invalidatedCaches"));
            cacheSignal.put("eventType", event.getEventType());
            cacheSignal.put("entityId", event.getEntityId());
            cacheSignal.put("timestamp", Instant.now());
            cacheSignal.put("requiresRefresh", true);
            cacheSignal.put("priority", "HIGH");

            // Add Vietnamese business context
            cacheSignal.put("lyDoVoHieuHoa", "Đồng bộ dữ liệu thời gian thực");

            // Send to dedicated cache invalidation topic for immediate frontend processing
            messagingTemplate.convertAndSend("/topic/cache-invalidation", cacheSignal);

            log.debug("Sent cache invalidation signal for event type: {} with scope: {}",
                event.getEventType(), metadata.get("scope"));

        } catch (Exception e) {
            log.error("Failed to send cache invalidation signal for event {}: {}",
                event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * Send coordinated WebSocket notification with cache invalidation metadata.
     */
    private void sendCoordinatedWebSocketNotification(CoordinatedCacheInvalidationEvent event) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "COORDINATED_UPDATE");
            notification.put("eventType", event.getEventType());
            notification.put("entityId", event.getEntityId());
            notification.put("data", event.getEventData());
            notification.put("timestamp", Instant.now());

            // Add cache invalidation completion signal
            Map<String, Object> cacheStatus = new HashMap<>();
            cacheStatus.put("invalidationCompleted", true);
            cacheStatus.put("invalidationTimestamp", Instant.now());
            cacheStatus.put("safeToRefresh", true);
            notification.put("cacheStatus", cacheStatus);

            // Send to coordinated updates topic
            messagingTemplate.convertAndSend("/topic/coordinated-updates", notification);

            log.debug("Sent coordinated WebSocket notification for event type: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Failed to send coordinated WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle event processing failures with proper error recovery.
     * This method is called when transactional event processing fails.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleEventProcessingFailure(PriceChangeEvent event) {
        log.warn("Transaction rolled back for price change event: variant {}", event.getVariantId());
        // No cache invalidation or WebSocket notifications should be sent
        // as the database transaction was rolled back
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleEventProcessingFailure(InventoryUpdateEvent event) {
        log.warn("Transaction rolled back for inventory update event: variant {}", event.getVariantId());
        // No cache invalidation or WebSocket notifications should be sent
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleEventProcessingFailure(VoucherChangeEvent event) {
        log.warn("Transaction rolled back for voucher change event: voucher {}", event.getVoucherId());
        // No cache invalidation or WebSocket notifications should be sent
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleEventProcessingFailure(OrderChangeEvent event) {
        log.warn("Transaction rolled back for order change event: order {}", event.getHoaDonId());
        // No cache invalidation or WebSocket notifications should be sent
    }

    /**
     * Add monitoring for event processing timing and performance.
     */
    private void logEventProcessingMetrics(String eventType, long executionTime, boolean success) {
        if (success) {
            log.info("Event processing metrics - Type: {}, Duration: {}ms, Status: SUCCESS", eventType, executionTime);
        } else {
            log.error("Event processing metrics - Type: {}, Duration: {}ms, Status: FAILURE", eventType, executionTime);
        }

        // Log performance warnings for slow events
        if (executionTime > 1000) {
            log.warn("Slow event processing detected - Type: {}, Duration: {}ms", eventType, executionTime);
        }
    }

    // ==================== PRICE CHANGE EVENTS ====================

    /**
     * Handle price change events with transactional coordination.
     * Cache invalidation occurs AFTER database transaction commits to ensure data consistency.
     * This listener focuses on additional cache invalidation beyond what's already done in PriceChangeNotificationService.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePriceChangeEvent(PriceChangeEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing post-commit cache invalidation for price change: variant {}", event.getVariantId());

            // Invalidate search results that might contain this product
            cacheInvalidationService.invalidateCache("searchResults");

            // Invalidate popular products if this might affect rankings
            cacheInvalidationService.invalidateCache("popularProducts");

            long executionTime = System.currentTimeMillis() - startTime;
            logEventProcessingMetrics("PRICE_CHANGE", executionTime, true);
            log.info("Post-commit cache invalidation completed for price change: variant {} ({}ms)",
                    event.getVariantId(), executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logEventProcessingMetrics("PRICE_CHANGE", executionTime, false);
            log.error("Failed to process post-commit cache invalidation for price change variant {}: {}",
                    event.getVariantId(), e.getMessage(), e);
        }
    }

    // ==================== INVENTORY UPDATE EVENTS ====================

    /**
     * Handle inventory update events with transactional coordination.
     * Cache invalidation and WebSocket notifications occur AFTER database transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryUpdateEvent(InventoryUpdateEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing post-commit inventory update event for variant {}: {} -> {}",
                event.getVariantId(), event.getSoLuongTonKhoCu(), event.getSoLuongTonKhoMoi());

            // Step 1: Invalidate product-related caches FIRST
            cacheInvalidationService.invalidateProductData(null); // Invalidate all product caches
            log.debug("Cache invalidation completed for inventory update: variant {}", event.getVariantId());

            // Step 2: Send WebSocket notification AFTER cache invalidation
            sendInventoryUpdateNotification(event);

            long executionTime = System.currentTimeMillis() - startTime;
            logEventProcessingMetrics("INVENTORY_UPDATE", executionTime, true);
            log.info("Post-commit inventory update event processing completed for variant {} ({}ms)",
                    event.getVariantId(), executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logEventProcessingMetrics("INVENTORY_UPDATE", executionTime, false);
            log.error("Failed to process post-commit inventory update event for variant {}: {}",
                event.getVariantId(), e.getMessage(), e);
        }
    }

    // ==================== VOUCHER CHANGE EVENTS ====================

    /**
     * Handle voucher change events with transactional coordination.
     * Cache invalidation and WebSocket notifications occur AFTER database transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoucherChangeEvent(VoucherChangeEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing post-commit voucher change event for voucher {}: {}",
                event.getVoucherId(), event.getLoaiThayDoi());

            // Step 1: Invalidate voucher-related caches FIRST
            if ("PHIEU_GIAM_GIA".equals(event.getLoaiVoucher())) {
                cacheInvalidationService.invalidateByPattern("phieuGiamGia:*");
                log.debug("Invalidated phieuGiamGia cache patterns for voucher {}", event.getVoucherId());
            } else if ("DOT_GIAM_GIA".equals(event.getLoaiVoucher())) {
                cacheInvalidationService.invalidateByPattern("dotGiamGia:*");
                log.debug("Invalidated dotGiamGia cache patterns for voucher {}", event.getVoucherId());
            }

            // Invalidate cart data if voucher status changed (affects pricing)
            if (event.hasStatusChanged()) {
                cacheInvalidationService.invalidateCache("cartData");
                log.debug("Invalidated cartData cache due to voucher status change: {}", event.getVoucherId());
            }

            // Step 2: Send WebSocket notification AFTER cache invalidation
            sendVoucherChangeNotification(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Post-commit voucher change event processing completed for voucher {} ({}ms)",
                    event.getVoucherId(), executionTime);

        } catch (Exception e) {
            log.error("Failed to process post-commit voucher change event for voucher {}: {}",
                event.getVoucherId(), e.getMessage(), e);
        }
    }

    // ==================== ORDER CHANGE EVENTS ====================

    /**
     * Handle order change events with transactional coordination.
     * Cache invalidation and WebSocket notifications occur AFTER database transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderChangeEvent(OrderChangeEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("Processing post-commit order change event for order {}: {}",
                event.getHoaDonId(), event.getLoaiThayDoi());

            // Step 1: Invalidate order-related caches FIRST
            cacheInvalidationService.invalidateByPattern("hoaDon:*");
            log.debug("Invalidated hoaDon cache patterns for order {}", event.getHoaDonId());

            // If order completed or cancelled, might affect product popularity
            if (event.isOrderCompleted() || event.isOrderCancelled()) {
                cacheInvalidationService.invalidateCache("popularProducts");
                log.debug("Invalidated popularProducts cache due to order status change: {}", event.getHoaDonId());
            }

            // Step 2: Send WebSocket notification AFTER cache invalidation
            sendOrderChangeNotification(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Post-commit order change event processing completed for order {} ({}ms)",
                    event.getHoaDonId(), executionTime);

        } catch (Exception e) {
            log.error("Failed to process post-commit order change event for order {}: {}",
                event.getHoaDonId(), e.getMessage(), e);
        }
    }

    // ==================== WEBSOCKET NOTIFICATION METHODS ====================

    /**
     * Send WebSocket notification for inventory updates with cache invalidation signals.
     * Includes cache invalidation metadata to help frontend coordinate data refresh.
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

            // Add cache invalidation metadata for frontend coordination
            Map<String, Object> cacheInvalidation = new HashMap<>();
            cacheInvalidation.put("invalidatedCaches", new String[]{"productData", "searchResults", "popularProducts"});
            cacheInvalidation.put("requiresRefresh", true);
            cacheInvalidation.put("refreshScope", "PRODUCT_INVENTORY");
            notification.put("cacheInvalidation", cacheInvalidation);
            
            // Send to specific variant topic
            messagingTemplate.convertAndSend("/topic/ton-kho/" + event.getVariantId(), notification);

            // Send to general inventory topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/ton-kho/all", notification);

            // Check for low stock and send alert if needed
            checkAndSendLowStockAlert(event);

            // Send cache invalidation signal to frontend
            Map<String, Object> cacheSignal = new HashMap<>();
            cacheSignal.put("type", "CACHE_INVALIDATION");
            cacheSignal.put("scope", "PRODUCT_INVENTORY");
            cacheSignal.put("variantId", event.getVariantId());
            cacheSignal.put("timestamp", Instant.now());
            messagingTemplate.convertAndSend("/topic/cache-invalidation", cacheSignal);

            log.debug("Sent inventory update WebSocket notification with cache invalidation signal for variant {}", event.getVariantId());
            
        } catch (Exception e) {
            log.error("Failed to send inventory update WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send WebSocket notification for voucher changes with cache invalidation signals.
     * Includes cache invalidation metadata to help frontend coordinate data refresh.
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

            // Add cache invalidation metadata for frontend coordination
            Map<String, Object> cacheInvalidation = new HashMap<>();
            String[] invalidatedCaches = event.hasStatusChanged() ?
                new String[]{"phieuGiamGia", "dotGiamGia", "cartData"} :
                new String[]{"phieuGiamGia", "dotGiamGia"};
            cacheInvalidation.put("invalidatedCaches", invalidatedCaches);
            cacheInvalidation.put("requiresRefresh", true);
            cacheInvalidation.put("refreshScope", "VOUCHER_DATA");
            notification.put("cacheInvalidation", cacheInvalidation);
            
            // Send to voucher-specific topic
            String topicPrefix = "PHIEU_GIAM_GIA".equals(event.getLoaiVoucher()) ? "phieu-giam-gia" : "dot-giam-gia";
            messagingTemplate.convertAndSend("/topic/" + topicPrefix + "/" + event.getVoucherId(), notification);

            // Send to general voucher topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/voucher/all", notification);

            // Send cache invalidation signal to frontend
            Map<String, Object> cacheSignal = new HashMap<>();
            cacheSignal.put("type", "CACHE_INVALIDATION");
            cacheSignal.put("scope", "VOUCHER_DATA");
            cacheSignal.put("voucherId", event.getVoucherId());
            cacheSignal.put("voucherType", event.getLoaiVoucher());
            cacheSignal.put("timestamp", Instant.now());
            messagingTemplate.convertAndSend("/topic/cache-invalidation", cacheSignal);

            log.debug("Sent voucher change WebSocket notification with cache invalidation signal for voucher {}", event.getVoucherId());
            
        } catch (Exception e) {
            log.error("Failed to send voucher change WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send WebSocket notification for order changes with cache invalidation signals.
     * Includes cache invalidation metadata to help frontend coordinate data refresh.
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

            // Add cache invalidation metadata for frontend coordination
            Map<String, Object> cacheInvalidation = new HashMap<>();
            String[] invalidatedCaches = (event.isOrderCompleted() || event.isOrderCancelled()) ?
                new String[]{"hoaDon", "popularProducts"} :
                new String[]{"hoaDon"};
            cacheInvalidation.put("invalidatedCaches", invalidatedCaches);
            cacheInvalidation.put("requiresRefresh", true);
            cacheInvalidation.put("refreshScope", "ORDER_DATA");
            notification.put("cacheInvalidation", cacheInvalidation);
            
            // Send to order-specific topic
            messagingTemplate.convertAndSend("/topic/hoa-don/" + event.getHoaDonId(), notification);

            // Send to general order topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/hoa-don/all", notification);

            // Send to customer-specific topic if customer exists
            if (event.getKhachHangId() != null) {
                messagingTemplate.convertAndSend("/topic/khach-hang/" + event.getKhachHangId() + "/orders", notification);
            }

            // Send cache invalidation signal to frontend
            Map<String, Object> cacheSignal = new HashMap<>();
            cacheSignal.put("type", "CACHE_INVALIDATION");
            cacheSignal.put("scope", "ORDER_DATA");
            cacheSignal.put("hoaDonId", event.getHoaDonId());
            cacheSignal.put("khachHangId", event.getKhachHangId());
            cacheSignal.put("timestamp", Instant.now());
            messagingTemplate.convertAndSend("/topic/cache-invalidation", cacheSignal);

            log.debug("Sent order change WebSocket notification with cache invalidation signal for order {}", event.getHoaDonId());

        } catch (Exception e) {
            log.error("Failed to send order change WebSocket notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Check inventory levels and send low stock alerts if thresholds are reached.
     * Vietnamese Business Context: Kiểm tra mức tồn kho và gửi cảnh báo hết hàng khi đạt ngưỡng
     */
    private void checkAndSendLowStockAlert(InventoryUpdateEvent event) {
        try {
            Integer currentStock = event.getSoLuongTonKhoMoi();
            if (currentStock == null) return;

            // Define stock thresholds
            final int CRITICAL_THRESHOLD = 5;  // Critical low stock
            final int WARNING_THRESHOLD = 10; // Warning low stock

            String alertLevel = null;
            String alertMessage = null;

            if (currentStock == 0) {
                alertLevel = "OUT_OF_STOCK";
                alertMessage = "Sản phẩm đã hết hàng";
            } else if (currentStock <= CRITICAL_THRESHOLD) {
                alertLevel = "CRITICAL";
                alertMessage = "Tồn kho ở mức nguy hiểm (còn " + currentStock + " sản phẩm)";
            } else if (currentStock <= WARNING_THRESHOLD) {
                alertLevel = "WARNING";
                alertMessage = "Tồn kho thấp (còn " + currentStock + " sản phẩm)";
            }

            // Send low stock alert if threshold is reached
            if (alertLevel != null) {
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("variantId", event.getVariantId());
                alertData.put("sku", event.getSku());
                alertData.put("tenSanPham", event.getTenSanPham());
                alertData.put("soLuongTonKho", currentStock);
                alertData.put("alertLevel", alertLevel);
                alertData.put("alertMessage", alertMessage);
                alertData.put("loaiThayDoi", event.getLoaiThayDoi());
                alertData.put("timestamp", Instant.now());

                // Send to low stock alert topic
                messagingTemplate.convertAndSend("/topic/ton-kho/low-stock", alertData);

                // Send to general alert topic for dashboard monitoring
                messagingTemplate.convertAndSend("/topic/alerts/inventory", alertData);

                log.info("Sent low stock alert for variant {}: {} - {}",
                        event.getVariantId(), alertLevel, alertMessage);
            }

        } catch (Exception e) {
            log.error("Failed to check and send low stock alert for variant {}: {}",
                    event.getVariantId(), e.getMessage(), e);
        }
    }
}
