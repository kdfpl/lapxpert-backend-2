package com.lapxpert.backend.danhgia.controller;

import com.lapxpert.backend.danhgia.dto.CreateReviewDto;
import com.lapxpert.backend.danhgia.dto.DanhGiaDto;
import com.lapxpert.backend.danhgia.dto.ProductRatingDto;
import com.lapxpert.backend.danhgia.dto.ReviewFilterDto;
import com.lapxpert.backend.danhgia.dto.UpdateReviewDto;
import com.lapxpert.backend.danhgia.service.DanhGiaService;
import com.lapxpert.backend.danhgia.service.ReviewEligibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing REST controller for DanhGia (Review) module
 * Provides endpoints for review submission, viewing, and management
 * Follows established controller patterns with Vietnamese naming conventions
 */
@RestController
@RequestMapping("/api/v2/danh-gia") // Customer API (v2)
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DanhGiaController {

    private final DanhGiaService danhGiaService;
    private final ReviewEligibilityService eligibilityService;

    // ==================== REVIEW CRUD OPERATIONS ====================

    /**
     * Create new product review
     * @param createDto review creation data
     * @return created review DTO
     */
    @PostMapping
    public ResponseEntity<DanhGiaDto> taoMoiDanhGia(@Valid @RequestBody CreateReviewDto createDto) {
        log.info("Creating new review for product {} by user {}", 
                createDto.getSanPhamId(), createDto.getNguoiDungId());

        try {
            DanhGiaDto result = danhGiaService.taoMoiDanhGia(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Review creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update existing review
     * @param id review ID
     * @param updateDto review update data
     * @return updated review DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<DanhGiaDto> capNhatDanhGia(@PathVariable Long id, 
                                                    @Valid @RequestBody UpdateReviewDto updateDto) {
        log.info("Updating review {} by user {}", id, updateDto.getNguoiDungId());

        try {
            DanhGiaDto result = danhGiaService.capNhatDanhGia(id, updateDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Review update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error updating review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get review by ID
     * @param id review ID
     * @return review DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<DanhGiaDto> layDanhGiaTheoId(@PathVariable Long id) {
        log.debug("Getting review by ID: {}", id);

        try {
            DanhGiaDto result = danhGiaService.layDanhGiaTheoId(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting review by ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== PRODUCT REVIEW QUERIES ====================

    /**
     * Get reviews for a specific product with filtering and pagination
     * @param sanPhamId product ID
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @param rating filter by specific rating (optional)
     * @param minRating minimum rating filter (optional)
     * @param maxRating maximum rating filter (optional)
     * @param sortBy sort field (default: ngayTao)
     * @param sortDirection sort direction (default: DESC)
     * @return page of reviews
     */
    @GetMapping("/san-pham/{sanPhamId}")
    public ResponseEntity<Page<DanhGiaDto>> layDanhGiaTheoSanPham(
            @PathVariable Long sanPhamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting reviews for product {} (page: {}, size: {}, rating: {})", 
                 sanPhamId, page, size, rating);

        try {
            ReviewFilterDto filter = ReviewFilterDto.builder()
                .page(page)
                .size(size)
                .exactRating(rating)
                .minRating(minRating)
                .maxRating(maxRating)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

            Page<DanhGiaDto> result = danhGiaService.layDanhGiaTheoSanPham(sanPhamId, filter);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid filter parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error getting reviews for product: {}", sanPhamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get product rating summary and statistics
     * @param sanPhamId product ID
     * @return product rating summary
     */
    @GetMapping("/san-pham/{sanPhamId}/thong-ke")
    public ResponseEntity<ProductRatingDto> layThongKeDanhGia(@PathVariable Long sanPhamId) {
        log.debug("Getting rating statistics for product: {}", sanPhamId);

        try {
            ProductRatingDto result = danhGiaService.tinhToanDanhGiaSanPham(sanPhamId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting rating statistics for product: {}", sanPhamId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CUSTOMER REVIEW MANAGEMENT ====================

    /**
     * Get reviews by customer with pagination
     * @param nguoiDungId customer ID
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @return page of customer reviews
     */
    @GetMapping("/nguoi-dung/{nguoiDungId}")
    public ResponseEntity<Page<DanhGiaDto>> layDanhGiaTheoNguoiDung(
            @PathVariable Long nguoiDungId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Getting reviews for customer {} (page: {}, size: {})", nguoiDungId, page, size);

        try {
            Page<DanhGiaDto> result = danhGiaService.layDanhGiaTheoNguoiDung(nguoiDungId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting reviews for customer: {}", nguoiDungId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== REVIEW ELIGIBILITY ====================

    /**
     * Check if customer can review a specific product
     * @param customerId customer ID
     * @param productId product ID
     * @return eligibility result
     */
    @GetMapping("/kiem-tra-dieu-kien/{customerId}/{productId}")
    public ResponseEntity<ReviewEligibilityResponse> kiemTraDieuKienDanhGia(
            @PathVariable Long customerId,
            @PathVariable Long productId) {

        log.debug("Checking review eligibility for customer {} and product {}", customerId, productId);

        try {
            ReviewEligibilityService.ReviewEligibilityResult result = 
                eligibilityService.checkEligibility(customerId, productId);

            ReviewEligibilityResponse response = new ReviewEligibilityResponse(
                result.isEligible(),
                result.getMessage(),
                result.getEligibleOrderItem().map(item -> item.getId()).orElse(null)
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking review eligibility for customer {} and product {}", 
                     customerId, productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== RESPONSE CLASSES ====================

    /**
     * Response class for review eligibility check
     */
    public static class ReviewEligibilityResponse {
        private final boolean eligible;
        private final String message;
        private final Long eligibleOrderItemId;

        public ReviewEligibilityResponse(boolean eligible, String message, Long eligibleOrderItemId) {
            this.eligible = eligible;
            this.message = message;
            this.eligibleOrderItemId = eligibleOrderItemId;
        }

        public boolean isEligible() { return eligible; }
        public String getMessage() { return message; }
        public Long getEligibleOrderItemId() { return eligibleOrderItemId; }
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Handle validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    /**
     * Handle entity not found errors
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
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
