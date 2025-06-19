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

        // Log available payment methods for debugging
        paymentGateways.forEach(gateway -> {
            log.info("Registered payment gateway: {} for method: {}",
                    gateway.getClass().getSimpleName(),
                    gateway.getSupportedPaymentMethod());
        });
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
                .orElseThrow(() -> {
                    String errorMessage = getPaymentMethodErrorMessage(paymentMethod);
                    log.error("Payment gateway not available for method: {} (Order: {})", paymentMethod, orderId);
                    return new IllegalArgumentException(errorMessage);
                });

        return gateway.createPaymentUrl(orderId, amount, orderInfo, baseUrl, clientIp);
    }
    
    /**
     * Verify payment using appropriate gateway
     */
    public PaymentGatewayService.PaymentVerificationResult verifyPayment(PhuongThucThanhToan paymentMethod,
                                                                        String transactionRef, String secureHash,
                                                                        String transactionStatus) {
        PaymentGatewayService gateway = getPaymentGateway(paymentMethod)
                .orElseThrow(() -> {
                    String errorMessage = getPaymentMethodErrorMessage(paymentMethod);
                    log.error("Payment gateway not available for verification: {} (Transaction: {})", paymentMethod, transactionRef);
                    return new IllegalArgumentException(errorMessage);
                });

        return gateway.verifyPayment(transactionRef, secureHash, transactionStatus);
    }

    /**
     * Get Vietnamese error message for unavailable payment method
     * @param paymentMethod Payment method that is not available
     * @return Vietnamese error message
     */
    private String getPaymentMethodErrorMessage(PhuongThucThanhToan paymentMethod) {
        switch (paymentMethod) {
            case MOMO:
                return "Dịch vụ thanh toán MoMo hiện không khả dụng. Vui lòng liên hệ quản trị viên hoặc sử dụng phương thức thanh toán khác.";
            case VNPAY:
                return "Dịch vụ thanh toán VNPay hiện không khả dụng. Vui lòng liên hệ quản trị viên hoặc sử dụng phương thức thanh toán khác.";
            case VIETQR:
                return "Dịch vụ thanh toán VietQR hiện không khả dụng. Vui lòng liên hệ quản trị viên hoặc sử dụng phương thức thanh toán khác.";
            case TIEN_MAT:
                return "Phương thức thanh toán tiền mặt hiện không khả dụng trong hệ thống trực tuyến.";
            default:
                return "Phương thức thanh toán " + paymentMethod + " hiện không khả dụng. Vui lòng liên hệ quản trị viên.";
        }
    }

    /**
     * Check if a specific payment method is available
     * @param paymentMethod Payment method to check
     * @return true if payment gateway is available for the method
     */
    public boolean isPaymentMethodAvailable(PhuongThucThanhToan paymentMethod) {
        return getPaymentGateway(paymentMethod).isPresent();
    }
}
