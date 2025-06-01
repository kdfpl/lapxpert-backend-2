package com.lapxpert.backend.hoadon.application.controller;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiThanhToan;
import com.lapxpert.backend.hoadon.domain.service.HoaDonService;
import com.lapxpert.backend.hoadon.domain.service.ReceiptPreviewService;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/hoa-don")
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final ReceiptPreviewService receiptPreviewService;

    public HoaDonController(HoaDonService hoaDonService, ReceiptPreviewService receiptPreviewService) {
        this.hoaDonService = hoaDonService;
        this.receiptPreviewService = receiptPreviewService;
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
}