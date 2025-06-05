package com.lapxpert.backend.payment.domain.service;

import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.vnpay.domain.VNPayService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * VNPay implementation of PaymentGatewayService
 */
@Slf4j
@Service
public class VNPayGatewayService implements PaymentGatewayService {
    
    private final VNPayService vnPayService;
    
    public VNPayGatewayService(VNPayService vnPayService) {
        this.vnPayService = vnPayService;
    }
    
    @Override
    public String createPaymentUrl(Long orderId, int amount, String orderInfo, String baseUrl, String clientIp) {
        return vnPayService.createOrderWithOrderId(amount, orderInfo, baseUrl, orderId.toString(), clientIp);
    }
    
    @Override
    public PaymentVerificationResult verifyPayment(String transactionRef, String secureHash, String transactionStatus) {
        // This would need to be implemented with proper VNPay verification logic
        // For now, return a basic implementation
        if ("00".equals(transactionStatus)) {
            return PaymentVerificationResult.success(transactionRef, transactionRef);
        } else {
            return PaymentVerificationResult.failure(transactionRef, transactionRef, "Payment failed");
        }
    }
    
    @Override
    public PhuongThucThanhToan getSupportedPaymentMethod() {
        return PhuongThucThanhToan.VNPAY;
    }
    
    @Override
    public boolean supports(PhuongThucThanhToan paymentMethod) {
        return PhuongThucThanhToan.VNPAY.equals(paymentMethod);
    }
}
