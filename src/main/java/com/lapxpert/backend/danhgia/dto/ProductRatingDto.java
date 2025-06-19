package com.lapxpert.backend.danhgia.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO for product rating aggregation and summary
 * Contains calculated rating information for products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingDto {
    
    private Long sanPhamId;
    private String tenSanPham;
    
    /**
     * Rating statistics
     */
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalApprovedReviews;
    private Integer totalPendingReviews;
    
    /**
     * Rating distribution
     */
    private RatingDistributionDto distribution;
    
    /**
     * Featured reviews (highest rated, most helpful, etc.)
     */
    private List<DanhGiaDto> featuredReviews;
    
    /**
     * Recent reviews for display
     */
    private List<DanhGiaDto> recentReviews;
    
    /**
     * Rating metadata
     */
    private Instant lastUpdated;
    private Instant lastReviewDate;
    
    /**
     * Quality indicators
     */
    private Double reviewQualityScore;
    private Integer verifiedPurchaseCount;
    private Integer reviewsWithImagesCount;
    
    /**
     * Check if product has enough reviews for reliable rating
     * @return true if product has 3 or more approved reviews
     */
    public boolean hasReliableRating() {
        return totalApprovedReviews != null && totalApprovedReviews >= 3;
    }
    
    /**
     * Get rounded average rating for display
     * @return average rating rounded to 1 decimal place
     */
    public Double getRoundedAverageRating() {
        if (averageRating == null) {
            return 0.0;
        }
        return Math.round(averageRating * 10.0) / 10.0;
    }
    
    /**
     * Get star rating (1-5 stars)
     * @return number of stars based on average rating
     */
    public Integer getStarRating() {
        if (averageRating == null) {
            return 0;
        }
        return (int) Math.round(averageRating);
    }
    
    /**
     * Check if product has recent reviews (within last 30 days)
     * @return true if last review was within 30 days
     */
    public boolean hasRecentReviews() {
        if (lastReviewDate == null) {
            return false;
        }
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        return lastReviewDate.isAfter(thirtyDaysAgo);
    }
    
    /**
     * Get percentage of verified purchase reviews
     * @return percentage of reviews from verified purchases
     */
    public Double getVerifiedPurchasePercentage() {
        if (totalApprovedReviews == null || totalApprovedReviews == 0 || verifiedPurchaseCount == null) {
            return 0.0;
        }
        return (double) verifiedPurchaseCount / totalApprovedReviews * 100.0;
    }
}
