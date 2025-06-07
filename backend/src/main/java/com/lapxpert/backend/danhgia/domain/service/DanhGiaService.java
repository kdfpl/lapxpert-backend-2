package com.lapxpert.backend.danhgia.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.application.dto.CreateReviewDto;
import com.lapxpert.backend.danhgia.application.dto.DanhGiaDto;
import com.lapxpert.backend.danhgia.application.dto.ProductRatingDto;
import com.lapxpert.backend.danhgia.application.dto.RatingDistributionDto;
import com.lapxpert.backend.danhgia.application.dto.ReviewFilterDto;
import com.lapxpert.backend.danhgia.application.dto.UpdateReviewDto;
import com.lapxpert.backend.danhgia.application.mapper.DanhGiaMapper;
import com.lapxpert.backend.danhgia.domain.entity.DanhGia;
import com.lapxpert.backend.danhgia.domain.repository.DanhGiaRepository;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonChiTietRepository;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core service for DanhGia (Review) module
 * Implements comprehensive review management with business rules validation
 * Provides CRUD operations, rating aggregation, and moderation workflows
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final DanhGiaMapper danhGiaMapper;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final ReviewEligibilityService eligibilityService;
    private final ReviewBusinessRules businessRules;
    private final ProductRatingCacheService ratingCacheService;

    // ==================== CORE CRUD OPERATIONS ====================

    /**
     * Create new review with comprehensive validation
     * @param createDto review creation data
     * @return created review DTO
     * @throws IllegalArgumentException if validation fails
     * @throws EntityNotFoundException if referenced entities not found
     */
    @Transactional
    public DanhGiaDto taoMoiDanhGia(CreateReviewDto createDto) {
        log.info("Creating new review for product {} by user {}",
                createDto.getSanPhamId(), createDto.getNguoiDungId());

        // 1. Validate review eligibility
        validateReviewEligibility(createDto);

        // 2. Check for existing review
        if (danhGiaRepository.existsByHoaDonChiTietId(createDto.getHoaDonChiTietId())) {
            throw new IllegalArgumentException("Đã có đánh giá cho sản phẩm này");
        }

        // 3. Validate business rules
        businessRules.validateReviewRating(createDto.getDiemDanhGia());
        businessRules.validateReviewContent(createDto.getNoiDung());
        businessRules.validateReviewImages(createDto.getHinhAnh());

        // 4. Create and populate entity
        DanhGia danhGia = danhGiaMapper.toEntity(createDto);
        setEntityReferences(danhGia, createDto);

        // 5. Apply auto-moderation
        TrangThaiDanhGia moderationStatus = businessRules.applyAutoModeration(danhGia);
        danhGia.setTrangThai(moderationStatus);

        // 6. Save review
        DanhGia savedReview = danhGiaRepository.save(danhGia);

        // 7. Invalidate product rating cache
        ratingCacheService.invalidateProductRating(createDto.getSanPhamId());

        log.info("Created review {} with status {} for product {} by user {}",
                savedReview.getId(), moderationStatus, createDto.getSanPhamId(), createDto.getNguoiDungId());

        return danhGiaMapper.toDto(savedReview);
    }

    /**
     * Update existing review with validation
     * @param reviewId review ID to update
     * @param updateDto update data
     * @return updated review DTO
     * @throws EntityNotFoundException if review not found
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public DanhGiaDto capNhatDanhGia(Long reviewId, UpdateReviewDto updateDto) {
        log.info("Updating review {} by user {}", reviewId, updateDto.getNguoiDungId());

        DanhGia existingReview = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        // Validate edit permissions
        validateEditPermissions(existingReview, updateDto);

        // Validate business rules
        businessRules.validateReviewRating(updateDto.getDiemDanhGia());
        businessRules.validateReviewContent(updateDto.getNoiDung());
        businessRules.validateReviewImages(updateDto.getHinhAnh());

        // Update fields using mapper
        danhGiaMapper.updateEntityFromDto(updateDto, existingReview);

        // Re-apply auto-moderation
        TrangThaiDanhGia moderationStatus = businessRules.applyAutoModeration(existingReview);
        existingReview.setTrangThai(moderationStatus);

        DanhGia savedReview = danhGiaRepository.save(existingReview);

        // Invalidate product rating cache
        ratingCacheService.invalidateProductRating(existingReview.getSanPham().getId());

        log.info("Updated review {} with new status {}", reviewId, moderationStatus);

        return danhGiaMapper.toDto(savedReview);
    }

    /**
     * Get review by ID
     * @param reviewId review ID
     * @return review DTO
     * @throws EntityNotFoundException if review not found
     */
    @Transactional(readOnly = true)
    public DanhGiaDto layDanhGiaTheoId(Long reviewId) {
        DanhGia review = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        return danhGiaMapper.toDto(review);
    }

    /**
     * Delete review (soft delete by setting status to DA_AN)
     * @param reviewId review ID to delete
     * @param reason deletion reason
     */
    @Transactional
    public void xoaDanhGia(Long reviewId, String reason) {
        DanhGia review = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        review.setTrangThai(TrangThaiDanhGia.DA_AN);
        danhGiaRepository.save(review);

        // Invalidate product rating cache
        ratingCacheService.invalidateProductRating(review.getSanPham().getId());

        log.info("Soft deleted review {} with reason: {}", reviewId, reason);
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get reviews for a product with filtering and pagination
     * @param sanPhamId product ID
     * @param filter filter criteria
     * @return page of reviews
     */
    @Transactional(readOnly = true)
    public Page<DanhGiaDto> layDanhGiaTheoSanPham(Long sanPhamId, ReviewFilterDto filter) {
        log.debug("Getting reviews for product {} with filter: {}", sanPhamId, filter);

        // Validate filter
        if (!filter.isValid()) {
            throw new IllegalArgumentException("Tham số lọc không hợp lệ");
        }

        // Create pageable with sorting
        Sort sort = createSortFromFilter(filter);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Query with filters
        Page<DanhGia> reviews = danhGiaRepository.findWithFilters(
            sanPhamId,
            null, // nguoiDungId
            TrangThaiDanhGia.DA_DUYET, // Only approved reviews for public display
            filter.getMinRating(),
            filter.getMaxRating(),
            filter.getFromDate(),
            filter.getToDate(),
            pageable
        );

        return reviews.map(danhGiaMapper::toDto);
    }

    /**
     * Get reviews by customer with pagination
     * @param nguoiDungId customer ID
     * @param page page number
     * @param size page size
     * @return page of customer reviews
     */
    @Transactional(readOnly = true)
    public Page<DanhGiaDto> layDanhGiaTheoNguoiDung(Long nguoiDungId, int page, int size) {
        log.debug("Getting reviews for customer {} (page: {}, size: {})", nguoiDungId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayTao"));
        Page<DanhGia> reviews = danhGiaRepository.findByNguoiDungIdOrderByNgayTaoDesc(nguoiDungId, pageable);

        return reviews.map(danhGiaMapper::toDto);
    }

    // ==================== RATING AGGREGATION ====================

    /**
     * Calculate and cache product rating statistics
     * @param sanPhamId product ID
     * @return product rating summary
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productRatings", key = "#sanPhamId")
    public ProductRatingDto tinhToanDanhGiaSanPham(Long sanPhamId) {
        log.debug("Calculating rating for product {}", sanPhamId);

        // Get basic statistics
        Optional<Double> averageRating = danhGiaRepository.calculateAverageRating(sanPhamId, TrangThaiDanhGia.DA_DUYET);
        Long totalReviews = danhGiaRepository.countReviewsByProduct(sanPhamId, TrangThaiDanhGia.DA_DUYET);

        // Get rating distribution
        RatingDistributionDto distribution = calculateRatingDistribution(sanPhamId);

        // Get recent reviews for display
        List<DanhGia> recentReviews = getRecentApprovedReviews(sanPhamId, 5);

        return ProductRatingDto.builder()
            .sanPhamId(sanPhamId)
            .averageRating(averageRating.orElse(0.0))
            .totalApprovedReviews(totalReviews.intValue())
            .distribution(distribution)
            .recentReviews(danhGiaMapper.toDtoList(recentReviews))
            .lastUpdated(Instant.now())
            .verifiedPurchaseCount(totalReviews.intValue()) // All reviews are verified purchases
            .build();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate review eligibility using eligibility service
     */
    private void validateReviewEligibility(CreateReviewDto createDto) {
        // Get order item and validate
        HoaDonChiTiet orderItem = hoaDonChiTietRepository.findById(createDto.getHoaDonChiTietId())
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết hóa đơn"));

        ReviewEligibilityService.ReviewEligibilityResult result =
            eligibilityService.checkOrderItemEligibility(orderItem);

        if (!result.isEligible()) {
            throw new IllegalArgumentException(result.getMessage());
        }
    }

    /**
     * Set entity references from IDs
     */
    private void setEntityReferences(DanhGia danhGia, CreateReviewDto createDto) {
        // Set user reference
        NguoiDung nguoiDung = nguoiDungRepository.findById(createDto.getNguoiDungId())
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        danhGia.setNguoiDung(nguoiDung);

        // Set product reference
        SanPham sanPham = sanPhamRepository.findById(createDto.getSanPhamId())
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm"));
        danhGia.setSanPham(sanPham);

        // Set order item reference
        HoaDonChiTiet hoaDonChiTiet = hoaDonChiTietRepository.findById(createDto.getHoaDonChiTietId())
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết hóa đơn"));
        danhGia.setHoaDonChiTiet(hoaDonChiTiet);
    }

    /**
     * Validate edit permissions
     */
    private void validateEditPermissions(DanhGia existingReview, UpdateReviewDto updateDto) {
        // Check ownership
        if (!existingReview.getNguoiDung().getId().equals(updateDto.getNguoiDungId())) {
            throw new IllegalArgumentException("Chỉ có thể chỉnh sửa đánh giá của chính mình");
        }

        // Check edit window
        if (!businessRules.canEditReview(existingReview.getNgayTao(), existingReview.getTrangThai())) {
            throw new IllegalArgumentException("Không thể chỉnh sửa đánh giá này. " +
                "Chỉ có thể chỉnh sửa trong vòng 24 giờ sau khi tạo và khi đang chờ duyệt.");
        }
    }

    /**
     * Create sort object from filter
     */
    private Sort createSortFromFilter(ReviewFilterDto filter) {
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "ngayTao";
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
            ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, sortBy);
    }

    /**
     * Calculate rating distribution for a product
     */
    private RatingDistributionDto calculateRatingDistribution(Long sanPhamId) {
        List<Object[]> distribution = danhGiaRepository.getRatingDistribution(sanPhamId, TrangThaiDanhGia.DA_DUYET);

        RatingDistributionDto result = new RatingDistributionDto();

        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];

            switch (rating) {
                case 1 -> result.setOneStar(count.intValue());
                case 2 -> result.setTwoStar(count.intValue());
                case 3 -> result.setThreeStar(count.intValue());
                case 4 -> result.setFourStar(count.intValue());
                case 5 -> result.setFiveStar(count.intValue());
            }
        }

        result.calculatePercentages();
        return result;
    }

    /**
     * Get recent approved reviews for a product
     */
    private List<DanhGia> getRecentApprovedReviews(Long sanPhamId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "ngayTao"));
        Page<DanhGia> reviews = danhGiaRepository.findBySanPhamWithDetails(sanPhamId, TrangThaiDanhGia.DA_DUYET, pageable);
        return reviews.getContent();
    }

    // ==================== MODERATION OPERATIONS ====================

    /**
     * Get pending reviews for admin moderation
     * @param page page number
     * @param size page size
     * @return page of pending reviews
     */
    @Transactional(readOnly = true)
    public Page<DanhGiaDto> layDanhGiaChoDuyet(int page, int size) {
        log.debug("Getting pending reviews for moderation (page: {}, size: {})", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "ngayTao"));
        Page<DanhGia> pendingReviews = danhGiaRepository.findWithFilters(
            null, null, TrangThaiDanhGia.CHO_DUYET, null, null, null, null, pageable);

        return pendingReviews.map(danhGiaMapper::toDto);
    }

    /**
     * Approve a review
     * @param reviewId review ID to approve
     * @param moderatorNote optional moderator note
     * @return approved review DTO
     */
    @Transactional
    public DanhGiaDto duyetDanhGia(Long reviewId, String moderatorNote) {
        log.info("Approving review {} with note: {}", reviewId, moderatorNote);

        DanhGia review = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        if (review.getTrangThai() != TrangThaiDanhGia.CHO_DUYET) {
            throw new IllegalArgumentException("Chỉ có thể duyệt đánh giá đang chờ duyệt");
        }

        review.setTrangThai(TrangThaiDanhGia.DA_DUYET);
        DanhGia savedReview = danhGiaRepository.save(review);

        // Invalidate product rating cache
        ratingCacheService.invalidateProductRating(review.getSanPham().getId());

        log.info("Approved review {}", reviewId);
        return danhGiaMapper.toDto(savedReview);
    }

    /**
     * Reject a review
     * @param reviewId review ID to reject
     * @param reason rejection reason
     * @return rejected review DTO
     */
    @Transactional
    public DanhGiaDto tuChoiDanhGia(Long reviewId, String reason) {
        log.info("Rejecting review {} with reason: {}", reviewId, reason);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Phải cung cấp lý do từ chối");
        }

        DanhGia review = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        if (review.getTrangThai() != TrangThaiDanhGia.CHO_DUYET) {
            throw new IllegalArgumentException("Chỉ có thể từ chối đánh giá đang chờ duyệt");
        }

        review.setTrangThai(TrangThaiDanhGia.BI_TU_CHOI);
        DanhGia savedReview = danhGiaRepository.save(review);

        // Invalidate product rating cache
        ratingCacheService.invalidateProductRating(review.getSanPham().getId());

        log.info("Rejected review {} with reason: {}", reviewId, reason);
        return danhGiaMapper.toDto(savedReview);
    }

    /**
     * Hide a review (admin action)
     * @param reviewId review ID to hide
     * @param reason hiding reason
     * @return hidden review DTO
     */
    @Transactional
    public DanhGiaDto anDanhGia(Long reviewId, String reason) {
        log.info("Hiding review {} with reason: {}", reviewId, reason);

        DanhGia review = danhGiaRepository.findById(reviewId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        review.setTrangThai(TrangThaiDanhGia.DA_AN);
        DanhGia savedReview = danhGiaRepository.save(review);

        // Invalidate product rating cache
        ratingCacheService.invalidateProductRating(review.getSanPham().getId());

        log.info("Hidden review {}", reviewId);
        return danhGiaMapper.toDto(savedReview);
    }

    // ==================== STATISTICS AND REPORTING ====================

    /**
     * Get review statistics for admin dashboard
     * @return review statistics
     */
    @Transactional(readOnly = true)
    public ReviewStatistics layThongKeDanhGia() {
        long totalReviews = danhGiaRepository.count();
        long pendingReviews = danhGiaRepository.countByTrangThai(TrangThaiDanhGia.CHO_DUYET);
        long approvedReviews = danhGiaRepository.countByTrangThai(TrangThaiDanhGia.DA_DUYET);
        long rejectedReviews = danhGiaRepository.countByTrangThai(TrangThaiDanhGia.BI_TU_CHOI);
        long hiddenReviews = danhGiaRepository.countByTrangThai(TrangThaiDanhGia.DA_AN);

        return new ReviewStatistics(totalReviews, pendingReviews, approvedReviews, rejectedReviews, hiddenReviews);
    }

    /**
     * Statistics class for review dashboard
     */
    public static class ReviewStatistics {
        private final long totalReviews;
        private final long pendingReviews;
        private final long approvedReviews;
        private final long rejectedReviews;
        private final long hiddenReviews;

        public ReviewStatistics(long totalReviews, long pendingReviews, long approvedReviews,
                              long rejectedReviews, long hiddenReviews) {
            this.totalReviews = totalReviews;
            this.pendingReviews = pendingReviews;
            this.approvedReviews = approvedReviews;
            this.rejectedReviews = rejectedReviews;
            this.hiddenReviews = hiddenReviews;
        }

        // Getters
        public long getTotalReviews() { return totalReviews; }
        public long getPendingReviews() { return pendingReviews; }
        public long getApprovedReviews() { return approvedReviews; }
        public long getRejectedReviews() { return rejectedReviews; }
        public long getHiddenReviews() { return hiddenReviews; }

        public double getApprovalRate() {
            return totalReviews > 0 ? (double) approvedReviews / totalReviews * 100.0 : 0.0;
        }
    }
}
