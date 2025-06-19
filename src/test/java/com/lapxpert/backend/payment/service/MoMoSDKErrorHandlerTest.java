package com.lapxpert.backend.payment.service;

import com.lapxpert.backend.payment.momo.shared.exception.MoMoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for MoMo SDK Error Handler
 */
public class MoMoSDKErrorHandlerTest {

    private MoMoSDKErrorHandler errorHandler;

    @BeforeEach
    public void setUp() {
        errorHandler = new MoMoSDKErrorHandler();
    }

    @Test
    public void testHandleMoMoException() {
        MoMoException exception = new MoMoException("Invalid signature");
        String orderId = "12345";
        
        PaymentGatewayService.PaymentVerificationResult result = 
            errorHandler.handleSDKException(exception, orderId);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertFalse(result.isSuccessful());
        assertEquals(orderId, result.getOrderId());
        assertTrue(result.getErrorMessage().contains("chữ ký"));
    }

    @Test
    public void testHandleValidationException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid amount");
        String orderId = "12345";
        
        PaymentGatewayService.PaymentVerificationResult result = 
            errorHandler.handleSDKException(exception, orderId);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("không hợp lệ"));
    }

    @Test
    public void testVietnameseErrorMapping() {
        MoMoException exception = new MoMoException("insufficient balance");
        String orderId = "12345";
        
        PaymentGatewayService.PaymentVerificationResult result = 
            errorHandler.handleSDKException(exception, orderId);
        
        assertNotNull(result);
        assertTrue(result.getErrorMessage().contains("không đủ"));
    }

    @Test
    public void testCreateErrorAuditInfo() {
        Exception exception = new RuntimeException("Test error");
        String orderId = "12345";
        
        Map<String, Object> auditInfo = errorHandler.createErrorAuditInfo("TEST_OPERATION", orderId, exception);
        
        assertNotNull(auditInfo);
        assertEquals("TEST_OPERATION", auditInfo.get("operation"));
        assertEquals(orderId, auditInfo.get("orderId"));
        assertEquals("RuntimeException", auditInfo.get("errorType"));
        assertEquals("Test error", auditInfo.get("errorMessage"));
        assertEquals("MoMo", auditInfo.get("gateway"));
        assertEquals("ERROR", auditInfo.get("status"));
    }

    @Test
    public void testIsRecoverableError() {
        MoMoException timeoutException = new MoMoException("Connection timeout");
        MoMoException signatureException = new MoMoException("Invalid signature");
        
        assertTrue(errorHandler.isRecoverableError(timeoutException));
        assertFalse(errorHandler.isRecoverableError(signatureException));
    }

    @Test
    public void testGetErrorSeverity() {
        MoMoException signatureException = new MoMoException("Invalid signature");
        MoMoException timeoutException = new MoMoException("Connection timeout");
        IllegalArgumentException validationException = new IllegalArgumentException("Invalid data");
        
        assertEquals("CRITICAL", errorHandler.getErrorSeverity(signatureException));
        assertEquals("HIGH", errorHandler.getErrorSeverity(timeoutException));
        assertEquals("LOW", errorHandler.getErrorSeverity(validationException));
    }
}
