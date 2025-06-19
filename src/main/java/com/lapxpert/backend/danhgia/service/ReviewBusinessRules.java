package com.lapxpert.backend.danhgia.service;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.entity.DanhGia;
import com.lapxpert.backend.hoadon.enums.TrangThaiDonHang;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Business rules and validation logic for review system
 * Centralizes all business rule definitions and validation methods
 * Follows Vietnamese e-commerce context requirements
 */
@Component
@Slf4j
public class ReviewBusinessRules {

    // ==================== BUSINESS RULE CONSTANTS ====================

    /**
     * Review submission time window in days after order completion
     */
    public static final int REVIEW_SUBMISSION_DAYS = 90;

    /**
     * Review editing time window in hours after creation
     */
    public static final int REVIEW_EDIT_HOURS = 24;

    /**
     * Maximum number of images allowed per review
     */
    public static final int MAX_IMAGES_PER_REVIEW = 5;

    /**
     * Minimum rating for auto-approval
     */
    public static final int AUTO_APPROVE_MIN_RATING = 4;

    /**
     * Maximum images for auto-approval
     */
    public static final int AUTO_APPROVE_MAX_IMAGES = 5;

    /**
     * Order statuses that allow review submission
     */
    public static final List<TrangThaiDonHang> REVIEW_ELIGIBLE_STATUSES = Arrays.asList(
        TrangThaiDonHang.DA_GIAO_HANG,
        TrangThaiDonHang.HOAN_THANH
    );

    // ==================== CONTENT FILTERING PATTERNS ====================

