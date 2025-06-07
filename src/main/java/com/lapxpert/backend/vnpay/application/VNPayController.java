package com.lapxpert.backend.vnpay.application;

import com.lapxpert.backend.vnpay.domain.VNPayService;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestParam("amount") int orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpayUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-payment")
    public void handlePaymentReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int paymentStatus = vnPayService.orderReturn(request);

        // Tạo URL redirect về front-end
        String redirectUrl = "http://localhost:5173/orders/create?" +
                "status=" + (paymentStatus == 1 ? "success" : "failed") +
                "&transactionId=" + request.getParameter("vnp_TransactionNo") +
                "&vnp_TxnRef=" + request.getParameter("vnp_TxnRef") +
                "&orderInfo=" + URLEncoder.encode(request.getParameter("vnp_OrderInfo"), StandardCharsets.UTF_8) +
                "&paymentTime=" + request.getParameter("vnp_PayDate") +
                "&totalPrice=" + request.getParameter("vnp_Amount");

        response.sendRedirect(redirectUrl);
    }

    /**
     * IPN (Instant Payment Notification) endpoint for server-to-server VNPay notifications.
     * This endpoint automatically updates order status when payments are completed.
     */
    @PostMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            // Validate the IPN request
            int paymentStatus = vnPayService.orderReturn(request);
            String vnp_TxnRef = request.getParameter("vnp_TxnRef");
            String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
            String vnp_Amount = request.getParameter("vnp_Amount");
            String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

            log.info("VNPay IPN received - TxnRef: {}, Status: {}, Amount: {}, TransactionNo: {}",
                    vnp_TxnRef, vnp_TransactionStatus, vnp_Amount, vnp_TransactionNo);

            if (paymentStatus == 1) {
                // Payment successful - update order status
                try {
                    Long orderId = Long.parseLong(vnp_TxnRef);
                    hoaDonService.confirmPayment(orderId, PhuongThucThanhToan.VNPAY);

                    log.info("Order {} payment confirmed successfully via VNPay IPN", orderId);
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");
                } catch (NumberFormatException e) {
                    log.error("Invalid order ID in vnp_TxnRef: {}", vnp_TxnRef);
                    response.put("RspCode", "01");
                    response.put("Message", "Invalid Order ID");
                } catch (Exception e) {
                    log.error("Failed to confirm payment for order {}: {}", vnp_TxnRef, e.getMessage());
                    response.put("RspCode", "02");
                    response.put("Message", "Order Confirm Failed");
                }
            } else if (paymentStatus == 0) {
                // Payment failed
                log.warn("VNPay payment failed for TxnRef: {}", vnp_TxnRef);
                response.put("RspCode", "00");
                response.put("Message", "Payment Failed");
            } else {
                // Invalid signature
                log.error("VNPay IPN signature validation failed for TxnRef: {}", vnp_TxnRef);
                response.put("RspCode", "97");
                response.put("Message", "Invalid Signature");
            }

        } catch (Exception e) {
            log.error("Error processing VNPay IPN: {}", e.getMessage());
            response.put("RspCode", "99");
            response.put("Message", "Unknown Error");
        }

        return ResponseEntity.ok(response);
    }
}
