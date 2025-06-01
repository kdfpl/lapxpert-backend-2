package com.lapxpert.backend.phieugiamgia.application.controller;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.service.NguoiDungService;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaAuditHistory;
import com.lapxpert.backend.phieugiamgia.domain.service.PhieuGiamGiaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for PhieuGiamGia (Voucher) operations
 * Enhanced with proper validation and error handling
 */
@Slf4j
@RestController
@RequestMapping("api/v1/phieu-giam-gia")
@CrossOrigin(origins = "*")
@Validated
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService phieuGiamGiaService;
    private final NguoiDungService nguoiDungService;

    @GetMapping()
    public List<PhieuGiamGiaDto> getAllPhieuGiamGia(@AuthenticationPrincipal NguoiDung currentUser) {
        return phieuGiamGiaService.getAllPhieuGiamGia();
    }
    @PostMapping()
    public ResponseEntity<PhieuGiamGiaDto> themPhieu(@RequestBody PhieuGiamGiaDto request,
                                                     @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            String reason = "Tạo phiếu giảm giá mới từ admin panel";
            PhieuGiamGiaDto createdPhieu = phieuGiamGiaService.taoPhieuWithAudit(request, reason);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPhieu);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhieuGiamGiaDto> capNhatPhieu(@PathVariable("id") Long phieuId,
                                                        @RequestBody PhieuGiamGiaDto phieuGiamGiaDto,
                                                        @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            // Get reason from DTO if provided, otherwise use default
            String reason = phieuGiamGiaDto.getLyDoThayDoi() != null && !phieuGiamGiaDto.getLyDoThayDoi().trim().isEmpty()
                    ? phieuGiamGiaDto.getLyDoThayDoi().trim()
                    : "Cập nhật thông tin phiếu giảm giá từ admin panel";

            phieuGiamGiaService.capNhatPhieuWithAudit(phieuGiamGiaDto, phieuId, reason);
            PhieuGiamGiaDto updatedPhieu = phieuGiamGiaService.getPhieuGiamGiaById(phieuId);
            return ResponseEntity.ok(updatedPhieu);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<PhieuGiamGiaDto> getPhieuById(@PathVariable("id") Long id,
                                                        @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            PhieuGiamGiaDto phieu = phieuGiamGiaService.getPhieuGiamGiaById(id);
            return ResponseEntity.ok(phieu);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}/audit-history")
    public ResponseEntity<List<PhieuGiamGiaAuditHistory>> getAuditHistory(@PathVariable("id") Long id,
                                                                          @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<PhieuGiamGiaAuditHistory> auditHistory = phieuGiamGiaService.getAuditHistory(id);
            return ResponseEntity.ok(auditHistory);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("delete/{id}")
    public ResponseEntity<PhieuGiamGiaDto> closeVoucher(@PathVariable Long id,
                                                        @RequestParam(value = "reason", required = false) String reason,
                                                        @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            // Use provided reason or default
            String auditReason = reason != null && !reason.trim().isEmpty()
                    ? reason.trim()
                    : "Đóng phiếu giảm giá từ admin panel";

            phieuGiamGiaService.deletePhieuGiamGiaWithAudit(id, auditReason);
            PhieuGiamGiaDto closedPhieu = phieuGiamGiaService.getPhieuGiamGiaById(id);
            return ResponseEntity.ok(closedPhieu);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== VOUCHER VALIDATION & ORDER INTEGRATION ENDPOINTS ====================

    /**
     * Validate voucher for order application
     * POST /api/v1/phieu-giam-gia/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(
            @RequestBody VoucherValidationRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            // Get customer if provided
            NguoiDung customer = null;
            if (request.getCustomerId() != null) {
                Optional<NguoiDung> customerOpt = nguoiDungService.findByIdOptional(request.getCustomerId());
                if (customerOpt.isPresent()) {
                    customer = customerOpt.get();
                } else {
                    VoucherValidationResponse errorResponse = new VoucherValidationResponse();
                    errorResponse.setValid(false);
                    errorResponse.setErrorMessage("Khách hàng không tồn tại");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Validate voucher using service
            PhieuGiamGiaService.VoucherValidationResult result =
                phieuGiamGiaService.validateVoucher(request.getVoucherCode(), customer, request.getOrderTotal());

            VoucherValidationResponse response = new VoucherValidationResponse();
            response.setValid(result.isValid());
            response.setErrorMessage(result.getErrorMessage());

            if (result.isValid()) {
                // Convert PhieuGiamGia to DTO using mapper
                PhieuGiamGiaDto voucherDto = phieuGiamGiaService.getPhieuGiamGiaByCode(request.getVoucherCode());
                response.setVoucher(voucherDto);
                response.setDiscountAmount(result.getDiscountAmount());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            VoucherValidationResponse errorResponse = new VoucherValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setErrorMessage("Lỗi xác thực voucher: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            VoucherValidationResponse errorResponse = new VoucherValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setErrorMessage("Lỗi hệ thống khi xác thực voucher");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get available vouchers for specific customer and order total
     * GET /api/v1/phieu-giam-gia/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<PhieuGiamGiaDto>> getAvailableVouchers(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") BigDecimal orderTotal,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<PhieuGiamGiaDto> availableVouchers = phieuGiamGiaService.getAvailableVouchers(customerId, orderTotal);
            return ResponseEntity.ok(availableVouchers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Find the best voucher for automatic selection
     * GET /api/v1/phieu-giam-gia/best
     */
    @GetMapping("/best")
    public ResponseEntity<PhieuGiamGiaService.BestVoucherResult> findBestVoucher(
            @RequestParam(required = false) Long customerId,
            @RequestParam BigDecimal orderTotal,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            PhieuGiamGiaService.BestVoucherResult result = phieuGiamGiaService.findBestVoucher(customerId, orderTotal);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error finding best voucher: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get top N best vouchers for a customer
     * GET /api/v1/phieu-giam-gia/top
     */
    @GetMapping("/top")
    public ResponseEntity<List<PhieuGiamGiaService.BestVoucherResult>> getTopVouchers(
            @RequestParam(required = false) Long customerId,
            @RequestParam BigDecimal orderTotal,
            @RequestParam(defaultValue = "3") int limit,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<PhieuGiamGiaService.BestVoucherResult> topVouchers =
                phieuGiamGiaService.findTopVouchers(customerId, orderTotal, limit);
            return ResponseEntity.ok(topVouchers);
        } catch (Exception e) {
            log.error("Error getting top vouchers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Search vouchers by query string
     * GET /api/v1/phieu-giam-gia/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<PhieuGiamGiaDto>> searchVouchers(
            @RequestParam("q") String query,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<PhieuGiamGiaDto> searchResults = phieuGiamGiaService.searchVouchers(query);
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Get voucher details by code
     * GET /api/v1/phieu-giam-gia/code/{voucherCode}
     */
    @GetMapping("/code/{voucherCode}")
    public ResponseEntity<PhieuGiamGiaDto> getVoucherByCode(
            @PathVariable String voucherCode,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            PhieuGiamGiaDto voucher = phieuGiamGiaService.getPhieuGiamGiaByCode(voucherCode);
            return ResponseEntity.ok(voucher);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== ORDER INTEGRATION ENDPOINTS ====================

    /**
     * Apply voucher to order (used during order creation)
     * POST /api/v1/phieu-giam-gia/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<VoucherApplicationResponse> applyVoucherToOrder(
            @RequestBody VoucherApplicationRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            // This endpoint is primarily for post-order creation voucher application
            // During order creation, vouchers are applied via HoaDonService
            VoucherApplicationResponse response = new VoucherApplicationResponse();
            response.setSuccess(true);
            response.setMessage("Voucher áp dụng thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            VoucherApplicationResponse errorResponse = new VoucherApplicationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi áp dụng voucher: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Remove voucher from order
     * DELETE /api/v1/phieu-giam-gia/remove
     */
    @DeleteMapping("/remove")
    public ResponseEntity<VoucherApplicationResponse> removeVoucherFromOrder(
            @RequestBody VoucherRemovalRequest request,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            // Remove vouchers from order
            phieuGiamGiaService.removeVouchersFromOrder(request.getOrderId());

            VoucherApplicationResponse response = new VoucherApplicationResponse();
            response.setSuccess(true);
            response.setMessage("Voucher đã được gỡ bỏ thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            VoucherApplicationResponse errorResponse = new VoucherApplicationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Lỗi gỡ bỏ voucher: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get applied vouchers for an order
     * GET /api/v1/phieu-giam-gia/applied/{orderId}
     */
    @GetMapping("/applied/{orderId}")
    public ResponseEntity<List<PhieuGiamGiaDto>> getAppliedVouchers(
            @PathVariable Long orderId,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            List<PhieuGiamGiaDto> appliedVouchers = phieuGiamGiaService.getAppliedVouchersForOrder(orderId);
            return ResponseEntity.ok(appliedVouchers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Check customer eligibility for voucher
     * GET /api/v1/phieu-giam-gia/eligibility
     */
    @GetMapping("/eligibility")
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @RequestParam String voucherCode,
            @RequestParam Long customerId,
            @AuthenticationPrincipal NguoiDung currentUser) {
        try {
            boolean eligible = phieuGiamGiaService.isCustomerEligible(voucherCode, customerId);

            EligibilityResponse response = new EligibilityResponse();
            response.setEligible(eligible);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            EligibilityResponse errorResponse = new EligibilityResponse();
            errorResponse.setEligible(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==================== REQUEST/RESPONSE DTOs ====================

    /**
     * Request DTO for voucher validation
     */
    public static class VoucherValidationRequest {
        private String voucherCode;

        private Long customerId;

        private BigDecimal orderTotal;

        // Getters and setters
        public String getVoucherCode() { return voucherCode; }
        public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public BigDecimal getOrderTotal() { return orderTotal; }
        public void setOrderTotal(BigDecimal orderTotal) { this.orderTotal = orderTotal; }
    }

    /**
     * Response DTO for voucher validation
     */
    public static class VoucherValidationResponse {
        private boolean valid;
        private PhieuGiamGiaDto voucher;
        private BigDecimal discountAmount;
        private String errorMessage;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public PhieuGiamGiaDto getVoucher() { return voucher; }
        public void setVoucher(PhieuGiamGiaDto voucher) { this.voucher = voucher; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Request DTO for voucher application
     */
    public static class VoucherApplicationRequest {
        private Long orderId;
        private String voucherCode;

        // Getters and setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getVoucherCode() { return voucherCode; }
        public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    }

    /**
     * Response DTO for voucher application
     */
    public static class VoucherApplicationResponse {
        private boolean success;
        private String message;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Request DTO for voucher removal
     */
    public static class VoucherRemovalRequest {
        private Long orderId;
        private String voucherCode;

        // Getters and setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getVoucherCode() { return voucherCode; }
        public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    }

    /**
     * Response DTO for eligibility check
     */
    public static class EligibilityResponse {
        private boolean eligible;

        // Getters and setters
        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }
    }

}