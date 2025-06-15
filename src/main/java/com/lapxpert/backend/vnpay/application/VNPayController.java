package com.lapxpert.backend.vnpay.application;

import com.lapxpert.backend.vnpay.domain.VNPayService;
import com.lapxpert.backend.vnpay.domain.VNPayConfig;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced VNPay controller with improved security, error handling, and audit logging.
 *
 * Security enhancements:
 * - Enhanced parameter validation
 * - Improved error handling with security-conscious responses
 * - Better IP address detection and logging
 * - Enhanced IPN processing with comprehensive validation
 * - Proper audit logging for all payment operations
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private HoaDonService hoaDonService;

    public VNPayController(VNPayService vnPayService, HoaDonService hoaDonService) {
        this.vnPayService = vnPayService;
        this.hoaDonService = hoaDonService;
    }

    /**
     * Create VNPay payment order with enhanced validation and error handling.
     *
     * @param orderTotal Payment amount in VND
     * @param orderInfo Order information
     * @param request HTTP request for IP detection
     * @return Payment URL response
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestParam("amount") int orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {

        String clientIp = VNPayConfig.getIpAddress(request);
        log.info("VNPay create order request - Amount: {}, ClientIP: {}", orderTotal, clientIp);

        Map<String, String> response = new HashMap<>();

        try {
            // Enhanced parameter validation
            if (orderTotal <= 0) {
                log.warn("Invalid payment amount: {} from IP: {}", orderTotal, clientIp);
                response.put("error", "Invalid payment amount");
                return ResponseEntity.badRequest().body(response);
            }

            if (orderInfo == null || orderInfo.trim().isEmpty()) {
                log.warn("Empty order info from IP: {}", clientIp);
                response.put("error", "Order information is required");
                return ResponseEntity.badRequest().body(response);
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl);

            response.put("paymentUrl", vnpayUrl);
            response.put("status", "success");

            log.info("VNPay payment URL created successfully for amount: {} from IP: {}", orderTotal, clientIp);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("VNPay create order validation error: {} from IP: {}", e.getMessage(), clientIp);
            response.put("error", "Invalid request parameters");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error creating VNPay payment order from IP: {} - {}", clientIp, e.getMessage(), e);
            response.put("error", "Unable to create payment order");
            return ResponseEntity.status(500).body(response);
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
        String clientIp = VNPayConfig.getIpAddress(request);
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

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
