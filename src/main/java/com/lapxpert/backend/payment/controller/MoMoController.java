package com.lapxpert.backend.payment.controller;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.service.HoaDonService;
import com.lapxpert.backend.payment.service.MoMoGatewayService;
import com.lapxpert.backend.payment.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Enhanced MoMo controller with SDK integration and improved security.
 * Extends BasePaymentController for common payment functionality and Vietnamese business requirements.
 * 
 * This controller is conditionally registered based on the momo.sdk.enabled property.
 * When disabled, MoMo payment endpoints will not be available.
 *
 * Security enhancements:
 * - SDK-based signature validation and verification
 * - Enhanced parameter validation through BasePaymentController
 * - Improved error handling with security-conscious responses
 * - Better IP address detection and logging
 * - Enhanced IPN processing with comprehensive validation
 * - Proper audit logging for all payment operations
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "momo.sdk.enabled", havingValue = "true", matchIfMissing = false)
public class MoMoController extends BasePaymentController<MoMoGatewayService> {

    private static final long MAX_MOMO_AMOUNT = 50_000_000L; // 50 million VND
    private final HoaDonService hoaDonService;

    public MoMoController(MoMoGatewayService moMoGatewayService, HoaDonService hoaDonService) {
        super(moMoGatewayService);
        this.hoaDonService = hoaDonService;
        log.info("MoMo Controller initialized with SDK integration and enhanced security");
    }

    @Override
    protected String getGatewayName() {
        return "MoMo";
    }

    @Override
    protected long getMaxPaymentAmount() {
        return MAX_MOMO_AMOUNT;
    }

    /**
     * Enhanced payment return handler for MoMo payment completion.
     * Handles user return from MoMo payment page with improved security and error handling.
     *
     * @param request HTTP request containing MoMo response parameters
     * @param response HTTP response for redirect
     * @throws IOException if redirect fails
     */
    @GetMapping("/momo-payment")
    public void handlePaymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = getClientIpAddress(request);
        String orderId = request.getParameter("orderId");
        String transId = request.getParameter("transId");
        String resultCode = request.getParameter("resultCode");
        String message = request.getParameter("message");

        log.info("MoMo payment return from IP: {} - OrderId: {}, TransId: {}, ResultCode: {}",
                clientIp, orderId, transId, resultCode);

