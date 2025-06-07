package com.lapxpert.backend.danhgia.application.dto;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import lombok.*;

import java.time.Instant;

/**
 * DTO for filtering and sorting product reviews
 * Used in review listing and search operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFilterDto {
    
    /**
     * Pagination parameters
     */
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 10;
    
    /**
     * Sorting parameters
     */
    private String sortBy; // "ngayTao", "diemDanhGia", "helpfulVotes"
    
    @Builder.Default
    private String sortDirection = "DESC"; // "ASC" or "DESC"
    
    /**
     * Filter by rating
     */
    private Integer minRating;
    private Integer maxRating;
    private Integer exactRating;
    
    /**
     * Filter by status
     */
    private TrangThaiDanhGia trangThai;
    
    /**
     * Filter by date range
     */
    private Instant fromDate;
    private Instant toDate;
    
    /**
     * Filter by review characteristics
     */
    private Boolean hasImages;
    private Boolean isVerifiedPurchase;
    private Boolean hasContent; // has text content
    
    /**
     * Filter by helpfulness
     */
    private Integer minHelpfulVotes;
    private Double minHelpfulnessPercentage;
    
    /**
     * Search parameters
     */
    private String searchKeyword; // search in review content
    private Long userId; // filter by specific user
    
    /**
     * Advanced filters
     */
    private Boolean excludeReported; // exclude reported reviews
    private Boolean featuredOnly; // only featured/highlighted reviews
    
    /**
     * Get sort field with fallback
     * @return sort field or default "ngayTao"
     */
    public String getSortByWithDefault() {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "ngayTao";
        }
        return sortBy;
    }
    
    /**
     * Check if date range filter is applied
     * @return true if both from and to dates are specified
     */
    public boolean hasDateRangeFilter() {
        return fromDate != null && toDate != null;
    }
    
    /**
     * Check if rating filter is applied
     * @return true if any rating filter is specified
     */
    public boolean hasRatingFilter() {
        return minRating != null || maxRating != null || exactRating != null;
    }
    
    /**
     * Validate filter parameters
     * @return true if filter parameters are valid
     */
    public boolean isValid() {
        // Validate page and size
        if (page < 0 || size <= 0 || size > 100) {
            return false;
        }
        
        // Validate rating range
        if (minRating != null && (minRating < 1 || minRating > 5)) {
            return false;
        }
        if (maxRating != null && (maxRating < 1 || maxRating > 5)) {
            return false;
        }
        if (exactRating != null && (exactRating < 1 || exactRating > 5)) {
            return false;
        }
        
        // Validate date range
        if (hasDateRangeFilter() && fromDate.isAfter(toDate)) {
            return false;
        }
        
        return true;
    }
}
