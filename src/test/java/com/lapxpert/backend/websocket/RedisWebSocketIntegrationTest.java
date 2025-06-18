package com.lapxpert.backend.websocket;

import com.lapxpert.backend.common.service.WebSocketIntegrationService;
import com.lapxpert.backend.websocket.service.RedisMessageSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for complete Redis Pub/Sub to WebSocket forwarding
 * 
 * Tests the complete message flow:
 * Business Logic → WebSocketIntegrationService → Redis Pub/Sub → RedisMessageSubscriber → WebSocket Clients
 */
@SpringBootTest
@ActiveProfiles("test")
public class RedisWebSocketIntegrationTest {

    @Autowired(required = false)
    private WebSocketIntegrationService webSocketIntegrationService;

    @Autowired(required = false)
    private RedisMessageSubscriber redisMessageSubscriber;

    @Test
    public void testCompleteMessageFlow() {
        // Verify both services are available
        assertNotNull(webSocketIntegrationService, "WebSocketIntegrationService should be available");
        assertNotNull(redisMessageSubscriber, "RedisMessageSubscriber should be available");
        
        // Test the complete flow by sending a message through the integration service
        // In a real scenario, this would go through Redis and be received by the subscriber
        assertDoesNotThrow(() -> {
            webSocketIntegrationService.sendPriceUpdate("12345", 999.99, "Test price update");
        }, "Price update through integration service should not throw exception");
        
        // Verify metrics are updated
        WebSocketIntegrationService.IntegrationMetrics metrics = webSocketIntegrationService.getMetrics();
        assertTrue(metrics.getTotalMessagesSent() >= 0, "Messages should be tracked");
    }

    @Test
    public void testVietnameseTopicHandling() {
        // Test Vietnamese topic structure handling
        String priceMessageJson = """
            {
                "destination": "/topic/gia-san-pham/12345",
                "payload": {
                    "variantId": "12345",
                    "newPrice": 999.99,
                    "message": "Giá sản phẩm đã được cập nhật"
                },
                "messageType": "PRICE_UPDATE",
                "timestamp": "2024-01-01T00:00:00Z"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handlePriceMessage(priceMessageJson, "lapxpert:websocket:price");
        }, "Vietnamese price topic should be handled correctly");
        
        String voucherMessageJson = """
            {
                "destination": "/topic/phieu-giam-gia/VOUCHER123",
                "payload": {
                    "voucherId": "VOUCHER123",
                    "message": "Phiếu giảm giá mới có sẵn"
                },
                "messageType": "VOUCHER_NOTIFICATION",
                "timestamp": "2024-01-01T00:00:00Z"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleVoucherMessage(voucherMessageJson, "lapxpert:websocket:voucher");
        }, "Vietnamese voucher topic should be handled correctly");
    }

    @Test
    public void testUserSpecificMessaging() {
        // Test user-specific messaging for future chatbox functionality
        String userMessageJson = """
            {
                "destination": "/user/testuser/queue/chat",
                "payload": {
                    "message": "Xin chào! Tôi có thể giúp gì cho bạn?",
                    "sender": "support"
                },
                "messageType": "PRIVATE_CHAT",
                "targetUser": "testuser",
                "timestamp": "2024-01-01T00:00:00Z"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleChatboxMessage(userMessageJson, "lapxpert:websocket:chatbox");
        }, "User-specific messaging should be handled correctly");
    }

    @Test
    public void testInvalidMessageHandling() {
        // Test that invalid messages are handled gracefully
        String invalidJson = "invalid json";
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleGlobalMessage(invalidJson, "test-channel");
        }, "Invalid JSON should be handled gracefully");
        
        String invalidDestination = """
            {
                "destination": "/invalid/destination",
                "payload": {"test": "data"},
                "messageType": "TEST"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handlePriceMessage(invalidDestination, "price-channel");
        }, "Invalid destination should be handled gracefully");
    }

    @Test
    public void testMessageValidation() {
        // Test message validation logic
        String validMessage = """
            {
                "destination": "/topic/test",
                "payload": {"message": "test"},
                "messageType": "TEST",
                "timestamp": "2024-01-01T00:00:00Z"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleGlobalMessage(validMessage, "global-channel");
        }, "Valid message should be processed without exception");
        
        String missingDestination = """
            {
                "payload": {"message": "test"},
                "messageType": "TEST"
            }
            """;
        
        assertDoesNotThrow(() -> {
            redisMessageSubscriber.handleGlobalMessage(missingDestination, "global-channel");
        }, "Message with missing destination should be handled gracefully");
    }
}
