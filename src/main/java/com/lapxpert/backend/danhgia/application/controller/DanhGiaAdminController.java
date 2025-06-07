package com.lapxpert.backend.danhgia.application.controller;

import com.lapxpert.backend.danhgia.application.dto.DanhGiaDto;
import com.lapxpert.backend.danhgia.domain.service.DanhGiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for DanhGia (Review) module moderation
 * Provides endpoints for review approval, rejection, and management
 * Follows established admin controller patterns with Vietnamese naming conventions
 */
@RestController
@RequestMapping("/api/v1/admin/danh-gia") // Admin API (v1)
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DanhGiaAdminController {

    private final DanhGiaService danhGiaService;

    // ==================== MODERATION OPERATIONS ====================

    /**
     * Get pending reviews for moderation
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @return page of pending reviews
     */
    @GetMapping("/cho-duyet")
    public ResponseEntity<Page<DanhGiaDto>> layDanhGiaChoDuyet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting pending reviews for moderation (page: {}, size: {})", page, size);

        try {
            Page<DanhGiaDto> result = danhGiaService.layDanhGiaChoDuyet(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting pending reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Approve a review
     * @param id review ID to approve
     * @param ghiChu optional moderator note
     * @return approved review DTO
     */
    @PostMapping("/{id}/duyet")
    public ResponseEntity<DanhGiaDto> duyetDanhGia(@PathVariable Long id,
                                                  @RequestParam(required = false) String ghiChu) {
        log.info("Admin approving review {} with note: {}", id, ghiChu);

        try {
            DanhGiaDto result = danhGiaService.duyetDanhGia(id, ghiChu);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Review approval failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error approving review: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject a review
     * @param id review ID to reject
     * @param lyDo rejection reason (required)
     * @return rejected review DTO
     */
    @PostMapping("/{id}/tu-choi")
    public ResponseEntity<DanhGiaDto> tuChoiDanhGia(@PathVariable Long id,
                                                   @RequestParam String lyDo) {
        log.info("Admin rejecting review {} with reason: {}", id, lyDo);

        try {
            DanhGiaDto result = danhGiaService.tuChoiDanhGia(id, lyDo);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Review rejection failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error rejecting review: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Hide a review (admin action for inappropriate content)
     * @param id review ID to hide
     * @param lyDo hiding reason (required)
     * @return hidden review DTO
     */
    @PostMapping("/{id}/an")
    public ResponseEntity<DanhGiaDto> anDanhGia(@PathVariable Long id,
                                               @RequestParam String lyDo) {
        log.info("Admin hiding review {} with reason: {}", id, lyDo);

        try {
            DanhGiaDto result = danhGiaService.anDanhGia(id, lyDo);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Review hiding failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error hiding review: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== ADMIN DASHBOARD AND STATISTICS ====================

    /**
     * Get review statistics for admin dashboard
     * @return review statistics summary
     */
    @GetMapping("/thong-ke")
    public ResponseEntity<DanhGiaService.ReviewStatistics> layThongKeDanhGia() {
        log.debug("Getting review statistics for admin dashboard");

        try {
            DanhGiaService.ReviewStatistics result = danhGiaService.layThongKeDanhGia();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting review statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific review by ID for admin view
     * @param id review ID
     * @return review DTO with admin details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DanhGiaDto> layDanhGiaTheoId(@PathVariable Long id) {
        log.debug("Admin getting review by ID: {}", id);

        try {
            DanhGiaDto result = danhGiaService.layDanhGiaTheoId(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting review by ID for admin: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Batch approve multiple reviews
     * @param reviewIds list of review IDs to approve
     * @param ghiChu optional batch moderator note
     * @return operation result
     */
    @PostMapping("/duyet-hang-loat")
    public ResponseEntity<BatchOperationResult> duyetHangLoat(
            @RequestBody BatchModerationRequest request) {
        log.info("Admin batch approving {} reviews", request.getReviewIds().size());

        try {
            int successCount = 0;
            int failureCount = 0;

            for (Long reviewId : request.getReviewIds()) {
                try {
                    danhGiaService.duyetDanhGia(reviewId, request.getGhiChu());
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to approve review {} in batch operation", reviewId, e);
                    failureCount++;
                }
            }

            BatchOperationResult result = new BatchOperationResult(
                "BATCH_APPROVE", successCount, failureCount, 
                String.format("Đã duyệt %d đánh giá, %d thất bại", successCount, failureCount)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in batch approve operation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Batch reject multiple reviews
     * @param request batch rejection request
     * @return operation result
     */
    @PostMapping("/tu-choi-hang-loat")
    public ResponseEntity<BatchOperationResult> tuChoiHangLoat(
            @RequestBody BatchModerationRequest request) {
        log.info("Admin batch rejecting {} reviews", request.getReviewIds().size());

        if (request.getLyDo() == null || request.getLyDo().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            int successCount = 0;
            int failureCount = 0;

            for (Long reviewId : request.getReviewIds()) {
                try {
                    danhGiaService.tuChoiDanhGia(reviewId, request.getLyDo());
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to reject review {} in batch operation", reviewId, e);
                    failureCount++;
                }
            }

            BatchOperationResult result = new BatchOperationResult(
                "BATCH_REJECT", successCount, failureCount,
                String.format("Đã từ chối %d đánh giá, %d thất bại", successCount, failureCount)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error in batch reject operation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== REQUEST/RESPONSE CLASSES ====================

    /**
     * Request class for batch moderation operations
     */
    public static class BatchModerationRequest {
        private java.util.List<Long> reviewIds;
        private String ghiChu;  // For approvals
        private String lyDo;    // For rejections

        // Constructors
        public BatchModerationRequest() {}

        public BatchModerationRequest(java.util.List<Long> reviewIds, String ghiChu, String lyDo) {
            this.reviewIds = reviewIds;
            this.ghiChu = ghiChu;
            this.lyDo = lyDo;
        }

        // Getters and setters
        public java.util.List<Long> getReviewIds() { return reviewIds; }
        public void setReviewIds(java.util.List<Long> reviewIds) { this.reviewIds = reviewIds; }
        public String getGhiChu() { return ghiChu; }
        public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
        public String getLyDo() { return lyDo; }
        public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    }

    /**
     * Result class for batch operations
     */
    public static class BatchOperationResult {
        private final String operation;
        private final int successCount;
        private final int failureCount;
        private final String message;

        public BatchOperationResult(String operation, int successCount, int failureCount, String message) {
            this.operation = operation;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.message = message;
        }

        public String getOperation() { return operation; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public String getMessage() { return message; }
        public int getTotalCount() { return successCount + failureCount; }
        public boolean isFullySuccessful() { return failureCount == 0; }
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Handle validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        log.warn("Admin validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    /**
     * Handle entity not found errors
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
        log.warn("Admin entity not found: {}", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    /**
     * Error response class
     */
    public static class ErrorResponse {
        private final String code;
        private final String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
