package com.lapxpert.backend.momo.application;

import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.payment.domain.service.MoMoGatewayService;
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
 * MoMo payment controller for handling payment callbacks and processing
 * Handles MoMo payment integration with Vietnamese business terminology
 */
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/payment")
public class MoMoController {

    private final MoMoGatewayService moMoGatewayService;
    private final HoaDonService hoaDonService;

    public MoMoController(MoMoGatewayService moMoGatewayService, HoaDonService hoaDonService) {
        this.moMoGatewayService = moMoGatewayService;
        this.hoaDonService = hoaDonService;
    }

    /**
     * Handle MoMo payment return (user redirect after payment)
     * @param request HTTP request containing MoMo callback parameters
     * @param response HTTP response for redirect
     */
    @GetMapping("/momo-payment")
    public void handlePaymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Extract MoMo callback parameters
            Map<String, String> callbackData = extractCallbackParameters(request);
            
            // Verify payment
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                moMoGatewayService.verifyPaymentWithCallback(callbackData);
            
            String orderId = callbackData.get("orderId");
            String transId = callbackData.get("transId");
            String resultCode = callbackData.get("resultCode");
            String message = callbackData.get("message");
            
            // Build redirect URL to frontend
            String redirectUrl = "http://localhost:5173/orders/create?" +
                    "status=" + (verificationResult.isSuccessful() ? "success" : "failed") +
                    "&transactionId=" + URLEncoder.encode(transId != null ? transId : "", StandardCharsets.UTF_8) +
                    "&orderId=" + URLEncoder.encode(orderId != null ? orderId : "", StandardCharsets.UTF_8) +
                    "&resultCode=" + URLEncoder.encode(resultCode != null ? resultCode : "", StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode(message != null ? message : "", StandardCharsets.UTF_8) +
                    "&paymentMethod=MOMO";

            log.info("MoMo payment return processed for order {}: {}", orderId, verificationResult.isSuccessful() ? "success" : "failed");
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error handling MoMo payment return: {}", e.getMessage(), e);
            String errorRedirectUrl = "http://localhost:5173/orders/create?status=error&message=" + 
                URLEncoder.encode("Lỗi xử lý thanh toán MoMo", StandardCharsets.UTF_8);
            response.sendRedirect(errorRedirectUrl);
        }
    }

    /**
     * Handle MoMo IPN (Instant Payment Notification)
     * @param request HTTP request containing MoMo IPN data
     * @return Response to MoMo
     */
    @PostMapping("/momo-ipn")
    public ResponseEntity<Map<String, String>> handlePaymentIPN(@RequestBody Map<String, String> ipnData) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Received MoMo IPN: {}", ipnData);
            
            // Verify payment
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                moMoGatewayService.verifyPaymentWithCallback(ipnData);
            
            String orderId = ipnData.get("orderId");
            String resultCode = ipnData.get("resultCode");
            
            if (verificationResult.isValid() && verificationResult.isSuccessful()) {
                // Payment successful - update order status
                try {
                    Long orderIdLong = Long.parseLong(orderId);
                    hoaDonService.confirmPayment(orderIdLong, PhuongThucThanhToan.MOMO);
                    
                    log.info("Order {} payment confirmed successfully via MoMo IPN", orderId);
                    response.put("resultCode", "0");
                    response.put("message", "Confirm Success");
                    
                } catch (NumberFormatException e) {
                    log.error("Invalid order ID in MoMo IPN: {}", orderId);
                    response.put("resultCode", "1");
                    response.put("message", "Invalid Order ID");
                    
                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {}: {}", orderId, e.getMessage());
                    response.put("resultCode", "2");
                    response.put("message", "Order Confirm Failed");
                }
                
            } else {
                // Payment failed or invalid
                log.warn("MoMo payment verification failed for order {}: {}", orderId, verificationResult.getErrorMessage());
                response.put("resultCode", "1");
                response.put("message", "Payment Verification Failed");
            }
            
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: {}", e.getMessage(), e);
            response.put("resultCode", "99");
            response.put("message", "System Error");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create MoMo payment URL for order
     * @param orderId Order ID
     * @param amount Payment amount
     * @param orderInfo Order information
     * @param request HTTP request for base URL
     * @return Payment URL response
     */
    @PostMapping("/momo/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") int amount,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {
        
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String clientIp = getClientIpAddress(request);
            
            String paymentUrl = moMoGatewayService.createPaymentUrl(orderId, amount, orderInfo, baseUrl, clientIp);
            
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("orderId", orderId.toString());
            response.put("paymentMethod", "MOMO");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating MoMo payment for order {}: {}", orderId, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tạo liên kết thanh toán MoMo");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Query MoMo transaction status
     * @param orderId Order ID
     * @param requestId Request ID
     * @return Transaction status
     */
    @GetMapping("/momo/query-status")
    public ResponseEntity<Map<String, Object>> queryTransactionStatus(
            @RequestParam("orderId") String orderId,
            @RequestParam("requestId") String requestId) {
        
        try {
            Map<String, Object> statusResult = moMoGatewayService.queryTransactionStatus(orderId, requestId);
            return ResponseEntity.ok(statusResult);
            
        } catch (Exception e) {
            log.error("Error querying MoMo transaction status for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("resultCode", -1);
            errorResponse.put("message", "Lỗi truy vấn trạng thái giao dịch: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Extract callback parameters from HTTP request
     * @param request HTTP request
     * @return Callback parameters map
     */
    private Map<String, String> extractCallbackParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        
        // Extract all MoMo callback parameters
        params.put("partnerCode", request.getParameter("partnerCode"));
        params.put("orderId", request.getParameter("orderId"));
        params.put("requestId", request.getParameter("requestId"));
        params.put("amount", request.getParameter("amount"));
        params.put("orderInfo", request.getParameter("orderInfo"));
        params.put("orderType", request.getParameter("orderType"));
        params.put("transId", request.getParameter("transId"));
        params.put("resultCode", request.getParameter("resultCode"));
        params.put("message", request.getParameter("message"));
        params.put("payType", request.getParameter("payType"));
        params.put("responseTime", request.getParameter("responseTime"));
        params.put("extraData", request.getParameter("extraData"));
        params.put("signature", request.getParameter("signature"));
        
        return params;
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
