package com.lapxpert.backend.danhgia.application.dto;

import lombok.*;

/**
 * DTO for rating distribution statistics
 * Shows breakdown of ratings by star count
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistributionDto {
    
    /**
     * Count of reviews by star rating
     */
    private Integer oneStar;
    private Integer twoStar;
    private Integer threeStar;
    private Integer fourStar;
    private Integer fiveStar;
    
    /**
     * Percentage distribution
     */
    private Double oneStarPercentage;
    private Double twoStarPercentage;
    private Double threeStarPercentage;
    private Double fourStarPercentage;
    private Double fiveStarPercentage;
    
    /**
     * Total count for validation
     */
    private Integer totalCount;
    
    /**
     * Calculate total number of reviews
     * @return sum of all star ratings
     */
    public Integer calculateTotalCount() {
        int total = 0;
        if (oneStar != null) total += oneStar;
        if (twoStar != null) total += twoStar;
        if (threeStar != null) total += threeStar;
        if (fourStar != null) total += fourStar;
        if (fiveStar != null) total += fiveStar;
        return total;
    }
    
    /**
     * Calculate percentages based on counts
     */
    public void calculatePercentages() {
        int total = calculateTotalCount();
        if (total == 0) {
            oneStarPercentage = twoStarPercentage = threeStarPercentage = 
            fourStarPercentage = fiveStarPercentage = 0.0;
            return;
        }
        
        oneStarPercentage = oneStar != null ? (double) oneStar / total * 100.0 : 0.0;
        twoStarPercentage = twoStar != null ? (double) twoStar / total * 100.0 : 0.0;
        threeStarPercentage = threeStar != null ? (double) threeStar / total * 100.0 : 0.0;
        fourStarPercentage = fourStar != null ? (double) fourStar / total * 100.0 : 0.0;
        fiveStarPercentage = fiveStar != null ? (double) fiveStar / total * 100.0 : 0.0;
        
        this.totalCount = total;
    }
    
    /**
     * Get count for specific star rating
     * @param stars star rating (1-5)
     * @return count of reviews for that rating
     */
    public Integer getCountForStars(int stars) {
        switch (stars) {
            case 1: return oneStar != null ? oneStar : 0;
            case 2: return twoStar != null ? twoStar : 0;
            case 3: return threeStar != null ? threeStar : 0;
            case 4: return fourStar != null ? fourStar : 0;
            case 5: return fiveStar != null ? fiveStar : 0;
            default: return 0;
        }
    }
    
    /**
     * Get percentage for specific star rating
     * @param stars star rating (1-5)
     * @return percentage of reviews for that rating
     */
    public Double getPercentageForStars(int stars) {
        switch (stars) {
            case 1: return oneStarPercentage != null ? oneStarPercentage : 0.0;
            case 2: return twoStarPercentage != null ? twoStarPercentage : 0.0;
            case 3: return threeStarPercentage != null ? threeStarPercentage : 0.0;
            case 4: return fourStarPercentage != null ? fourStarPercentage : 0.0;
            case 5: return fiveStarPercentage != null ? fiveStarPercentage : 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * Check if distribution is positive (more 4-5 star than 1-2 star reviews)
     * @return true if positive sentiment dominates
     */
    public boolean isPositiveDistribution() {
        int positive = getCountForStars(4) + getCountForStars(5);
        int negative = getCountForStars(1) + getCountForStars(2);
        return positive > negative;
    }
}
