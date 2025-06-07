package com.lapxpert.backend.payment.domain.service;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonRepository;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonAuditHistoryRepository;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
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
    
    public PaymentMonitoringService(HoaDonRepository hoaDonRepository, 
                                  HoaDonAuditHistoryRepository auditHistoryRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.auditHistoryRepository = auditHistoryRepository;
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
            
            // TODO: Implement timeout handling logic
            // - Release inventory reservations
            // - Send notification to customer
            // - Update order status if appropriate
            
        } catch (Exception e) {
            log.error("Failed to handle payment timeout for order {}: {}", order.getId(), e.getMessage());
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