        try {
            // Create audit log for payment return
            Map<String, Object> auditInfo = new HashMap<>();
            auditInfo.put("transId", transId);
            auditInfo.put("resultCode", resultCode);
            auditInfo.put("message", message);
            auditInfo.put("returnType", "USER_RETURN");
            createAuditLog("PROCESS_RETURN", orderId, null, clientIp, auditInfo);

            // Prepare callback data for verification
            Map<String, String> callbackData = new HashMap<>();
            callbackData.put("orderId", orderId);
            callbackData.put("transId", transId);
            callbackData.put("resultCode", resultCode);
            callbackData.put("message", message);

            // Verify payment using SDK
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                paymentGatewayService.verifyPaymentWithCallback(callbackData);

            if (verificationResult.isValid() && verificationResult.isSuccessful()) {
                // Payment successful - redirect to success page with original order ID
                try {
                    Long originalOrderId = ((MoMoGatewayService) paymentGatewayService).extractOrderIdFromMoMoOrderId(orderId);
                    String successRedirectUrl = "http://localhost:5173/orders/create?status=success&orderId=" + originalOrderId;
                    response.sendRedirect(successRedirectUrl);
                    log.info("MoMo payment successful for order {} - redirecting to success page (MoMo OrderId: {})", originalOrderId, orderId);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid order ID format in MoMo return - MoMo OrderId: {} - Error: {}", orderId, e.getMessage());
                    String errorRedirectUrl = "http://localhost:5173/orders/create?status=error&message=" +
                        URLEncoder.encode("Lỗi định dạng mã đơn hàng", StandardCharsets.UTF_8);
                    response.sendRedirect(errorRedirectUrl);
                }
            } else {
                // Payment failed - redirect to error page with Vietnamese message
                String errorMessage = verificationResult.getErrorMessage() != null ?
                    verificationResult.getErrorMessage() : "Thanh toán MoMo thất bại";
                String errorRedirectUrl = "http://localhost:5173/orders/create?status=error&message=" +
                    URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
                response.sendRedirect(errorRedirectUrl);
                log.warn("MoMo payment failed for order {} - redirecting to error page: {}", orderId, errorMessage);
            }
            
        } catch (Exception e) {
            log.error("Error handling MoMo payment return for order {}: {}", orderId, e.getMessage(), e);
            String errorRedirectUrl = "http://localhost:5173/orders/create?status=error&message=" + 
                URLEncoder.encode("Lỗi xử lý thanh toán MoMo", StandardCharsets.UTF_8);
            response.sendRedirect(errorRedirectUrl);
        }
    }

    /**
     * Enhanced IPN (Instant Payment Notification) endpoint for server-to-server MoMo notifications.
     * This endpoint automatically updates order status when payments are completed with enhanced security.
     * Uses SDK verification methods for proper signature validation.
     */
    @PostMapping("/momo-ipn")
    public ResponseEntity<Map<String, String>> handleMoMoIPN(@RequestBody Map<String, String> ipnData,
                                                             HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String orderId = ipnData.get("orderId");
        String transId = ipnData.get("transId");
        String resultCode = ipnData.get("resultCode");
        String message = ipnData.get("message");

        // Validate callback request using base controller
        if (!validateCallbackRequest(request)) {
            Map<String, String> response = new HashMap<>();
            response.put("resultCode", "97");
            response.put("message", "Yêu cầu IPN không hợp lệ");
            return ResponseEntity.ok(response);
        }

        // Create audit log for IPN processing
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("transId", transId);
        auditInfo.put("resultCode", resultCode);
        auditInfo.put("message", message);
        auditInfo.put("ipnType", "INSTANT_NOTIFICATION");
        createAuditLog("PROCESS_IPN", orderId, null, clientIp, auditInfo);

        log.info("MoMo IPN received from IP: {} - OrderId: {}, TransId: {}, ResultCode: {}",
                clientIp, orderId, transId, resultCode);

        Map<String, String> response = new HashMap<>();

        try {
            // Use SDK verification with full callback data
            PaymentGatewayService.PaymentVerificationResult verificationResult = 
                paymentGatewayService.verifyPaymentWithCallback(ipnData);

            if (!verificationResult.isValid()) {
                log.error("MoMo IPN validation failed from IP: {} - OrderId: {} - Error: {}",
                         clientIp, orderId, verificationResult.getErrorMessage());
                response.put("resultCode", "97");
                response.put("message", "Xác thực IPN thất bại");
                return ResponseEntity.ok(response);
            }

            if (verificationResult.isSuccessful()) {
                // Payment successful - update order status
                try {
                    // Extract original order ID from MoMo order ID (handles both old and new formats)
                    Long orderIdLong = ((MoMoGatewayService) paymentGatewayService).extractOrderIdFromMoMoOrderId(orderId);
                    hoaDonService.confirmPayment(orderIdLong, PhuongThucThanhToan.MOMO);

                    log.info("Order {} payment confirmed successfully via MoMo IPN from IP: {} (MoMo OrderId: {})",
                            orderIdLong, clientIp, orderId);
                    response.put("resultCode", "0");
                    response.put("message", "Xác nhận thành công");

                } catch (IllegalArgumentException e) {
                    log.error("Invalid order ID format in MoMo IPN from IP: {} - MoMo OrderId: {} - Error: {}",
                             clientIp, orderId, e.getMessage());
                    response.put("resultCode", "1");
                    response.put("message", "Mã đơn hàng không hợp lệ");

                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {} from IP: {} - {}",
                             orderId, clientIp, e.getMessage(), e);
                    response.put("resultCode", "2");
                    response.put("message", "Xác nhận đơn hàng thất bại");
                }
            } else {
                // Payment failed
                log.warn("MoMo payment failed from IP: {} - OrderId: {}, Message: {}",
                        clientIp, orderId, verificationResult.getErrorMessage());
                response.put("resultCode", "0");
                response.put("message", "Thanh toán thất bại");
            }

        } catch (Exception e) {
            log.error("Error processing MoMo IPN from IP: {} - OrderId: {} - {}",
                     clientIp, orderId, e.getMessage(), e);
            response.put("resultCode", "99");
            response.put("message", "Lỗi không xác định");
        }

        return ResponseEntity.ok(response);
    }
}
