package com.lapxpert.backend.danhgia.domain.service;

import com.lapxpert.backend.danhgia.domain.repository.DanhGiaRepository;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonChiTietRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for checking review eligibility based on purchase history
 * Validates customer purchase requirements and review submission rules
 * Integrates with HoaDon module for order verification
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewEligibilityService {

    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final ReviewBusinessRules businessRules;

    /**
     * Check if customer is eligible to review a specific product
     * @param customerId customer ID
     * @param productId product ID
     * @return eligibility result with details
     */
    public ReviewEligibilityResult checkEligibility(Long customerId, Long productId) {
        log.debug("Checking review eligibility for customer {} and product {}", customerId, productId);

        // Find all order items for this customer and product
        List<HoaDonChiTiet> orderItems = findEligibleOrderItems(customerId, productId);

        if (orderItems.isEmpty()) {
            log.debug("No eligible order items found for customer {} and product {}", customerId, productId);
            return ReviewEligibilityResult.notEligible(
                "Bạn chưa mua sản phẩm này hoặc đơn hàng chưa hoàn thành"
            );
        }

        // Check each order item for review eligibility
        for (HoaDonChiTiet orderItem : orderItems) {
            ReviewEligibilityResult result = checkOrderItemEligibility(orderItem);
            if (result.isEligible()) {
                log.debug("Found eligible order item {} for review", orderItem.getId());
                return result;
            }
        }

        log.debug("No eligible order items found after detailed check");
        return ReviewEligibilityResult.notEligible(
            "Đã đánh giá sản phẩm này hoặc đã quá thời hạn đánh giá (90 ngày)"
        );
    }

    /**
     * Check eligibility for a specific order item
     * @param orderItem order item to check
     * @return eligibility result
     */
    public ReviewEligibilityResult checkOrderItemEligibility(HoaDonChiTiet orderItem) {
        HoaDon order = orderItem.getHoaDon();

        // Check order status
        if (!businessRules.isOrderStatusEligibleForReview(order.getTrangThaiDonHang())) {
            return ReviewEligibilityResult.notEligible(
                "Đơn hàng chưa hoàn thành. Chỉ có thể đánh giá sau khi đã giao hàng thành công."
            );
        }

        // Check if already reviewed
        if (danhGiaRepository.existsByHoaDonChiTietId(orderItem.getId())) {
            return ReviewEligibilityResult.notEligible(
                "Bạn đã đánh giá sản phẩm này rồi"
            );
        }

        // Check time window
        if (!businessRules.isWithinReviewSubmissionWindow(order.getNgayCapNhat())) {
            return ReviewEligibilityResult.notEligible(
                "Đã quá thời hạn đánh giá. Bạn chỉ có thể đánh giá trong vòng 90 ngày sau khi nhận hàng."
            );
        }

        // Check customer ownership
        if (!order.getKhachHang().getId().equals(orderItem.getHoaDon().getKhachHang().getId())) {
            return ReviewEligibilityResult.notEligible(
                "Chỉ khách hàng mua hàng mới có thể đánh giá"
            );
        }

        return ReviewEligibilityResult.eligible(orderItem);
    }

    /**
     * Find order items eligible for review (completed orders, not yet reviewed)
     * @param customerId customer ID
     * @param productId product ID
     * @return list of eligible order items
     */
    private List<HoaDonChiTiet> findEligibleOrderItems(Long customerId, Long productId) {
        // This query should be added to HoaDonChiTietRepository
        // For now, we'll implement a basic version
        return hoaDonChiTietRepository.findEligibleForReview(customerId, productId);
    }

    /**
     * Get detailed eligibility information for customer dashboard
     * @param customerId customer ID
     * @return list of products eligible for review
     */
    public List<ReviewEligibilityInfo> getEligibleProductsForCustomer(Long customerId) {
        log.debug("Getting eligible products for customer {}", customerId);

        // Implementation would query for all completed orders
        // and check which products haven't been reviewed yet
        // This is a future enhancement for customer dashboard

        return List.of(); // Placeholder implementation
    }

    /**
     * Result class for review eligibility checks
     */
    public static class ReviewEligibilityResult {
        private final boolean eligible;
        private final String message;
        private final HoaDonChiTiet eligibleOrderItem;

        private ReviewEligibilityResult(boolean eligible, String message, HoaDonChiTiet eligibleOrderItem) {
            this.eligible = eligible;
            this.message = message;
            this.eligibleOrderItem = eligibleOrderItem;
        }

        public static ReviewEligibilityResult eligible(HoaDonChiTiet orderItem) {
            return new ReviewEligibilityResult(true, "Đủ điều kiện đánh giá", orderItem);
        }

        public static ReviewEligibilityResult notEligible(String reason) {
            return new ReviewEligibilityResult(false, reason, null);
        }

        public boolean isEligible() {
            return eligible;
        }

        public String getMessage() {
            return message;
        }

        public Optional<HoaDonChiTiet> getEligibleOrderItem() {
            return Optional.ofNullable(eligibleOrderItem);
        }
    }

    /**
     * Information class for eligible products
     */
    public static class ReviewEligibilityInfo {
        private final Long productId;
        private final String productName;
        private final Long orderItemId;
        private final java.time.Instant purchaseDate;
        private final java.time.Instant eligibilityExpiry;

        public ReviewEligibilityInfo(Long productId, String productName, Long orderItemId,
                                   java.time.Instant purchaseDate, java.time.Instant eligibilityExpiry) {
            this.productId = productId;
            this.productName = productName;
            this.orderItemId = orderItemId;
            this.purchaseDate = purchaseDate;
            this.eligibilityExpiry = eligibilityExpiry;
        }

        // Getters
        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public Long getOrderItemId() { return orderItemId; }
        public java.time.Instant getPurchaseDate() { return purchaseDate; }
        public java.time.Instant getEligibilityExpiry() { return eligibilityExpiry; }
    }
}
