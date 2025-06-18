package com.lapxpert.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lapxpert.backend.common.config.CommonBeansConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the Jackson configuration properly handles Instant serialization
 * for WebSocket Redis Pub/Sub integration with UTC timezone support.
 *
 * This test simulates the updated scenario:
 * WebSocketIntegrationService creating messages with Instant timestamps
 * and serializing them for Redis Pub/Sub with better distributed system compatibility.
 */
public class WebSocketSerializationFixTest {

    @Test
    public void testWebSocketIntegrationServiceMessageSerialization() throws Exception {
        // Use the same ObjectMapper configuration as CommonBeansConfig
        CommonBeansConfig commonBeansConfig = new CommonBeansConfig();
        ObjectMapper objectMapper = commonBeansConfig.objectMapper();
        
        // Simulate the exact message structure created by WebSocketIntegrationService.sendPriceUpdate()
        Map<String, Object> message = new HashMap<>();
        message.put("destination", "/topic/gia-san-pham/12345");
        message.put("messageType", "PRICE_UPDATE");
        message.put("sourceService", "MAIN_APPLICATION");
        message.put("timestamp", Instant.now());

        // Simulate the payload structure that includes Instant
        Map<String, Object> payload = new HashMap<>();
        payload.put("variantId", "12345");
        payload.put("newPrice", 999.99);
        payload.put("message", "Giá sản phẩm đã được cập nhật");
        payload.put("timestamp", Instant.now()); // Now uses Instant for better distributed system compatibility
        
        message.put("payload", payload);
        
        // This should NOT throw InvalidDefinitionException anymore
        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(message),
                "WebSocket message with Instant should serialize without error");
        
        assertNotNull(json, "Serialized JSON should not be null");
        assertTrue(json.contains("gia-san-pham"), "JSON should contain Vietnamese topic");
        assertTrue(json.contains("12345"), "JSON should contain variant ID");
        assertTrue(json.contains("PRICE_UPDATE"), "JSON should contain message type");
        
        // Verify the timestamp is serialized as ISO-8601 string, not timestamp
        assertFalse(json.contains("\"timestamp\":1"), "Timestamp should not be numeric");
        assertTrue(json.contains("\"timestamp\":\""), "Timestamp should be string");
        
        System.out.println("✅ Successfully serialized WebSocket message: " + json);
    }

    @Test
    public void testVoucherNotificationSerialization() throws Exception {
        CommonBeansConfig commonBeansConfig = new CommonBeansConfig();
        ObjectMapper objectMapper = commonBeansConfig.objectMapper();
        
        // Simulate WebSocketIntegrationService.sendVoucherNotification()
        Map<String, Object> message = new HashMap<>();
        message.put("destination", "/topic/phieu-giam-gia/VOUCHER123");
        message.put("messageType", "VOUCHER_NOTIFICATION");
        message.put("sourceService", "MAIN_APPLICATION");
        message.put("timestamp", Instant.now());

        Map<String, Object> notification = new HashMap<>();
        notification.put("voucherId", "VOUCHER123");
        notification.put("voucherType", "PHIEU_GIAM_GIA");
        notification.put("message", "Phiếu giảm giá mới có sẵn");
        notification.put("timestamp", Instant.now());
        
        message.put("payload", notification);
        
        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(message),
                "Voucher notification with Instant should serialize without error");
        
        assertNotNull(json);
        assertTrue(json.contains("phieu-giam-gia"), "Should contain Vietnamese voucher topic");
        assertTrue(json.contains("VOUCHER123"), "Should contain voucher ID");
        
        System.out.println("✅ Successfully serialized voucher notification: " + json);
    }

    @Test
    public void testSystemNotificationSerialization() throws Exception {
        CommonBeansConfig commonBeansConfig = new CommonBeansConfig();
        ObjectMapper objectMapper = commonBeansConfig.objectMapper();
        
        // Simulate WebSocketIntegrationService.sendSystemNotification()
        Map<String, Object> message = new HashMap<>();
        message.put("destination", "/topic/system/notifications");
        message.put("messageType", "SYSTEM_NOTIFICATION");
        message.put("sourceService", "MAIN_APPLICATION");
        message.put("timestamp", Instant.now());

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", "Hệ thống sẽ bảo trì trong 5 phút");
        notification.put("level", "INFO");
        notification.put("timestamp", Instant.now());
        
        message.put("payload", notification);
        
        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(message),
                "System notification with Instant should serialize without error");
        
        assertNotNull(json);
        assertTrue(json.contains("system/notifications"), "Should contain system topic");
        assertTrue(json.contains("Hệ thống"), "Should contain Vietnamese message");
        
        System.out.println("✅ Successfully serialized system notification: " + json);
    }

    @Test
    public void testComplexNestedStructureWithMultipleTimestamps() throws Exception {
        CommonBeansConfig commonBeansConfig = new CommonBeansConfig();
        ObjectMapper objectMapper = commonBeansConfig.objectMapper();
        
        // Test complex nested structure with multiple Instant fields
        Map<String, Object> message = new HashMap<>();
        message.put("destination", "/topic/orders/ORDER123");
        message.put("messageType", "ORDER_UPDATE");
        message.put("sourceService", "MAIN_APPLICATION");
        message.put("timestamp", Instant.now());

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "ORDER123");
        orderData.put("status", "CONFIRMED");
        orderData.put("createdAt", Instant.now().minusSeconds(3600)); // 1 hour ago
        orderData.put("updatedAt", Instant.now());
        orderData.put("estimatedDelivery", Instant.now().plusSeconds(259200)); // 3 days later

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Nguyễn Văn A");
        customer.put("lastLogin", Instant.now().minusSeconds(1800)); // 30 minutes ago
        
        orderData.put("customer", customer);
        message.put("payload", orderData);
        
        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(message),
                "Complex nested structure with multiple Instant fields should serialize");
        
        assertNotNull(json);
        assertTrue(json.contains("ORDER123"), "Should contain order ID");
        assertTrue(json.contains("Nguyễn Văn A"), "Should contain Vietnamese customer name");
        
        // Verify all timestamps are serialized as strings
        long timestampCount = json.split("\"timestamp\":\"").length - 1;
        long dateTimeCount = json.split("At\":\"").length - 1; // createdAt, updatedAt
        long loginCount = json.split("Login\":\"").length - 1; // lastLogin
        long deliveryCount = json.split("Delivery\":\"").length - 1; // estimatedDelivery
        
        assertTrue(timestampCount + dateTimeCount + loginCount + deliveryCount >= 5,
                "All Instant fields should be serialized as strings with UTC timezone");
        
        System.out.println("✅ Successfully serialized complex order structure: " + json);
    }
}
