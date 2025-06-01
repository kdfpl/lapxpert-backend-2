package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for calculating dynamic product pricing based on active discount campaigns (DotGiamGia).
 * This service replaces the manual giaKhuyenMai field with automatic price calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    /**
     * Calculate the effective selling price for a product item.
     * This considers all active discount campaigns and applies the best discount.
     *
     * @param sanPhamChiTiet The product item to calculate price for
     * @return The effective selling price after applying discounts
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateEffectivePrice(SanPhamChiTiet sanPhamChiTiet) {
        if (sanPhamChiTiet == null || sanPhamChiTiet.getGiaBan() == null) {
            throw new IllegalArgumentException("Product item and base price cannot be null");
        }

        BigDecimal basePrice = sanPhamChiTiet.getGiaBan();

        // Get active campaigns for this product
        List<DotGiamGia> activeCampaigns = getActiveCampaigns(sanPhamChiTiet);

        if (activeCampaigns.isEmpty()) {
            log.debug("No active campaigns for product item {}, using base price: {}",
                sanPhamChiTiet.getId(), basePrice);
            return basePrice;
        }

        // Apply the best discount (highest percentage)
        DotGiamGia bestCampaign = findBestCampaign(activeCampaigns);
        BigDecimal discountedPrice = applyDiscount(basePrice, bestCampaign.getPhanTramGiam());

        log.debug("Applied campaign '{}' ({}%) to product item {}. Price: {} -> {}",
            bestCampaign.getTenDotGiamGia(),
            bestCampaign.getPhanTramGiam(),
            sanPhamChiTiet.getId(),
            basePrice,
            discountedPrice);

        return discountedPrice;
    }

    /**
     * Calculate effective price with detailed information about applied discounts.
     *
     * @param sanPhamChiTiet The product item to calculate price for
     * @return Detailed pricing information
     */
    @Transactional(readOnly = true)
    public PricingResult calculateDetailedPrice(SanPhamChiTiet sanPhamChiTiet) {
        if (sanPhamChiTiet == null || sanPhamChiTiet.getGiaBan() == null) {
            throw new IllegalArgumentException("Product item and base price cannot be null");
        }

        BigDecimal basePrice = sanPhamChiTiet.getGiaBan();
        List<DotGiamGia> activeCampaigns = getActiveCampaigns(sanPhamChiTiet);

        if (activeCampaigns.isEmpty()) {
            return PricingResult.builder()
                .originalPrice(basePrice)
                .effectivePrice(basePrice)
                .discountAmount(BigDecimal.ZERO)
                .discountPercentage(BigDecimal.ZERO)
                .appliedCampaign(null)
                .hasDiscount(false)
                .build();
        }

        DotGiamGia bestCampaign = findBestCampaign(activeCampaigns);
        BigDecimal discountedPrice = applyDiscount(basePrice, bestCampaign.getPhanTramGiam());
        BigDecimal discountAmount = basePrice.subtract(discountedPrice);

        return PricingResult.builder()
            .originalPrice(basePrice)
            .effectivePrice(discountedPrice)
            .discountAmount(discountAmount)
            .discountPercentage(bestCampaign.getPhanTramGiam())
            .appliedCampaign(bestCampaign)
            .hasDiscount(true)
            .build();
    }

    /**
     * Get all active discount campaigns for a product item.
     * A campaign is active if:
     * - Status is DA_DIEN_RA
     * - Current time is between start and end dates
     * - Campaign is not hidden (daAn = false)
     *
     * @param sanPhamChiTiet The product item to check campaigns for
     * @return List of active campaigns
     */
    private List<DotGiamGia> getActiveCampaigns(SanPhamChiTiet sanPhamChiTiet) {
        Instant now = Instant.now();

        return sanPhamChiTiet.getDotGiamGias().stream()
            .filter(campaign -> campaign.getTrangThai() == TrangThaiCampaign.DA_DIEN_RA)
            .filter(campaign -> !campaign.getTrangThai().isCancelled()) // Not cancelled
            .filter(campaign -> isWithinDateRange(campaign, now))
            .collect(Collectors.toList());
    }

    /**
     * Check if current time is within campaign date range.
     */
    private boolean isWithinDateRange(DotGiamGia campaign, Instant now) {
        return now.isAfter(campaign.getNgayBatDau()) && now.isBefore(campaign.getNgayKetThuc());
    }

    /**
     * Find the best campaign (highest discount percentage).
     * In case of ties, prefer the campaign that ends sooner (more urgent).
     */
    private DotGiamGia findBestCampaign(List<DotGiamGia> campaigns) {
        return campaigns.stream()
            .max(Comparator
                .comparing(DotGiamGia::getPhanTramGiam)
                .thenComparing(campaign -> campaign.getNgayKetThuc(), Comparator.reverseOrder()))
            .orElseThrow(() -> new IllegalStateException("No campaigns available"));
    }

    /**
     * Apply discount percentage to base price.
     */
    private BigDecimal applyDiscount(BigDecimal basePrice, BigDecimal discountPercentage) {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice;
        }

        // Calculate discount amount: basePrice * (discountPercentage / 100)
        BigDecimal discountAmount = basePrice
            .multiply(discountPercentage)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Return discounted price
        return basePrice.subtract(discountAmount);
    }

    /**
     * Check if a product has any active discounts.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveDiscount(SanPhamChiTiet sanPhamChiTiet) {
        return !getActiveCampaigns(sanPhamChiTiet).isEmpty();
    }

    /**
     * Get the best available discount percentage for a product.
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getBestDiscountPercentage(SanPhamChiTiet sanPhamChiTiet) {
        List<DotGiamGia> activeCampaigns = getActiveCampaigns(sanPhamChiTiet);

        if (activeCampaigns.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(findBestCampaign(activeCampaigns).getPhanTramGiam());
    }

    /**
     * Data class for detailed pricing information.
     */
    @lombok.Builder
    @lombok.Data
    public static class PricingResult {
        private final BigDecimal originalPrice;
        private final BigDecimal effectivePrice;
        private final BigDecimal discountAmount;
        private final BigDecimal discountPercentage;
        private final DotGiamGia appliedCampaign;
        private final boolean hasDiscount;

        /**
         * Get savings amount as a formatted string.
         */
        public String getFormattedSavings() {
            if (!hasDiscount) {
                return "No discount";
            }
            return String.format("Save %s (%.1f%% off)",
                discountAmount.toString(),
                discountPercentage.doubleValue());
        }
    }
}
