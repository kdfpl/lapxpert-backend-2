package com.lapxpert.backend.danhgia.application.dto;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import lombok.*;

import java.time.Instant;

/**
 * DTO for filtering reviews in moderation interface
 * Used by admin users for review management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationFilterDto {
    
    /**
     * Pagination parameters
     */
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;
    
    /**
     * Sorting parameters
     */
    @Builder.Default
    private String sortBy = "ngayTao"; // "ngayTao", "diemDanhGia", "reportCount"
    
    @Builder.Default
    private String sortDirection = "ASC"; // "ASC" or "DESC" (oldest first for moderation)
    
    /**
     * Filter by moderation status
     */
    private TrangThaiDanhGia trangThai;
    
    /**
     * Filter by priority
     */
    private Boolean highPriority; // reviews that need immediate attention
    private Boolean hasReports; // reviews that have been reported
    private Integer minReportCount;
    
    /**
     * Filter by review characteristics
     */
    private Integer minRating;
    private Integer maxRating;
    private Boolean hasImages;
    private Boolean isVerifiedPurchase;
    
    /**
     * Filter by date range
     */
    private Instant fromDate;
    private Instant toDate;
    
    /**
     * Filter by user characteristics
     */
    private Boolean newUserReviews; // reviews from users with < 3 total reviews
    private Boolean suspiciousActivity; // potential spam or fake reviews
    
    /**
     * Filter by content characteristics
     */
    private Boolean hasKeywords; // contains flagged keywords
    private Boolean duplicateContent; // similar to other reviews
    private Boolean shortContent; // very short reviews
    private Boolean longContent; // very long reviews
    
    /**
     * Search parameters
     */
    private String searchKeyword;
    private Long productId;
    private Long userId;
    
    /**
     * Moderator assignment
     */
    private Long assignedModeratorId;
    private Boolean unassigned; // reviews not assigned to any moderator
    
    /**
     * Get default filters for pending reviews
     * @return filter configured for pending review moderation
     */
    public static ReviewModerationFilterDto forPendingReviews() {
        return ReviewModerationFilterDto.builder()
            .trangThai(TrangThaiDanhGia.CHO_DUYET)
            .sortBy("ngayTao")
            .sortDirection("ASC")
            .size(20)
            .build();
    }
    
    /**
     * Get default filters for reported reviews
     * @return filter configured for reported review moderation
     */
    public static ReviewModerationFilterDto forReportedReviews() {
        return ReviewModerationFilterDto.builder()
            .hasReports(true)
            .sortBy("reportCount")
            .sortDirection("DESC")
            .size(20)
            .build();
    }
    
    /**
     * Get default filters for suspicious reviews
     * @return filter configured for suspicious review detection
     */
    public static ReviewModerationFilterDto forSuspiciousReviews() {
        return ReviewModerationFilterDto.builder()
            .suspiciousActivity(true)
            .sortBy("ngayTao")
            .sortDirection("DESC")
            .size(20)
            .build();
    }
    
    /**
     * Check if any priority filters are applied
     * @return true if filtering for high priority items
     */
    public boolean hasPriorityFilters() {
        return Boolean.TRUE.equals(highPriority) || 
               Boolean.TRUE.equals(hasReports) || 
               Boolean.TRUE.equals(suspiciousActivity);
    }
}
