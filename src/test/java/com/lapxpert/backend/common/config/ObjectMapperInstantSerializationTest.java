package com.lapxpert.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the centralized ObjectMapper from CommonBeansConfig
 * properly handles Instant serialization for Redis Pub/Sub and WebSocket integration.
 *
 * Verifies that Instant objects are serialized to ISO-8601 UTC format with Z suffix
 * for better distributed system compatibility and timezone handling.
 */
public class ObjectMapperInstantSerializationTest {

    private final CommonBeansConfig commonBeansConfig = new CommonBeansConfig();
    private final ObjectMapper objectMapper = commonBeansConfig.objectMapper();

    @Test
    public void testInstantSerializationFormat() throws Exception {
        // Create a test Instant
        Instant testInstant = Instant.parse("2024-06-18T12:30:45.123Z");
        
        // Serialize the Instant
        String json = objectMapper.writeValueAsString(testInstant);

        // Verify the format is ISO-8601 with Z suffix (exact match for this specific instant)
        assertEquals("\"2024-06-18T12:30:45.123Z\"", json,
                "Instant should be serialized as ISO-8601 UTC format with Z suffix");
        
        // Verify it can be deserialized back
        Instant deserializedInstant = objectMapper.readValue(json, Instant.class);
        assertEquals(testInstant, deserializedInstant, 
                "Deserialized Instant should match original");
    }

    @Test
    public void testWebSocketMessageStructureSerialization() throws Exception {
        // Create a message structure similar to WebSocketIntegrationService
        Instant now = Instant.now();
        Map<String, Object> message = new HashMap<>();
        message.put("destination", "/topic/gia-san-pham/12345");
        message.put("messageType", "PRICE_UPDATE");
        message.put("sourceService", "MAIN_APPLICATION");
        message.put("timestamp", now);

        Map<String, Object> payload = new HashMap<>();
        payload.put("variantId", "12345");
        payload.put("newPrice", 999.99);
        payload.put("message", "Giá sản phẩm đã được cập nhật");
        payload.put("timestamp", now);

        message.put("payload", payload);

        // Serialize the message
        String json = objectMapper.writeValueAsString(message);

        // Debug: Print the actual JSON to see the format
        System.out.println("DEBUG - WebSocket message JSON: " + json);

        // Verify the JSON contains properly formatted timestamps
        assertNotNull(json, "Serialized JSON should not be null");
        assertTrue(json.contains("\"timestamp\""), "JSON should contain timestamp field");

        // Verify the timestamp format using regex (accounting for pretty-printing with spaces and nanosecond precision)
        Pattern iso8601Pattern = Pattern.compile("\"timestamp\"\\s*:\\s*\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z\"");
        assertTrue(iso8601Pattern.matcher(json).find(),
                "Timestamp should match ISO-8601 UTC format with Z suffix");

        // Verify Vietnamese content is preserved
        assertTrue(json.contains("gia-san-pham"), "Vietnamese topic should be preserved");
        assertTrue(json.contains("Giá sản phẩm đã được cập nhật"), "Vietnamese message should be preserved");

        System.out.println("✅ WebSocket message structure serialized correctly: " + json);
    }

    @Test
    public void testComplexObjectWithMultipleInstants() throws Exception {
        // Create a complex object with multiple Instant fields
        Map<String, Object> complexObject = new HashMap<>();
        complexObject.put("id", "ORDER123");
        complexObject.put("createdAt", Instant.parse("2024-06-18T10:00:00Z"));
        complexObject.put("updatedAt", Instant.parse("2024-06-18T12:30:00Z"));
        complexObject.put("scheduledAt", Instant.parse("2024-06-20T09:00:00Z"));
        
        Map<String, Object> nestedObject = new HashMap<>();
        nestedObject.put("lastAccessed", Instant.parse("2024-06-18T11:15:30.456Z"));
        nestedObject.put("name", "Nguyễn Văn A");
        
        complexObject.put("customer", nestedObject);
        
        // Serialize the complex object
        String json = objectMapper.writeValueAsString(complexObject);
        
        // Verify all Instant fields are properly serialized
        assertNotNull(json, "Serialized JSON should not be null");
        
        // Count the number of properly formatted timestamps (accounting for nanosecond precision)
        Pattern iso8601Pattern = Pattern.compile("\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z\"");
        long timestampCount = iso8601Pattern.matcher(json).results().count();
        assertEquals(4, timestampCount, "Should have 4 properly formatted timestamps");
        
        // Verify Vietnamese content is preserved
        assertTrue(json.contains("Nguyễn Văn A"), "Vietnamese name should be preserved");
        
        System.out.println("✅ Complex object with multiple Instants serialized correctly: " + json);
    }

    @Test
    public void testInstantDeserializationCompatibility() throws Exception {
        // Test that we can deserialize various ISO-8601 formats
        String[] testTimestamps = {
                "\"2024-06-18T12:30:45Z\"",
                "\"2024-06-18T12:30:45.123Z\"",
                "\"2024-06-18T12:30:45.123456Z\""
        };
        
        for (String timestampJson : testTimestamps) {
            Instant instant = assertDoesNotThrow(() -> objectMapper.readValue(timestampJson, Instant.class),
                    "Should be able to deserialize timestamp: " + timestampJson);
            assertNotNull(instant, "Deserialized Instant should not be null");
            
            // Verify it can be serialized back
            String serialized = objectMapper.writeValueAsString(instant);
            assertNotNull(serialized, "Re-serialized timestamp should not be null");
            assertTrue(serialized.endsWith("Z\""), "Re-serialized timestamp should end with Z");
        }
    }

    @Test
    public void testRedisMessageCompatibility() throws Exception {
        // Test message format that would be sent through Redis Pub/Sub
        Map<String, Object> redisMessage = new HashMap<>();
        redisMessage.put("destination", "/topic/phieu-giam-gia/VOUCHER123");
        redisMessage.put("messageType", "VOUCHER_NOTIFICATION");
        redisMessage.put("sourceService", "MAIN_APPLICATION");
        redisMessage.put("timestamp", Instant.now());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("voucherId", "VOUCHER123");
        payload.put("voucherType", "PHIEU_GIAM_GIA");
        payload.put("message", "Phiếu giảm giá mới có sẵn");
        payload.put("timestamp", Instant.now());
        
        redisMessage.put("payload", payload);
        
        // Serialize for Redis
        String json = objectMapper.writeValueAsString(redisMessage);

        // Debug: Print the actual JSON to see the format
        System.out.println("DEBUG - Redis message JSON: " + json);

        // Verify the message can be serialized without errors
        assertNotNull(json, "Redis message should serialize successfully");
        assertTrue(json.contains("phieu-giam-gia"), "Vietnamese topic should be preserved");
        assertTrue(json.contains("Phiếu giảm giá"), "Vietnamese message should be preserved");

        // Verify timestamps are in correct format (accounting for nanosecond precision)
        Pattern iso8601Pattern = Pattern.compile("\"timestamp\"\\s*:\\s*\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?Z\"");
        assertTrue(iso8601Pattern.matcher(json).find(),
                "Should contain properly formatted timestamps");
        
        // Verify the message can be deserialized back
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedMessage = objectMapper.readValue(json, Map.class);
        assertNotNull(deserializedMessage, "Message should deserialize successfully");
        assertEquals("VOUCHER_NOTIFICATION", deserializedMessage.get("messageType"));
        
        System.out.println("✅ Redis message compatibility verified: " + json);
    }
}
