package com.lapxpert.backend.vnpay.application;

import com.lapxpert.backend.vnpay.domain.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    public VNPayController(VNPayService vnPayService) {
        this.vnPayService = vnPayService;
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
}
