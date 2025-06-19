package com.lapxpert.backend.websocket;

import com.lapxpert.backend.common.service.WebSocketIntegrationService;
import com.lapxpert.backend.websocket.service.RedisMessageSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for enhanced WebSocket functionality
 * 
 * Tests the enhanced WebSocket capabilities within the monolithic application,
 * including Redis Pub/Sub integration and message routing.
 */
@SpringBootTest
@ActiveProfiles("test")
public class WebSocketIntegrationTest {

    @Autowired(required = false)
    private WebSocketIntegrationService webSocketIntegrationService;

    @Autowired(required = false)
    private RedisMessageSubscriber redisMessageSubscriber;

    @Test
    public void testWebSocketIntegrationServiceIsAvailable() {
        // Test that the WebSocket integration service is properly configured
        assertNotNull(webSocketIntegrationService, "WebSocketIntegrationService should be available");
    }

    @Test
    public void testRedisMessageSubscriberIsAvailable() {
        // Test that the Redis message subscriber is properly configured
        assertNotNull(redisMessageSubscriber, "RedisMessageSubscriber should be available");
    }

    @Test
    public void testPriceUpdateMessage() {
        // Test sending a price update message
        assertDoesNotThrow(() -> {
            webSocketIntegrationService.sendPriceUpdate(
                "12345", 
                999.99, 
                "Test price update message"
            );
        }, "Price update should not throw exception");
    }

    @Test
    public void testVoucherNotificationMessage() {
        // Test sending a voucher notification message
        assertDoesNotThrow(() -> {
            webSocketIntegrationService.sendVoucherNotification(
                "VOUCHER123", 
                "PHIEU_GIAM_GIA", 
                "Test voucher notification", 
                null
            );
        }, "Voucher notification should not throw exception");
    }

    @Test
    public void testSystemNotificationMessage() {
        // Test sending a system notification message
        assertDoesNotThrow(() -> {
            webSocketIntegrationService.sendSystemNotification(
                "Test system notification", 
                "INFO"
            );
        }, "System notification should not throw exception");
    }

    @Test
    public void testCustomMessage() {
        // Test sending a custom message
        assertDoesNotThrow(() -> {
            webSocketIntegrationService.sendCustomMessage(
                "/topic/test", 
                "Test custom message payload", 
                "TEST_MESSAGE"
            );
        }, "Custom message should not throw exception");
    }

    @Test
    public void testMetricsCollection() {
        // Test that metrics are being collected
        assertNotNull(webSocketIntegrationService.getMetrics(), "Metrics should be available");

        WebSocketIntegrationService.IntegrationMetrics metrics = webSocketIntegrationService.getMetrics();
        assertTrue(metrics.getTotalMessagesSent() >= 0, "Total messages sent should be non-negative");
        assertTrue(metrics.getSendErrors() >= 0, "Send errors should be non-negative");
        assertNotNull(metrics.getTimestamp(), "Metrics timestamp should not be null");
    }

    @Test
    public void testRedisMessageHandling() {
        // Test that Redis message handling doesn't throw exceptions
        String testMessageJson = "{\"destination\":\"/topic/test\",\"payload\":{\"message\":\"test\"},\"messageType\":\"TEST\",\"timestamp\":\"2024-01-01T00:00:00Z\"}";

        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleGlobalMessage(testMessageJson, "test-channel");
        }, "Global message handling should not throw exception");

        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handlePriceMessage(
                "{\"destination\":\"/topic/gia-san-pham/123\",\"payload\":{\"price\":999.99},\"messageType\":\"PRICE_UPDATE\"}",
                "price-channel"
            );
        }, "Price message handling should not throw exception");

        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleVoucherMessage(
                "{\"destination\":\"/topic/phieu-giam-gia/456\",\"payload\":{\"voucher\":\"TEST\"},\"messageType\":\"VOUCHER_NOTIFICATION\"}",
                "voucher-channel"
            );
        }, "Voucher message handling should not throw exception");
    }
}
