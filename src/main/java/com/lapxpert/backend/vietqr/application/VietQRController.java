package com.lapxpert.backend.vietqr.application;

import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.payment.domain.service.VietQRGatewayService;
import com.lapxpert.backend.payment.domain.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * VietQR payment controller for handling payment processing and verification
 * Handles VietQR bank transfer integration with Vietnamese business terminology
 */
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/payment")
public class VietQRController {

    private final VietQRGatewayService vietQRGatewayService;
    private final HoaDonService hoaDonService;

    public VietQRController(VietQRGatewayService vietQRGatewayService, HoaDonService hoaDonService) {
        this.vietQRGatewayService = vietQRGatewayService;
        this.hoaDonService = hoaDonService;
    }

    /**
     * Handle VietQR payment return (user redirect after payment)
     * @param request HTTP request containing VietQR callback parameters
     * @param response HTTP response for redirect
     */
    @GetMapping("/vietqr-payment")
    public void handlePaymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Extract VietQR callback parameters
            String orderId = request.getParameter("orderId");
            String transactionId = request.getParameter("transactionId");
            String status = request.getParameter("status");
            String amount = request.getParameter("amount");
            String message = request.getParameter("message");
            
            // Build redirect URL to frontend
            String redirectUrl = "http://localhost:5173/orders/create?" +
                    "status=" + (("SUCCESS".equals(status) || "COMPLETED".equals(status)) ? "success" : "failed") +
                    "&transactionId=" + URLEncoder.encode(transactionId != null ? transactionId : "", StandardCharsets.UTF_8) +
                    "&orderId=" + URLEncoder.encode(orderId != null ? orderId : "", StandardCharsets.UTF_8) +
                    "&amount=" + URLEncoder.encode(amount != null ? amount : "", StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode(message != null ? message : "", StandardCharsets.UTF_8) +
                    "&paymentMethod=VIETQR";

            log.info("VietQR payment return processed for order {}: {}", orderId, status);
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error handling VietQR payment return: {}", e.getMessage(), e);
            String errorRedirectUrl = "http://localhost:5173/orders/create?status=error&message=" + 
                URLEncoder.encode("Lỗi xử lý thanh toán VietQR", StandardCharsets.UTF_8);
            response.sendRedirect(errorRedirectUrl);
        }
    }

    /**
     * Handle VietQR payment notification (bank webhook or manual confirmation)
     * @param request HTTP request containing VietQR transaction data
     * @return Response to bank or confirmation system
     */
    @PostMapping("/vietqr-ipn")
    public ResponseEntity<Map<String, String>> handlePaymentNotification(@RequestBody Map<String, String> transactionData) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Received VietQR payment notification: {}", transactionData);
            
            // Verify payment
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                vietQRGatewayService.verifyPaymentWithTransactionData(transactionData);
            
            String orderId = transactionData.get("orderId");
            String status = transactionData.get("status");
            
            if (verificationResult.isValid() && verificationResult.isSuccessful()) {
                // Payment successful - update order status
                try {
                    Long orderIdLong = Long.parseLong(orderId);
                    hoaDonService.confirmPayment(orderIdLong, PhuongThucThanhToan.VIETQR);
                    
                    log.info("Order {} payment confirmed successfully via VietQR notification", orderId);
                    response.put("resultCode", "0");
                    response.put("message", "Confirm Success");
                    
                } catch (NumberFormatException e) {
                    log.error("Invalid order ID in VietQR notification: {}", orderId);
                    response.put("resultCode", "1");
                    response.put("message", "Invalid Order ID");
                    
                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {}: {}", orderId, e.getMessage());
                    response.put("resultCode", "2");
                    response.put("message", "Order Confirm Failed");
                }
                
            } else {
                // Payment failed or invalid
                log.warn("VietQR payment verification failed for order {}: {}", orderId, verificationResult.getErrorMessage());
                response.put("resultCode", "1");
                response.put("message", "Payment Verification Failed");
            }
            
        } catch (Exception e) {
            log.error("Error processing VietQR payment notification: {}", e.getMessage(), e);
            response.put("resultCode", "99");
            response.put("message", "System Error");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create VietQR payment for order
     * @param orderId Order ID
     * @param amount Payment amount
     * @param orderInfo Order information
     * @param request HTTP request for base URL
     * @return VietQR payment instructions
     */
    @PostMapping("/vietqr/create-payment")
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") int amount,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {
        
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String clientIp = getClientIpAddress(request);
            
            // Create VietQR payment URL (QR code image)
            String qrUrl = vietQRGatewayService.createPaymentUrl(orderId, amount, orderInfo, baseUrl, clientIp);
            
            // Generate payment instructions
            Map<String, Object> paymentInstructions = vietQRGatewayService.generatePaymentInstructions(orderId.toString(), amount);
            paymentInstructions.put("qrUrl", qrUrl);
            paymentInstructions.put("orderId", orderId.toString());
            paymentInstructions.put("paymentMethod", "VIETQR");
            
            return ResponseEntity.ok(paymentInstructions);
            
        } catch (Exception e) {
            log.error("Error creating VietQR payment for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tạo thanh toán VietQR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Check VietQR payment status
     * @param orderId Order ID
     * @param expectedAmount Expected payment amount
     * @param bankTransactionId Bank transaction ID
     * @return Payment status
     */
    @GetMapping("/vietqr/check-status")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @RequestParam("orderId") String orderId,
            @RequestParam("expectedAmount") long expectedAmount,
            @RequestParam(value = "bankTransactionId", required = false) String bankTransactionId) {
        
        try {
            Map<String, Object> statusResult = vietQRGatewayService.checkPaymentStatus(orderId, expectedAmount, bankTransactionId);
            return ResponseEntity.ok(statusResult);
            
        } catch (Exception e) {
            log.error("Error checking VietQR payment status for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Lỗi kiểm tra trạng thái thanh toán: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Manual payment confirmation for VietQR
     * @param confirmationData Payment confirmation data
     * @return Confirmation result
     */
    @PostMapping("/vietqr/confirm-payment")
    public ResponseEntity<Map<String, String>> confirmPayment(@RequestBody Map<String, String> confirmationData) {
        Map<String, String> response = new HashMap<>();
        
        try {
            String orderId = confirmationData.get("orderId");
            String bankTransactionId = confirmationData.get("bankTransactionId");
            String amount = confirmationData.get("amount");
            
            // Validate required fields
            if (orderId == null || bankTransactionId == null || amount == null) {
                response.put("resultCode", "1");
                response.put("message", "Missing required fields");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create transaction data for verification
            Map<String, String> transactionData = new HashMap<>();
            transactionData.put("orderId", orderId);
            transactionData.put("transactionId", bankTransactionId);
            transactionData.put("amount", amount);
            transactionData.put("status", "SUCCESS");
            transactionData.put("bankTransactionId", bankTransactionId);
            
            // Verify and confirm payment
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                vietQRGatewayService.verifyPaymentWithTransactionData(transactionData);
            
            if (verificationResult.isValid() && verificationResult.isSuccessful()) {
                Long orderIdLong = Long.parseLong(orderId);
                hoaDonService.confirmPayment(orderIdLong, PhuongThucThanhToan.VIETQR);
                
                log.info("VietQR payment manually confirmed for order {}", orderId);
                response.put("resultCode", "0");
                response.put("message", "Payment confirmed successfully");
            } else {
                response.put("resultCode", "1");
                response.put("message", "Payment verification failed");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error confirming VietQR payment: {}", e.getMessage(), e);
            response.put("resultCode", "99");
            response.put("message", "System error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get client IP address from request
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
