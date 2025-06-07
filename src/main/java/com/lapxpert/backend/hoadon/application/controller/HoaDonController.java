package com.lapxpert.backend.hoadon.application.controller;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.hoadon.domain.service.ReceiptPreviewService;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.vnpay.domain.VNPayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/hoa-don")
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final ReceiptPreviewService receiptPreviewService;
    private final VNPayService vnPayService;

    public HoaDonController(HoaDonService hoaDonService, ReceiptPreviewService receiptPreviewService, VNPayService vnPayService) {
        this.hoaDonService = hoaDonService;
        this.receiptPreviewService = receiptPreviewService;
        this.vnPayService = vnPayService;
    }

    // Lấy tất cả hóa đơn hoặc lọc theo trạng thái giao hàng
    // This effectively serves as getAllHoaDons for admin if trangThai is null/empty
    @GetMapping
    public ResponseEntity<List<HoaDonDto>> getAllHoaDon(@RequestParam(value = "trangThai", required = false) String trangThai) {
        List<HoaDonDto> hoaDonDtos = hoaDonService.getHoaDonsByTrangThai(trangThai);
        return ResponseEntity.ok(hoaDonDtos);
    }

    // Thêm mới hóa đơn - Path changed from /add to / to match frontend, NguoiDung added
    @PostMapping
    public ResponseEntity<HoaDonDto> createHoaDon(@RequestBody HoaDonDto hoaDonDto, @AuthenticationPrincipal NguoiDung currentUser) {
        HoaDonDto createdHoaDonDto = hoaDonService.createHoaDon(hoaDonDto, currentUser);
        return new ResponseEntity<>(createdHoaDonDto, HttpStatus.CREATED);
    }

    // Lấy hóa đơn theo ID với kiểm tra bảo mật
    @GetMapping("/{id}")
    public ResponseEntity<HoaDonDto> getHoaDonById(@PathVariable Long id, @AuthenticationPrincipal NguoiDung currentUser) {
        HoaDonDto hoaDonDto = hoaDonService.getHoaDonByIdSecure(id, currentUser);
        return ResponseEntity.ok(hoaDonDto);
    }

    // Cập nhật hóa đơn với kiểm tra bảo mật
    @PutMapping("/{id}")
    public ResponseEntity<HoaDonDto> updateHoaDon(@PathVariable Long id, @RequestBody HoaDonDto hoaDonDto, @AuthenticationPrincipal NguoiDung currentUser) {
        // Security check is handled in the service method
        HoaDonDto updatedHoaDonDto = hoaDonService.updateHoaDon(id, hoaDonDto, currentUser);
        return ResponseEntity.ok(updatedHoaDonDto);
    }

    // Endpoint to get orders for the authenticated user - NEW
    @GetMapping("/me")
    public ResponseEntity<List<HoaDonDto>> getMyOrders(@AuthenticationPrincipal NguoiDung currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<HoaDonDto> orders = hoaDonService.findByNguoiDungEmail(currentUser.getEmail());
        return ResponseEntity.ok(orders);
    }

    // Note: Timeline functionality is now handled by HoaDonAuditHistory
    // Use HoaDonAuditHistoryDto.TimelineEntry for frontend timeline display

    // Endpoint để xác nhận thanh toán với kiểm tra bảo mật
    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<HoaDonDto> confirmPayment(
            @PathVariable Long orderId,
            @RequestParam PhuongThucThanhToan phuongThucThanhToan,
            @AuthenticationPrincipal NguoiDung currentUser) {
        HoaDonDto confirmedOrder = hoaDonService.confirmPaymentSecure(orderId, phuongThucThanhToan, currentUser);
        return ResponseEntity.ok(confirmedOrder);
    }

    // Endpoint để hủy đơn hàng với kiểm tra bảo mật
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<HoaDonDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal NguoiDung currentUser) {
        HoaDonDto cancelledOrder = hoaDonService.cancelOrderSecure(orderId, reason != null ? reason : "Cancelled by user", currentUser);
        return ResponseEntity.ok(cancelledOrder);
    }

    // Endpoint để cập nhật trạng thái thanh toán với kiểm tra bảo mật
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<HoaDonDto> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam TrangThaiThanhToan trangThaiThanhToan,
            @RequestParam(required = false) String ghiChu,
            @AuthenticationPrincipal NguoiDung currentUser) {
        HoaDonDto updatedOrder = hoaDonService.updatePaymentStatusSecure(orderId, trangThaiThanhToan, ghiChu, currentUser);
        return ResponseEntity.ok(updatedOrder);
    }

    // Endpoint để lấy dữ liệu preview hóa đơn
    @GetMapping("/{orderId}/receipt-preview")
    public ResponseEntity<ReceiptPreviewService.ReceiptPreviewData> getReceiptPreview(
            @PathVariable Long orderId,
            @AuthenticationPrincipal NguoiDung currentUser) {
        // Security check through service layer
        hoaDonService.getHoaDonByIdSecure(orderId, currentUser); // This will throw if user doesn't have access

        ReceiptPreviewService.ReceiptPreviewData preview = receiptPreviewService.generateReceiptPreview(orderId);
        return ResponseEntity.ok(preview);
    }

    // Endpoint để xử lý thanh toán VNPay cho đơn hàng cụ thể
    @PostMapping("/{orderId}/vnpay-payment")
    public ResponseEntity<Map<String, String>> processVNPayPayment(
            @PathVariable Long orderId,
            @RequestBody VNPayPaymentRequest vnpayRequest,
            @AuthenticationPrincipal NguoiDung currentUser,
            HttpServletRequest request) {

        // Security check: user can only process payment for their own orders
        HoaDonDto order = hoaDonService.getHoaDonByIdSecure(orderId, currentUser);

        // Validate order can be paid
        if (order.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new IllegalStateException("Đơn hàng đã được thanh toán");
        }

        // Create VNPay payment URL with order correlation
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String clientIp = getClientIpAddress(request);
        String vnpayUrl = hoaDonService.createVNPayPayment(orderId, vnpayRequest.getAmount(),
                                                          vnpayRequest.getOrderInfo(), baseUrl, clientIp);

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpayUrl);
        response.put("orderId", orderId.toString());
        return ResponseEntity.ok(response);
    }

    // DTO class for VNPay payment request
    public static class VNPayPaymentRequest {
        private int amount;
        private String orderInfo;
        private String returnUrl;

        // Getters and setters
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }

        public String getOrderInfo() { return orderInfo; }
        public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }

        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    }

    // Endpoint để lấy HTML preview hóa đơn
    @GetMapping("/{orderId}/receipt-preview-html")
    public ResponseEntity<String> getReceiptPreviewHtml(
            @PathVariable Long orderId,
            @AuthenticationPrincipal NguoiDung currentUser) {
        // Security check through service layer
        hoaDonService.getHoaDonByIdSecure(orderId, currentUser); // This will throw if user doesn't have access

        String htmlPreview = receiptPreviewService.generateReceiptPreviewHtml(orderId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(htmlPreview);
    }

    // Endpoint để tạo và tải PDF hóa đơn
    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<byte[]> generateReceiptPdf(
            @PathVariable Long orderId,
            @AuthenticationPrincipal NguoiDung currentUser) {
        // Security check through service layer
        hoaDonService.getHoaDonByIdSecure(orderId, currentUser); // This will throw if user doesn't have access

        try {
            byte[] pdfBytes = receiptPreviewService.generateReceiptPdf(orderId);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=hoa-don-" + orderId + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF receipt: " + e.getMessage(), e);
        }
    }

    // Endpoint để lấy lịch sử audit của đơn hàng
    @GetMapping("/{id}/audit-history")
    public ResponseEntity<List<HoaDonAuditHistory>> getOrderAuditHistory(@PathVariable Long id, @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<HoaDonAuditHistory> auditHistory = hoaDonService.getAuditHistory(id);
            return ResponseEntity.ok(auditHistory);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get client IP address from request, handling proxy headers
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

        // Handle multiple IPs in X-Forwarded-For header
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress != null ? ipAddress : "127.0.0.1";
    }
}