    /**
     * Basic profanity patterns (Vietnamese context)
     * In production, this should be replaced with a comprehensive content filtering service
     */
    private static final List<Pattern> PROFANITY_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*\\b(đồ\\s*ngu|ngu\\s*ngốc|khốn\\s*nạn)\\b.*"),
        Pattern.compile("(?i).*\\b(chết\\s*tiệt|đồ\\s*khốn)\\b.*"),
        Pattern.compile("(?i).*\\b(fuck|shit|damn)\\b.*") // English profanity
    );

    /**
     * Spam content patterns
     */
    private static final List<Pattern> SPAM_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*(mua\\s*ngay|liên\\s*hệ|số\\s*điện\\s*thoại).*"),
        Pattern.compile("(?i).*(khuyến\\s*mãi|giảm\\s*giá|ưu\\s*đãi).*"),
        Pattern.compile(".*([0-9]{10,11}).*") // Phone numbers
    );

    // ==================== ORDER ELIGIBILITY VALIDATION ====================

    /**
     * Check if order status allows review submission
     * @param orderStatus current order status
     * @return true if order is eligible for review
     */
    public boolean isOrderStatusEligibleForReview(TrangThaiDonHang orderStatus) {
        boolean eligible = REVIEW_ELIGIBLE_STATUSES.contains(orderStatus);
        log.debug("Order status {} is {} for review", orderStatus, eligible ? "eligible" : "not eligible");
        return eligible;
    }

    /**
     * Check if review submission is within allowed time window
     * @param orderCompletionDate date when order was completed
     * @return true if within submission window
     */
    public boolean isWithinReviewSubmissionWindow(Instant orderCompletionDate) {
        if (orderCompletionDate == null) {
            log.warn("Order completion date is null, rejecting review submission");
            return false;
        }

        Instant cutoffDate = orderCompletionDate.plus(REVIEW_SUBMISSION_DAYS, ChronoUnit.DAYS);
        boolean withinWindow = Instant.now().isBefore(cutoffDate);

        log.debug("Review submission window check: order completed {}, cutoff {}, within window: {}",
                 orderCompletionDate, cutoffDate, withinWindow);

        return withinWindow;
    }

    /**
     * Check if review can be edited (within edit window and correct status)
     * @param reviewCreationDate when review was created
     * @param currentStatus current review status
     * @return true if review can be edited
     */
    public boolean canEditReview(Instant reviewCreationDate, TrangThaiDanhGia currentStatus) {
        if (reviewCreationDate == null) {
            return false;
        }

        // Only pending reviews can be edited
        if (currentStatus != TrangThaiDanhGia.CHO_DUYET) {
            log.debug("Review cannot be edited - status is {}, only CHO_DUYET reviews can be edited", currentStatus);
            return false;
        }

        Instant editCutoff = reviewCreationDate.plus(REVIEW_EDIT_HOURS, ChronoUnit.HOURS);
        boolean canEdit = Instant.now().isBefore(editCutoff);

        log.debug("Review edit window check: created {}, cutoff {}, can edit: {}",
                 reviewCreationDate, editCutoff, canEdit);

        return canEdit;
    }

    // ==================== AUTO-MODERATION RULES ====================

    /**
     * Determine if review should be automatically approved
     * @param review review to evaluate
     * @return true if should be auto-approved
     */
    public boolean shouldAutoApprove(DanhGia review) {
        if (review.getDiemDanhGia() == null || review.getDiemDanhGia() < AUTO_APPROVE_MIN_RATING) {
            log.debug("Review rating {} below auto-approve threshold {}",
                     review.getDiemDanhGia(), AUTO_APPROVE_MIN_RATING);
            return false;
        }

        if (review.getHinhAnh() != null && review.getHinhAnh().size() > AUTO_APPROVE_MAX_IMAGES) {
            log.debug("Review has {} images, exceeds auto-approve limit {}",
                     review.getHinhAnh().size(), AUTO_APPROVE_MAX_IMAGES);
            return false;
        }

        if (containsFlaggedContent(review.getNoiDung())) {
            log.debug("Review content contains flagged content, cannot auto-approve");
            return false;
        }

        log.debug("Review meets auto-approval criteria");
        return true;
    }

    /**
     * Determine if review should be automatically rejected
     * @param review review to evaluate
     * @return true if should be auto-rejected
     */
    public boolean shouldAutoReject(DanhGia review) {
        if (containsProfanity(review.getNoiDung())) {
            log.debug("Review contains profanity, auto-rejecting");
            return true;
        }

        if (isSpamContent(review.getNoiDung())) {
            log.debug("Review appears to be spam, auto-rejecting");
            return true;
        }

        if (review.getHinhAnh() != null && review.getHinhAnh().size() > MAX_IMAGES_PER_REVIEW) {
            log.debug("Review has {} images, exceeds maximum limit {}",
                     review.getHinhAnh().size(), MAX_IMAGES_PER_REVIEW);
            return true;
        }

        return false;
    }

    /**
     * Apply auto-moderation logic to determine review status
     * @param review review to moderate
     * @return appropriate status after auto-moderation
     */
    public TrangThaiDanhGia applyAutoModeration(DanhGia review) {
        if (shouldAutoReject(review)) {
            log.info("Auto-rejecting review for product {} by user {}",
                    review.getSanPham().getId(), review.getNguoiDung().getId());
            return TrangThaiDanhGia.BI_TU_CHOI;
        }

        if (shouldAutoApprove(review)) {
            log.info("Auto-approving review for product {} by user {}",
                    review.getSanPham().getId(), review.getNguoiDung().getId());
            return TrangThaiDanhGia.DA_DUYET;
        }

        log.debug("Review requires manual moderation");
        return TrangThaiDanhGia.CHO_DUYET;
    }

    // ==================== CONTENT FILTERING METHODS ====================

    /**
     * Check if content contains flagged keywords or patterns
     * @param content content to check
     * @return true if content is flagged
     */
    public boolean containsFlaggedContent(String content) {
        return containsProfanity(content) || isSpamContent(content);
    }

    /**
     * Check if content contains profanity
     * @param content content to check
     * @return true if profanity detected
     */
    public boolean containsProfanity(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String normalizedContent = content.toLowerCase().trim();
        return PROFANITY_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(normalizedContent).matches());
    }

    /**
     * Check if content appears to be spam
     * @param content content to check
     * @return true if spam detected
     */
    public boolean isSpamContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String normalizedContent = content.toLowerCase().trim();
        return SPAM_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(normalizedContent).matches());
    }

    // ==================== VALIDATION HELPER METHODS ====================

    /**
     * Validate review content length and format
     * @param content review content
     * @throws IllegalArgumentException if content is invalid
     */
    public void validateReviewContent(String content) {
        if (content != null && content.length() > 1000) {
            throw new IllegalArgumentException("Nội dung đánh giá không được vượt quá 1000 ký tự");
        }
    }

    /**
     * Validate review rating
     * @param rating review rating
     * @throws IllegalArgumentException if rating is invalid
     */
    public void validateReviewRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }
    }

    /**
     * Validate review images
     * @param imageUrls list of image URLs
     * @throws IllegalArgumentException if images are invalid
     */
    public void validateReviewImages(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > MAX_IMAGES_PER_REVIEW) {
            throw new IllegalArgumentException(
                String.format("Số lượng hình ảnh không được vượt quá %d", MAX_IMAGES_PER_REVIEW));
        }
    }
}
