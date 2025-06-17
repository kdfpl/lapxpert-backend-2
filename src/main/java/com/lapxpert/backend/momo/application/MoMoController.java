package com.lapxpert.backend.momo.application;

import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.payment.domain.service.BasePaymentController;
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
 * Enhanced MoMo payment controller with improved security, error handling, and audit logging.
 * Extends BasePaymentController for common payment functionality and Vietnamese business requirements.
 *
 * Security enhancements:
 * - Enhanced parameter validation through BasePaymentController
 * - Improved error handling with security-conscious responses
 * - Better IP address detection and logging
 * - Enhanced IPN processing with comprehensive validation
 * - Proper audit logging for all payment operations
 *
 * v3 API Features:
 * - Support for enhanced payment creation
 * - Multiple response format handling
 * - Improved callback processing
 */
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/payment")
public class MoMoController extends BasePaymentController<MoMoGatewayService> {

    private static final long MAX_MOMO_AMOUNT = 50_000_000L; // 50 million VND
    private final HoaDonService hoaDonService;

    public MoMoController(MoMoGatewayService moMoGatewayService, HoaDonService hoaDonService) {
        super(moMoGatewayService);
        this.hoaDonService = hoaDonService;
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
                paymentGatewayService.verifyPaymentWithCallback(callbackData);
            
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
     * Enhanced MoMo IPN (Instant Payment Notification) handler with comprehensive security validation.
     *
     * @param ipnData MoMo IPN data
     * @param request HTTP request for IP detection
     * @return Response to MoMo
     */
    @PostMapping("/momo-ipn")
    public ResponseEntity<Map<String, String>> handlePaymentIPN(@RequestBody Map<String, String> ipnData,
                                                               HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String orderId = ipnData.get("orderId");
        String transId = ipnData.get("transId");
        String amount = ipnData.get("amount");

        // Validate callback request using base controller
        if (!validateCallbackRequest(request)) {
            Map<String, String> response = new HashMap<>();
            response.put("resultCode", "1");
            response.put("message", "Invalid IPN request");
            return ResponseEntity.ok(response);
        }

        // Create audit log for IPN processing
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("transId", transId);
        auditInfo.put("amount", amount);
        auditInfo.put("ipnType", "INSTANT_NOTIFICATION");
        createAuditLog("PROCESS_IPN", orderId, amount != null ? Long.parseLong(amount) : null, clientIp, auditInfo);

        log.info("MoMo IPN received from IP: {} - OrderID: {}, TransID: {}",
                clientIp, orderId, transId);

        Map<String, String> response = new HashMap<>();

        try {
            // Enhanced parameter validation
            if (orderId == null || orderId.trim().isEmpty()) {
                log.error("MoMo IPN missing orderId from IP: {}", clientIp);
                response.put("resultCode", "1");
                response.put("message", "Missing Order ID");
                return ResponseEntity.ok(response);
            }

            // Verify payment using enhanced verification
            PaymentGatewayService.PaymentVerificationResult verificationResult =
                paymentGatewayService.verifyPaymentWithCallback(ipnData);

            if (verificationResult.isValid() && verificationResult.isSuccessful()) {
                // Payment successful - update order status
                try {
                    Long orderIdLong = Long.parseLong(orderId);
                    hoaDonService.confirmPayment(orderIdLong, PhuongThucThanhToan.MOMO);

                    log.info("Order {} payment confirmed successfully via MoMo IPN from IP: {}",
                            orderId, clientIp);
                    response.put("resultCode", "0");
                    response.put("message", "Confirm Success");

                } catch (NumberFormatException e) {
                    log.error("Invalid order ID in MoMo IPN from IP: {} - OrderID: {}", clientIp, orderId);
                    response.put("resultCode", "1");
                    response.put("message", "Invalid Order ID");

                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {} from IP: {} - {}",
                             orderId, clientIp, e.getMessage(), e);
                    response.put("resultCode", "2");
                    response.put("message", "Order Confirm Failed");
                }

            } else {
                // Payment failed or invalid
                log.warn("MoMo payment verification failed for order {} from IP: {} - Error: {}",
                        orderId, clientIp, verificationResult.getErrorMessage());
                response.put("resultCode", "1");
                response.put("message", "Payment Verification Failed");
            }

        } catch (Exception e) {
            log.error("Error processing MoMo IPN from IP: {} - OrderID: {} - {}",
                     clientIp, orderId, e.getMessage(), e);
            response.put("resultCode", "99");
            response.put("message", "System Error");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Create MoMo payment URL for order
     * @param orderId Order ID (mã đơn hàng)
     * @param amount Payment amount in VND (số tiền thanh toán)
     * @param orderInfo Order information (thông tin đơn hàng)
     * @param request HTTP request for base URL
     * @return Payment URL response
     */
    @PostMapping("/momo/create-payment")
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestParam("orderId") Long orderId,
            @RequestParam("amount") int amount,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);

            // Validate payment parameters using base controller
            validatePaymentParameters(amount, orderInfo, orderId.toString(),
                request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());

            // Create audit log for payment creation
            Map<String, Object> auditInfo = new HashMap<>();
            auditInfo.put("userAgent", request.getHeader("User-Agent"));
            auditInfo.put("referer", request.getHeader("Referer"));
            createAuditLog("CREATE_PAYMENT", orderId.toString(), (long) amount, clientIp, auditInfo);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String paymentUrl = paymentGatewayService.createPaymentUrl(orderId, amount, orderInfo, baseUrl, clientIp);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("paymentUrl", paymentUrl);
            responseData.put("orderId", orderId.toString());
            responseData.put("paymentMethod", "MOMO");

            return createSuccessResponse(responseData, "Tạo liên kết thanh toán MoMo thành công");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid MoMo payment parameters for order {}: {}", orderId, e.getMessage());
            return createErrorResponse(e.getMessage(), "INVALID_PARAMETERS",
                org.springframework.http.HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error creating MoMo payment for order {}: {}", orderId, e.getMessage(), e);
            return createErrorResponse("Không thể tạo liên kết thanh toán MoMo", "PAYMENT_CREATION_FAILED",
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
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
            Map<String, Object> statusResult = paymentGatewayService.queryTransactionStatus(orderId, requestId);
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

    // IP address detection is now handled by BasePaymentController.getClientIpAddress()
}
