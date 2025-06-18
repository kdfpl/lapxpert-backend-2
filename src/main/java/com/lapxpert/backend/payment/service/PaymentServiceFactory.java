package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Factory service for payment gateways
 * Provides abstraction layer for multiple payment providers
 */
@Slf4j
@Service
public class PaymentServiceFactory {
    
    private final List<PaymentGatewayService> paymentGateways;
    
    public PaymentServiceFactory(List<PaymentGatewayService> paymentGateways) {
        this.paymentGateways = paymentGateways;
        log.info("Initialized PaymentServiceFactory with {} payment gateways", paymentGateways.size());
    }
    
    /**
     * Get payment gateway for specific payment method
     */
    public Optional<PaymentGatewayService> getPaymentGateway(PhuongThucThanhToan paymentMethod) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.supports(paymentMethod))
                .findFirst();
    }
    
    /**
     * Create payment URL using appropriate gateway
     */
    public String createPaymentUrl(PhuongThucThanhToan paymentMethod, Long orderId, int amount, 
                                 String orderInfo, String baseUrl, String clientIp) {
        PaymentGatewayService gateway = getPaymentGateway(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("No payment gateway found for method: " + paymentMethod));
        
        return gateway.createPaymentUrl(orderId, amount, orderInfo, baseUrl, clientIp);
    }
    
    /**
     * Verify payment using appropriate gateway
     */
    public PaymentGatewayService.PaymentVerificationResult verifyPayment(PhuongThucThanhToan paymentMethod, 
                                                                        String transactionRef, String secureHash, 
                                                                        String transactionStatus) {
        PaymentGatewayService gateway = getPaymentGateway(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("No payment gateway found for method: " + paymentMethod));
        
        return gateway.verifyPayment(transactionRef, secureHash, transactionStatus);
    }
}
