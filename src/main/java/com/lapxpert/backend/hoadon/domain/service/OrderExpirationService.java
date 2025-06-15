package com.lapxpert.backend.hoadon.domain.service;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonAuditHistoryRepository;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.sanpham.domain.service.SerialNumberService;
import com.lapxpert.backend.phieugiamgia.domain.service.PhieuGiamGiaService;
import com.lapxpert.backend.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Order Expiration Service with Inventory Release
 * Handles automatic order expiration for unpaid orders after 24 hours
 * Follows existing scheduler patterns from DotGiamGiaService and PhieuGiamGiaService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonAuditHistoryRepository auditHistoryRepository;
    private final SerialNumberService serialNumberService;
    private final PhieuGiamGiaService phieuGiamGiaService;
    private final EmailService emailService;

    /**
     * Process expired orders every 5 minutes
     * Orders expire after 24 hours without payment
     * Following the same pattern as SerialNumberService.cleanupExpiredReservations()
     */
    @Scheduled(cron = "0 */5 * * * ?") // Every 5 minutes
    @Transactional
    public void processExpiredOrders() {
        try {
            // Calculate cutoff time: 24 hours ago
            Instant cutoffTime = Instant.now().minus(24, ChronoUnit.HOURS);
            
            // Find expired unpaid orders
            List<HoaDon> expiredOrders = hoaDonRepository.findExpiredUnpaidOrders(cutoffTime);
            
            if (expiredOrders.isEmpty()) {
                log.debug("No expired orders found for processing");
                return;
            }
            
            log.info("Found {} expired orders to process", expiredOrders.size());
            
            // Process each expired order
            for (HoaDon order : expiredOrders) {
                try {
                    processExpiredOrder(order);
                } catch (Exception e) {
                    log.error("Failed to process expired order {}: {}", order.getId(), e.getMessage(), e);
                    // Continue processing other orders even if one fails
                }
            }
            
            log.info("Completed processing {} expired orders", expiredOrders.size());
            
        } catch (Exception e) {
            // Log error but don't throw to prevent scheduler from stopping
            log.error("Error in order expiration scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single expired order
     * 1. Release serial numbers back to inventory
     * 2. Remove applied vouchers
     * 3. Update order status to DA_HUY
     * 4. Send email notification to customer
     * 5. Create audit trail entry
     */
    @Transactional
    public void processExpiredOrder(HoaDon order) {
        log.info("Processing expired order: {}", order.getId());
        
        try {
            // Step 1: Release serial numbers back to inventory
            releaseOrderInventory(order);
            
            // Step 2: Remove applied vouchers and restore usage counts
            removeOrderVouchers(order);
            
            // Step 3: Update order status to cancelled with Vietnamese reason
            order.setTrangThaiDonHang(TrangThaiDonHang.DA_HUY);
            HoaDon savedOrder = hoaDonRepository.save(order);
            
            // Step 4: Create audit trail entry
            createExpirationAuditEntry(savedOrder);
            
            // Step 5: Send email notification to customer
            sendExpirationNotification(savedOrder);
            
            log.info("Successfully processed expired order: {}", order.getId());
            
        } catch (Exception e) {
            log.error("Failed to process expired order {}: {}", order.getId(), e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    /**
     * Release serial numbers back to inventory using existing SerialNumberService
     */
    private void releaseOrderInventory(HoaDon order) {
        try {
            // Get reserved serial numbers for this order
            List<Long> reservedItemIds = serialNumberService.getReservedSerialNumberIdsForOrder(order.getId().toString());
            
            if (!reservedItemIds.isEmpty()) {
                // Release reservations safely
                serialNumberService.releaseReservationsSafely(reservedItemIds);
                log.info("Released {} reserved items for expired order {}", reservedItemIds.size(), order.getId());
            } else {
                log.debug("No reserved items found for expired order {}", order.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to release inventory for expired order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to release inventory for expired order", e);
        }
    }

    /**
     * Remove applied vouchers and restore usage counts
     */
    private void removeOrderVouchers(HoaDon order) {
        try {
            // Use existing PhieuGiamGiaService method to remove vouchers from order
            phieuGiamGiaService.removeVouchersFromOrder(order.getId());
            log.info("Removed vouchers from expired order {}", order.getId());
            
        } catch (Exception e) {
            log.error("Failed to remove vouchers from expired order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception here as voucher removal is not critical for order cancellation
        }
    }

    /**
     * Create audit trail entry for order expiration
     */
    private void createExpirationAuditEntry(HoaDon order) {
        try {
            String auditValues = buildAuditJson(order);
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.createEntry(
                order.getId(),
                auditValues,
                "SYSTEM",
                "Đơn hàng hết hạn thanh toán - tự động hủy sau 24 giờ"
            );
            auditHistoryRepository.save(auditEntry);
            
            log.debug("Created audit entry for expired order {}", order.getId());
            
        } catch (Exception e) {
            log.error("Failed to create audit entry for expired order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception as audit failure shouldn't prevent order cancellation
        }
    }

    /**
     * Send email notification to customer about order expiration
     */
    private void sendExpirationNotification(HoaDon order) {
        try {
            if (order.getKhachHang() == null || order.getKhachHang().getEmail() == null) {
                log.warn("Cannot send expiration notification for order {} - no customer email", order.getId());
                return;
            }
            
            String customerEmail = order.getKhachHang().getEmail();
            String customerName = order.getKhachHang().getHoTen() != null ? 
                order.getKhachHang().getHoTen() : "Quý khách";
            
            String subject = "Thông báo hết hạn thanh toán đơn hàng #" + order.getId();
            String text = String.format(
                "Chào %s,\n\n" +
                "Đơn hàng #%s của bạn đã hết hạn thanh toán sau 24 giờ.\n" +
                "Đơn hàng đã được hủy tự động và các sản phẩm đã được trả về kho.\n\n" +
                "Nếu bạn vẫn muốn mua các sản phẩm này, vui lòng tạo đơn hàng mới.\n\n" +
                "Xin lỗi vì sự bất tiện này.\n\n" +
                "Trân trọng,\nLapXpert Team",
                customerName,
                order.getId()
            );
            
            emailService.sendEmail(customerEmail, subject, text);
            log.info("Sent expiration notification to customer {} for order {}", customerEmail, order.getId());
            
        } catch (Exception e) {
            log.error("Failed to send expiration notification for order {}: {}", order.getId(), e.getMessage());
            // Don't throw exception as email failure shouldn't prevent order cancellation
        }
    }

    /**
     * Build audit JSON for order expiration
     * Following the same pattern as other services
     */
    private String buildAuditJson(HoaDon order) {
        return String.format(
            "{\"orderId\":%d,\"status\":\"%s\",\"totalAmount\":%s,\"expirationTime\":\"%s\"}",
            order.getId(),
            order.getTrangThaiDonHang().name(),
            order.getTongThanhToan().toString(),
            Instant.now().toString()
        );
    }
}
