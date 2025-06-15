package com.lapxpert.backend.payment.domain.service;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonAuditHistoryRepository;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.sanpham.domain.service.SerialNumberService;
import com.lapxpert.backend.common.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Payment Monitoring Service
 * Monitors payment-order correlation and handles payment failures
 */
@Slf4j
@Service
public class PaymentMonitoringService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonAuditHistoryRepository auditHistoryRepository;
    private final SerialNumberService serialNumberService;
    private final EmailService emailService;

    public PaymentMonitoringService(HoaDonRepository hoaDonRepository,
                                  HoaDonAuditHistoryRepository auditHistoryRepository,
                                  SerialNumberService serialNumberService,
                                  EmailService emailService) {
        this.hoaDonRepository = hoaDonRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.serialNumberService = serialNumberService;
        this.emailService = emailService;
    }
    
    /**
     * Monitor for payment timeouts (runs every 10 minutes)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void monitorPaymentTimeouts() {
        Instant timeoutThreshold = Instant.now().minus(30, ChronoUnit.MINUTES);
        
        // Find orders that are pending payment for more than 30 minutes
        List<HoaDon> timedOutOrders = hoaDonRepository.findOrdersWithPaymentTimeout(timeoutThreshold);
        
        for (HoaDon order : timedOutOrders) {
            handlePaymentTimeout(order);
        }
        
        if (!timedOutOrders.isEmpty()) {
            log.info("Processed {} payment timeout orders", timedOutOrders.size());
        }
    }
    
    /**
     * Monitor for payment-order mismatches (runs every 15 minutes)
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void monitorPaymentOrderMismatches() {
        // This would check for VNPay transactions that succeeded but orders weren't updated
        // Implementation would depend on having a payment transaction log
        log.debug("Monitoring payment-order mismatches");
    }
    
    /**
     * Handle payment timeout for an order
     */
    private void handlePaymentTimeout(HoaDon order) {
        try {
            log.warn("Payment timeout detected for order {}", order.getId());
            
            // Create audit entry for timeout
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.createEntry(
                order.getId(),
                createAuditValues(order),
                "SYSTEM",
                "Payment timeout - Order pending payment for more than 30 minutes"
            );
            auditHistoryRepository.save(auditEntry);
            
            // Release inventory reservations for this order
            releaseOrderInventoryReservations(order);

            // Send notification to customer about payment timeout
            sendPaymentTimeoutNotification(order);

            // Update order status to cancelled due to payment timeout
            updateOrderStatusForTimeout(order);
            
        } catch (Exception e) {
            log.error("Failed to handle payment timeout for order {}: {}", order.getId(), e.getMessage());
        }
    }
    
    /**
     * Release inventory reservations for a timed-out order
     */
    private void releaseOrderInventoryReservations(HoaDon order) {
        try {
            // Get reserved serial number IDs for this order
            List<Long> reservedSerialNumberIds = serialNumberService.getReservedSerialNumberIdsForOrder(
                order.getId().toString()
            );

            if (!reservedSerialNumberIds.isEmpty()) {
                // Release the reservations
                serialNumberService.releaseReservations(
                    reservedSerialNumberIds,
                    "SYSTEM",
                    "Payment timeout - Releasing inventory reservations"
                );
                log.info("Released {} inventory reservations for timed-out order {}",
                    reservedSerialNumberIds.size(), order.getId());
            } else {
                log.debug("No inventory reservations found for order {}", order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to release inventory reservations for order {}: {}", order.getId(), e.getMessage());
        }
    }

    /**
     * Send payment timeout notification to customer
     */
    private void sendPaymentTimeoutNotification(HoaDon order) {
        try {
            if (order.getKhachHang() != null && order.getKhachHang().getEmail() != null) {
                String customerEmail = order.getKhachHang().getEmail();
                String customerName = order.getKhachHang().getHoTen() != null ?
                    order.getKhachHang().getHoTen() : "Quý khách";

                String subject = "Thông báo hết hạn thanh toán đơn hàng #" + order.getId();
                String text = String.format(
                    "Chào %s,\n\n" +
                    "Đơn hàng #%s của bạn đã hết hạn thanh toán sau 30 phút.\n" +
                    "Đơn hàng đã được hủy tự động và các sản phẩm đã được trả về kho.\n\n" +
                    "Nếu bạn vẫn muốn mua các sản phẩm này, vui lòng tạo đơn hàng mới.\n\n" +
                    "Xin lỗi vì sự bất tiện này.\n\n" +
                    "Trân trọng,\nLapXpert Team",
                    customerName,
                    order.getId()
                );

                emailService.sendEmail(customerEmail, subject, text);
                log.info("Sent payment timeout notification to customer {} for order {}",
                    customerEmail, order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send payment timeout notification for order {}: {}",
                order.getId(), e.getMessage());
        }
    }

    /**
     * Update order status to cancelled due to payment timeout
     */
    private void updateOrderStatusForTimeout(HoaDon order) {
        try {
            // Update order status to cancelled
            order.setTrangThaiDonHang(TrangThaiDonHang.DA_HUY);
            order.setTrangThaiThanhToan(TrangThaiThanhToan.THANH_TOAN_LOI);
            hoaDonRepository.save(order);

            // Create audit entry for status change
            HoaDonAuditHistory statusAuditEntry = HoaDonAuditHistory.createEntry(
                order.getId(),
                createAuditValues(order),
                "SYSTEM",
                "Order cancelled due to payment timeout"
            );
            auditHistoryRepository.save(statusAuditEntry);

            log.info("Updated order {} status to cancelled due to payment timeout", order.getId());
        } catch (Exception e) {
            log.error("Failed to update order status for timed-out order {}: {}",
                order.getId(), e.getMessage());
        }
    }

    /**
     * Create audit values for order
     */
    private String createAuditValues(HoaDon order) {
        return String.format(
            "{\"orderId\":\"%s\",\"status\":\"%s\",\"paymentStatus\":\"%s\",\"amount\":\"%s\"}",
            order.getId(),
            order.getTrangThaiDonHang(),
            order.getTrangThaiThanhToan(),
            order.getTongThanhToan()
        );
    }
    
    /**
     * Get payment monitoring metrics
     */
    public PaymentMetrics getPaymentMetrics() {
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);
        
        long totalOrders = hoaDonRepository.countOrdersInPeriod(last24Hours);
        long paidOrders = hoaDonRepository.countPaidOrdersInPeriod(last24Hours);
        long pendingOrders = hoaDonRepository.countPendingPaymentOrdersInPeriod(last24Hours);
        long vnpayOrders = hoaDonRepository.countOrdersByPaymentMethodInPeriod(PhuongThucThanhToan.VNPAY, last24Hours);
        
        return new PaymentMetrics(totalOrders, paidOrders, pendingOrders, vnpayOrders);
    }
    
    /**
     * Payment metrics data class
     */
    public static class PaymentMetrics {
        private final long totalOrders;
        private final long paidOrders;
        private final long pendingOrders;
        private final long vnpayOrders;
        
        public PaymentMetrics(long totalOrders, long paidOrders, long pendingOrders, long vnpayOrders) {
            this.totalOrders = totalOrders;
            this.paidOrders = paidOrders;
            this.pendingOrders = pendingOrders;
            this.vnpayOrders = vnpayOrders;
        }
        
        // Getters
        public long getTotalOrders() { return totalOrders; }
        public long getPaidOrders() { return paidOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getVnpayOrders() { return vnpayOrders; }
        
        public double getPaymentSuccessRate() {
            return totalOrders > 0 ? (double) paidOrders / totalOrders * 100 : 0;
        }
        
        public double getVnpayUsageRate() {
            return totalOrders > 0 ? (double) vnpayOrders / totalOrders * 100 : 0;
        }
    }
}
