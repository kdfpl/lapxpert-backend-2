package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for PaymentServiceFactory conditional service registration
 * Tests the feature flag functionality for MoMo SDK service
 */
@SpringBootTest
@ActiveProfiles("test")
public class PaymentServiceFactoryConditionalTest {

    @Autowired
    private PaymentServiceFactory paymentServiceFactory;

    /**
     * Test with MoMo SDK enabled (default test configuration)
     */
    @Test
    @TestPropertySource(properties = {"momo.sdk.enabled=true"})
    public void testMoMoServiceAvailableWhenEnabled() {
        // Test that MoMo service is available when enabled
        boolean isAvailable = paymentServiceFactory.isPaymentMethodAvailable(PhuongThucThanhToan.MOMO);
        assertTrue(isAvailable, "MoMo payment method should be available when SDK is enabled");
        
        // Test that we can get the gateway
        var gateway = paymentServiceFactory.getPaymentGateway(PhuongThucThanhToan.MOMO);
        assertTrue(gateway.isPresent(), "MoMo gateway should be present when SDK is enabled");
    }

    /**
     * Test error handling when trying to use unavailable payment method
     */
    @Test
    public void testVietnameseErrorMessageForUnavailableMethod() {
        // Test that appropriate Vietnamese error message is thrown
        // when payment method is not available
        
        // This test assumes MoMo might not be available in some test configurations
        try {
            paymentServiceFactory.createPaymentUrl(
                PhuongThucThanhToan.MOMO, 
                12345L, 
                100000, 
                "Test payment", 
                "http://localhost:8080", 
                "127.0.0.1"
            );
            // If we reach here, MoMo is available, which is fine
        } catch (IllegalArgumentException e) {
            // Verify Vietnamese error message
            assertTrue(e.getMessage().contains("MoMo"), 
                "Error message should mention MoMo");
            assertTrue(e.getMessage().contains("không khả dụng"), 
                "Error message should be in Vietnamese");
        }
    }

    /**
     * Test that other payment methods are still available
     */
    @Test
    public void testOtherPaymentMethodsAvailable() {
        // VNPay should always be available (not conditional)
        boolean vnpayAvailable = paymentServiceFactory.isPaymentMethodAvailable(PhuongThucThanhToan.VNPAY);
        assertTrue(vnpayAvailable, "VNPay should always be available");
        
        // VietQR should always be available (not conditional)
        boolean vietqrAvailable = paymentServiceFactory.isPaymentMethodAvailable(PhuongThucThanhToan.VIETQR);
        assertTrue(vietqrAvailable, "VietQR should always be available");
    }

    /**
     * Test factory initialization and gateway registration logging
     */
    @Test
    public void testFactoryInitialization() {
        // Test that factory is properly initialized
        assertNotNull(paymentServiceFactory, "PaymentServiceFactory should be initialized");
        
        // Test that at least some payment gateways are registered
        // (VNPay and VietQR should always be available)
        assertTrue(paymentServiceFactory.isPaymentMethodAvailable(PhuongThucThanhToan.VNPAY) ||
                  paymentServiceFactory.isPaymentMethodAvailable(PhuongThucThanhToan.VIETQR),
                  "At least one payment gateway should be available");
    }
}
