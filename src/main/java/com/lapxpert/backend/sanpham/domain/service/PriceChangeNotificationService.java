package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.sanpham.domain.dto.PriceUpdateMessage;
import com.lapxpert.backend.sanpham.domain.entity.SanPhamChiTietAuditHistory;
import com.lapxpert.backend.sanpham.domain.event.PriceChangeEvent;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietAuditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling real-time price change notifications.
 * Extends existing audit infrastructure with WebSocket messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceChangeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SanPhamChiTietAuditHistoryRepository auditHistoryRepository;

    /**
     * Handle price change events with audit trail and real-time notifications
     */
    @EventListener
    @Transactional
    @CacheEvict(value = {"cartData", "activeSanPhamList"}, allEntries = true)
    public void handlePriceChange(PriceChangeEvent event) {
        try {
            log.info("Processing price change for variant {}: {} -> {}", 
                event.getVariantId(), event.getEffectiveOldPrice(), event.getEffectiveNewPrice());

            // Create audit trail entry
            createAuditEntry(event);

            // Send real-time WebSocket notification
            sendWebSocketNotification(event);

            log.info("Price change notification completed for variant {}", event.getVariantId());

        } catch (Exception e) {
            log.error("Failed to process price change notification for variant {}: {}", 
                event.getVariantId(), e.getMessage(), e);
        }
    }

    /**
     * Create audit trail entry for price change
     */
    private void createAuditEntry(PriceChangeEvent event) {
        try {
            // Create audit entry for regular price change
            if (event.hasRegularPriceChanged()) {
                SanPhamChiTietAuditHistory auditEntry = SanPhamChiTietAuditHistory.priceChangeEntry(
                    event.getVariantId(),
                    event.getOldPrice() != null ? event.getOldPrice().toString() : "null",
                    event.getNewPrice() != null ? event.getNewPrice().toString() : "null",
                    event.getNguoiThucHien(),
                    event.getLyDoThayDoi()
                );
                auditHistoryRepository.save(auditEntry);
                
                log.debug("Created audit entry for regular price change: variant {}", event.getVariantId());
            }

            // Create separate audit entry for promotional price change if different
            if (event.hasPromotionalPriceChanged()) {
                String oldPromoPrice = event.getOldPromotionalPrice() != null ? 
                    event.getOldPromotionalPrice().toString() : "null";
                String newPromoPrice = event.getNewPromotionalPrice() != null ? 
                    event.getNewPromotionalPrice().toString() : "null";
                
                String oldValues = String.format("{\"giaKhuyenMai\":\"%s\"}", oldPromoPrice);
                String newValues = String.format("{\"giaKhuyenMai\":\"%s\"}", newPromoPrice);
                
                SanPhamChiTietAuditHistory promoAuditEntry = SanPhamChiTietAuditHistory.builder()
                        .sanPhamChiTietId(event.getVariantId())
                        .hanhDong("PROMOTIONAL_PRICE_CHANGE")
                        .thoiGianThayDoi(event.getTimestamp())
                        .nguoiThucHien(event.getNguoiThucHien() != null ? event.getNguoiThucHien() : "SYSTEM")
                        .lyDoThayDoi(event.getLyDoThayDoi() != null ? event.getLyDoThayDoi() : "Thay đổi giá khuyến mãi")
                        .giaTriCu(oldValues)
                        .giaTriMoi(newValues)
                        .build();
                
                auditHistoryRepository.save(promoAuditEntry);
                
                log.debug("Created audit entry for promotional price change: variant {}", event.getVariantId());
            }

        } catch (Exception e) {
            log.error("Failed to create audit entry for price change: {}", e.getMessage(), e);
        }
    }

    /**
     * Send real-time WebSocket notification
     */
    private void sendWebSocketNotification(PriceChangeEvent event) {
        try {
            // Create price update message
            PriceUpdateMessage message = PriceUpdateMessage.fromEvent(event);

            // Send to specific variant topic
            String variantTopic = "/topic/gia-san-pham/" + event.getVariantId();
            messagingTemplate.convertAndSend(variantTopic, message);

            // Send to general price updates topic for dashboard monitoring
            messagingTemplate.convertAndSend("/topic/gia-san-pham/all", message);

            log.debug("Sent WebSocket notification for variant {} to topics: {}, /topic/gia-san-pham/all", 
                event.getVariantId(), variantTopic);

        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for price change: {}", e.getMessage(), e);
        }
    }

    /**
     * Send test price update notification (for testing purposes)
     */
    public void sendTestNotification(Long variantId, String sku, String productName, 
                                   Double oldPrice, Double newPrice) {
        try {
            PriceUpdateMessage testMessage = PriceUpdateMessage.builder()
                    .variantId(variantId)
                    .sku(sku)
                    .productName(productName)
                    .oldPrice(java.math.BigDecimal.valueOf(oldPrice))
                    .newPrice(java.math.BigDecimal.valueOf(newPrice))
                    .message(String.format("🧪 Test: Giá sản phẩm %s đã thay đổi từ %,.0f₫ thành %,.0f₫", 
                        productName, oldPrice, newPrice))
                    .timestamp(java.time.Instant.now())
                    .changeType(newPrice > oldPrice ? "INCREASE" : "DECREASE")
                    .nguoiThucHien("TEST_USER")
                    .build();

            messagingTemplate.convertAndSend("/topic/gia-san-pham/" + variantId, testMessage);
            messagingTemplate.convertAndSend("/topic/gia-san-pham/all", testMessage);

            log.info("Sent test price notification for variant {}", variantId);

        } catch (Exception e) {
            log.error("Failed to send test price notification: {}", e.getMessage(), e);
        }
    }
}
