package com.lapxpert.backend.payment.vnpay;

import com.lapxpert.backend.hoadon.service.HoaDonService;
import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.payment.controller.BasePaymentController;
import com.lapxpert.backend.payment.service.VNPayGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced VNPay controller with improved security, error handling, and audit logging.
 * Extends BasePaymentController for common payment functionality and Vietnamese business requirements.
 *
 * Security enhancements:
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
public class VNPayController extends BasePaymentController<VNPayGatewayService> {

    private static final long MAX_VNPAY_AMOUNT = 100_000_000L; // 100 million VND
    private final VNPayService vnPayService;
    private final HoaDonService hoaDonService;

    public VNPayController(VNPayGatewayService vnPayGatewayService, VNPayService vnPayService, HoaDonService hoaDonService) {
        super(vnPayGatewayService);
        this.vnPayService = vnPayService;
        this.hoaDonService = hoaDonService;
    }

    @Override
    protected String getGatewayName() {
        return "VNPay";
    }

    @Override
    protected long getMaxPaymentAmount() {
        return MAX_VNPAY_AMOUNT;
    }

    /**
     * Create VNPay payment order with enhanced validation and error handling.
     *
     * @param orderTotal Payment amount in VND (số tiền thanh toán)
     * @param orderInfo Order information (thông tin đơn hàng)
     * @param request HTTP request for IP detection
     * @return Payment URL response
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam("amount") int orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {

        try {
            String clientIp = getClientIpAddress(request);
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

            // Validate payment parameters using base controller
            validatePaymentParameters(orderTotal, orderInfo, "VNPay-" + System.currentTimeMillis(), baseUrl);

            // Create audit log for payment creation
            Map<String, Object> auditInfo = new HashMap<>();
            auditInfo.put("userAgent", request.getHeader("User-Agent"));
            auditInfo.put("referer", request.getHeader("Referer"));
            createAuditLog("CREATE_PAYMENT", "VNPay-" + System.currentTimeMillis(), (long) orderTotal, clientIp, auditInfo);

            String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("paymentUrl", vnpayUrl);
            responseData.put("paymentMethod", "VNPAY");

            return createSuccessResponse(responseData, "Tạo liên kết thanh toán VNPay thành công");

        } catch (IllegalArgumentException e) {
            log.warn("Invalid VNPay payment parameters: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), "INVALID_PARAMETERS",
                org.springframework.http.HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error creating VNPay payment order: {}", e.getMessage(), e);
            return createErrorResponse("Không thể tạo liên kết thanh toán VNPay", "PAYMENT_CREATION_FAILED",
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Enhanced payment return handler with improved security and error handling.
     *
     * @param request HTTP request containing VNPay response
     * @param response HTTP response for redirect
     * @throws IOException if redirect fails
     */
    @GetMapping("/vnpay-payment")
    public void handlePaymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = VNPayConfig.getIpAddress(request);
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

        log.info("VNPay payment return from IP: {} - TxnRef: {}, TransactionNo: {}",
                clientIp, vnp_TxnRef, vnp_TransactionNo);

