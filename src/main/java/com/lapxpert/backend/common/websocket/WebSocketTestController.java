package com.lapxpert.backend.common.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket test controller for verifying real-time messaging functionality.
 * Provides endpoints for testing Vietnamese topic naming conventions.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle messages sent to /app/test-message and broadcast to /topic/test-response
     */
    @MessageMapping("/test-message")
    @SendTo("/topic/test-response")
    public TestMessage handleTestMessage(TestMessage message) {
        log.info("Received WebSocket test message: {}", message.getContent());
        
        TestMessage response = new TestMessage();
        response.setContent("Echo: " + message.getContent());
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.setSender("Server");
        
        return response;
    }

    /**
     * Test Vietnamese topic naming - price update simulation
     */
    @MessageMapping("/gia-san-pham")
    @SendTo("/topic/gia-san-pham/test")
    public PriceUpdateMessage handlePriceUpdate(PriceUpdateMessage message) {
        log.info("Received price update for variant: {}", message.getVariantId());

        PriceUpdateMessage response = new PriceUpdateMessage();
        response.setVariantId(message.getVariantId());
        response.setNewPrice(message.getNewPrice());
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.setMessage("Giá sản phẩm đã được cập nhật");

        return response;
    }

    /**
     * Test voucher suggestion WebSocket topic
     */
    @MessageMapping("/phieu-giam-gia/suggestions")
    @SendTo("/topic/phieu-giam-gia/suggestions")
    public VoucherSuggestionMessage handleVoucherSuggestion(VoucherSuggestionMessage message) {
        log.info("Received voucher suggestion for customer: {}", message.getCustomerId());

        VoucherSuggestionMessage response = new VoucherSuggestionMessage();
        response.setCustomerId(message.getCustomerId());
        response.setVoucherCode(message.getVoucherCode());
        response.setSavingsAmount(message.getSavingsAmount());
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.setMessage("Tìm thấy voucher tốt hơn: " + message.getVoucherCode());

        return response;
    }

    /**
     * Simple DTO for test messages
     */
    public static class TestMessage {
        private String content;
        private String timestamp;
        private String sender;

        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
    }

    /**
     * Simple DTO for price update messages
     */
    public static class PriceUpdateMessage {
        private String variantId;
        private Double newPrice;
        private String timestamp;
        private String message;

        // Getters and setters
        public String getVariantId() { return variantId; }
        public void setVariantId(String variantId) { this.variantId = variantId; }
        
        public Double getNewPrice() { return newPrice; }
        public void setNewPrice(Double newPrice) { this.newPrice = newPrice; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Simple DTO for voucher suggestion messages
     */
    public static class VoucherSuggestionMessage {
        private Long customerId;
        private String voucherCode;
        private Double savingsAmount;
        private String timestamp;
        private String message;

        // Getters and setters
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }

        public String getVoucherCode() { return voucherCode; }
        public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

        public Double getSavingsAmount() { return savingsAmount; }
        public void setSavingsAmount(Double savingsAmount) { this.savingsAmount = savingsAmount; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

/**
 * REST controller for testing WebSocket broadcasting from HTTP endpoints
 */
@RestController
@RequestMapping("/api/v1/websocket-test")
@RequiredArgsConstructor
@Slf4j
class WebSocketTestRestController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Test endpoint to broadcast messages to WebSocket topics
     */
    @PostMapping("/broadcast")
    public String broadcastMessage(@RequestParam String topic, @RequestParam String message) {
        try {
            WebSocketTestController.TestMessage testMessage = new WebSocketTestController.TestMessage();
            testMessage.setContent(message);
            testMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            testMessage.setSender("REST API");
            
            messagingTemplate.convertAndSend("/topic/" + topic, testMessage);
            
            log.info("Broadcasted message to topic: /topic/{}", topic);
            return "Message broadcasted successfully to /topic/" + topic;
        } catch (Exception e) {
            log.error("Failed to broadcast message: {}", e.getMessage());
            return "Failed to broadcast message: " + e.getMessage();
        }
    }

    /**
     * Test Vietnamese topic broadcasting
     */
    @PostMapping("/broadcast-gia-san-pham")
    public String broadcastPriceUpdate(@RequestParam String variantId, @RequestParam Double newPrice) {
        try {
            WebSocketTestController.PriceUpdateMessage priceMessage = new WebSocketTestController.PriceUpdateMessage();
            priceMessage.setVariantId(variantId);
            priceMessage.setNewPrice(newPrice);
            priceMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            priceMessage.setMessage("Giá sản phẩm variant " + variantId + " đã thay đổi thành " + newPrice);
            
            messagingTemplate.convertAndSend("/topic/gia-san-pham/" + variantId, priceMessage);
            
            log.info("Broadcasted price update for variant: {}", variantId);
            return "Price update broadcasted successfully for variant: " + variantId;
        } catch (Exception e) {
            log.error("Failed to broadcast price update: {}", e.getMessage());
            return "Failed to broadcast price update: " + e.getMessage();
        }
    }
}