        try {
            int paymentStatus = vnPayService.orderReturn(request);

            String status;
            if (paymentStatus == 1) {
                status = "success";
                log.info("VNPay payment return successful - TxnRef: {} from IP: {}", vnp_TxnRef, clientIp);
            } else if (paymentStatus == 0) {
                status = "failed";
                log.warn("VNPay payment return failed - TxnRef: {} from IP: {}", vnp_TxnRef, clientIp);
            } else {
                status = "invalid";
                log.error("VNPay payment return invalid signature - TxnRef: {} from IP: {}", vnp_TxnRef, clientIp);
            }

            // Enhanced URL construction with proper encoding and validation
            StringBuilder redirectUrl = new StringBuilder("http://localhost:5173/orders/create?");
            redirectUrl.append("status=").append(status);

            if (vnp_TransactionNo != null) {
                redirectUrl.append("&transactionId=").append(URLEncoder.encode(vnp_TransactionNo, StandardCharsets.UTF_8));
            }

            if (vnp_TxnRef != null) {
                redirectUrl.append("&vnp_TxnRef=").append(URLEncoder.encode(vnp_TxnRef, StandardCharsets.UTF_8));
            }

            String orderInfo = request.getParameter("vnp_OrderInfo");
            if (orderInfo != null) {
                redirectUrl.append("&orderInfo=").append(URLEncoder.encode(orderInfo, StandardCharsets.UTF_8));
            }

            String paymentTime = request.getParameter("vnp_PayDate");
            if (paymentTime != null) {
                redirectUrl.append("&paymentTime=").append(URLEncoder.encode(paymentTime, StandardCharsets.UTF_8));
            }

            String amount = request.getParameter("vnp_Amount");
            if (amount != null) {
                redirectUrl.append("&totalPrice=").append(URLEncoder.encode(amount, StandardCharsets.UTF_8));
            }

            log.info("Redirecting VNPay payment return to frontend - TxnRef: {} from IP: {}", vnp_TxnRef, clientIp);
            response.sendRedirect(redirectUrl.toString());

        } catch (Exception e) {
            log.error("Error handling VNPay payment return - TxnRef: {} from IP: {} - {}",
                     vnp_TxnRef, clientIp, e.getMessage(), e);

            // Redirect to error page
            String errorUrl = "http://localhost:5173/orders/create?status=error&message=" +
                            URLEncoder.encode("Payment processing error", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Enhanced IPN (Instant Payment Notification) endpoint for server-to-server VNPay notifications.
     * This endpoint automatically updates order status when payments are completed with enhanced security.
     */
    @PostMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String vnp_Amount = request.getParameter("vnp_Amount");

        // Validate callback request using base controller
        if (!validateCallbackRequest(request)) {
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "97");
            response.put("Message", "Invalid IPN request");
            return ResponseEntity.ok(response);
        }

        // Create audit log for IPN processing
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put("vnp_TransactionNo", vnp_TransactionNo);
        auditInfo.put("vnp_Amount", vnp_Amount);
        auditInfo.put("ipnType", "INSTANT_NOTIFICATION");
        createAuditLog("PROCESS_IPN", vnp_TxnRef, vnp_Amount != null ? Long.parseLong(vnp_Amount) / 100 : null, clientIp, auditInfo);

        log.info("VNPay IPN received from IP: {} - TxnRef: {}, TransactionNo: {}",
                clientIp, vnp_TxnRef, vnp_TransactionNo);

        Map<String, String> response = new HashMap<>();

        try {
            // Use enhanced IPN processing
            VNPayService.PaymentIPNResult ipnResult = vnPayService.processIPN(request);

            if (!ipnResult.isValid()) {
                log.error("VNPay IPN validation failed from IP: {} - TxnRef: {} - Error: {}",
                         clientIp, vnp_TxnRef, ipnResult.getErrorMessage());
                response.put("RspCode", "97");
                response.put("Message", "Invalid Request");
                return ResponseEntity.ok(response);
            }

            if (ipnResult.isSuccessful()) {
                // Payment successful - update order status
                try {
                    Long orderId = Long.parseLong(ipnResult.getTransactionRef());
                    hoaDonService.confirmPayment(orderId, PhuongThucThanhToan.VNPAY);

                    log.info("Order {} payment confirmed successfully via VNPay IPN from IP: {}",
                            orderId, clientIp);
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");

                } catch (NumberFormatException e) {
                    log.error("Invalid order ID in VNPay IPN from IP: {} - TxnRef: {}",
                             clientIp, ipnResult.getTransactionRef());
                    response.put("RspCode", "01");
                    response.put("Message", "Invalid Order ID");

                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {} from IP: {} - {}",
                             ipnResult.getTransactionRef(), clientIp, e.getMessage(), e);
                    response.put("RspCode", "02");
                    response.put("Message", "Order Confirm Failed");
                }
            } else {
                // Payment failed
                log.warn("VNPay payment failed from IP: {} - TxnRef: {}, Status: {}",
                        clientIp, ipnResult.getTransactionRef(), ipnResult.getStatus());
                response.put("RspCode", "00");
                response.put("Message", "Payment Failed");
            }

        } catch (Exception e) {
            log.error("Error processing VNPay IPN from IP: {} - TxnRef: {} - {}",
                     clientIp, vnp_TxnRef, e.getMessage(), e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown Error");
        }

        return ResponseEntity.ok(response);
    }
}
