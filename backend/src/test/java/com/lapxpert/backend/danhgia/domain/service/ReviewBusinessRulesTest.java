package com.lapxpert.backend.danhgia.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.domain.entity.DanhGia;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReviewBusinessRules
 * Tests business logic validation and auto-moderation rules
 */
@ExtendWith(MockitoExtension.class)
class ReviewBusinessRulesTest {

    @InjectMocks
    private ReviewBusinessRules reviewBusinessRules;

    private DanhGia validReview;
    private NguoiDung validUser;
    private SanPham validProduct;

    @BeforeEach
    void setUp() {
        validUser = new NguoiDung();
        validUser.setId(1L);

        validProduct = new SanPham();
        validProduct.setId(1L);

        validReview = DanhGia.builder()
                .id(1L)
                .nguoiDung(validUser)
                .sanPham(validProduct)
                .diemDanhGia(5)
                .noiDung("Sản phẩm rất tốt, chất lượng cao")
                .tieuDe("Laptop tuyệt vời")
                .hinhAnh(Arrays.asList("image1.jpg", "image2.jpg"))
                .trangThai(TrangThaiDanhGia.CHO_DUYET)
                .build();
    }

    @Test
    void validateReviewImages_ValidImageCount_ShouldPass() {
        // Arrange
        var validImages = Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg");

        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewImages(validImages));
    }

    @Test
    void validateReviewImages_ExceedsMaxLimit_ShouldThrowException() {
        // Arrange - 6 images exceeds the limit of 5
        var tooManyImages = Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg", "img6.jpg");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewBusinessRules.validateReviewImages(tooManyImages)
        );

        assertEquals("Số lượng hình ảnh không được vượt quá 5", exception.getMessage());
    }

    @Test
    void validateReviewImages_ExactlyMaxLimit_ShouldPass() {
        // Arrange - exactly 5 images (the limit)
        var maxImages = Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg");

        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewImages(maxImages));
    }

    @Test
    void validateReviewImages_EmptyList_ShouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewImages(Collections.emptyList()));
    }

    @Test
    void validateReviewImages_NullList_ShouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewImages(null));
    }

    @Test
    void shouldAutoApprove_HighRatingFewImages_ShouldReturnTrue() {
        // Arrange
        validReview.setDiemDanhGia(5);
        validReview.setHinhAnh(Arrays.asList("img1.jpg", "img2.jpg"));
        validReview.setNoiDung("Sản phẩm tuyệt vời");

        // Act
        boolean result = reviewBusinessRules.shouldAutoApprove(validReview);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldAutoApprove_LowRating_ShouldReturnFalse() {
        // Arrange
        validReview.setDiemDanhGia(2); // Below auto-approve threshold of 4
        validReview.setHinhAnh(Arrays.asList("img1.jpg"));
        validReview.setNoiDung("Sản phẩm bình thường");

        // Act
        boolean result = reviewBusinessRules.shouldAutoApprove(validReview);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldAutoApprove_TooManyImages_ShouldReturnFalse() {
        // Arrange
        validReview.setDiemDanhGia(5);
        validReview.setHinhAnh(Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg", "img6.jpg"));
        validReview.setNoiDung("Sản phẩm tuyệt vời");

        // Act
        boolean result = reviewBusinessRules.shouldAutoApprove(validReview);

        // Assert
        assertFalse(result);
    }

    @Test
    void shouldAutoReject_TooManyImages_ShouldReturnTrue() {
        // Arrange
        validReview.setHinhAnh(Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg", "img6.jpg"));

        // Act
        boolean result = reviewBusinessRules.shouldAutoReject(validReview);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldAutoReject_ProfanityContent_ShouldReturnTrue() {
        // Arrange
        validReview.setNoiDung("Sản phẩm đồ ngu");
        validReview.setHinhAnh(Arrays.asList("img1.jpg"));

        // Act
        boolean result = reviewBusinessRules.shouldAutoReject(validReview);

        // Assert
        assertTrue(result);
    }

    @Test
    void shouldAutoReject_SpamContent_ShouldReturnTrue() {
        // Arrange
        validReview.setNoiDung("Liên hệ ngay 0123456789 để mua ngay");
        validReview.setHinhAnh(Arrays.asList("img1.jpg"));

        // Act
        boolean result = reviewBusinessRules.shouldAutoReject(validReview);

        // Assert
        assertTrue(result);
    }

    @Test
    void applyAutoModeration_ShouldAutoApprove_ReturnsApprovedStatus() {
        // Arrange
        validReview.setDiemDanhGia(5);
        validReview.setHinhAnh(Arrays.asList("img1.jpg"));
        validReview.setNoiDung("Sản phẩm tuyệt vời");

        // Act
        TrangThaiDanhGia result = reviewBusinessRules.applyAutoModeration(validReview);

        // Assert
        assertEquals(TrangThaiDanhGia.DA_DUYET, result);
    }

    @Test
    void applyAutoModeration_ShouldAutoReject_ReturnsRejectedStatus() {
        // Arrange
        validReview.setNoiDung("Sản phẩm đồ ngu");

        // Act
        TrangThaiDanhGia result = reviewBusinessRules.applyAutoModeration(validReview);

        // Assert
        assertEquals(TrangThaiDanhGia.BI_TU_CHOI, result);
    }

    @Test
    void applyAutoModeration_RequiresManualReview_ReturnsPendingStatus() {
        // Arrange
        validReview.setDiemDanhGia(3); // Below auto-approve threshold but not auto-reject
        validReview.setNoiDung("Sản phẩm bình thường");

        // Act
        TrangThaiDanhGia result = reviewBusinessRules.applyAutoModeration(validReview);

        // Assert
        assertEquals(TrangThaiDanhGia.CHO_DUYET, result);
    }

    @Test
    void isOrderStatusEligibleForReview_CompletedOrder_ShouldReturnTrue() {
        // Act
        boolean result = reviewBusinessRules.isOrderStatusEligibleForReview(TrangThaiDonHang.HOAN_THANH);

        // Assert
        assertTrue(result);
    }

    @Test
    void isOrderStatusEligibleForReview_DeliveredOrder_ShouldReturnTrue() {
        // Act
        boolean result = reviewBusinessRules.isOrderStatusEligibleForReview(TrangThaiDonHang.DA_GIAO_HANG);

        // Assert
        assertTrue(result);
    }

    @Test
    void isOrderStatusEligibleForReview_PendingOrder_ShouldReturnFalse() {
        // Act
        boolean result = reviewBusinessRules.isOrderStatusEligibleForReview(TrangThaiDonHang.CHO_XAC_NHAN);

        // Assert
        assertFalse(result);
    }

    @Test
    void isWithinReviewSubmissionWindow_RecentOrder_ShouldReturnTrue() {
        // Arrange
        Instant recentDate = Instant.now().minus(30, ChronoUnit.DAYS);

        // Act
        boolean result = reviewBusinessRules.isWithinReviewSubmissionWindow(recentDate);

        // Assert
        assertTrue(result);
    }

    @Test
    void isWithinReviewSubmissionWindow_OldOrder_ShouldReturnFalse() {
        // Arrange
        Instant oldDate = Instant.now().minus(100, ChronoUnit.DAYS);

        // Act
        boolean result = reviewBusinessRules.isWithinReviewSubmissionWindow(oldDate);

        // Assert
        assertFalse(result);
    }

    @Test
    void canEditReview_RecentPendingReview_ShouldReturnTrue() {
        // Arrange
        Instant recentDate = Instant.now().minus(1, ChronoUnit.HOURS);

        // Act
        boolean result = reviewBusinessRules.canEditReview(recentDate, TrangThaiDanhGia.CHO_DUYET);

        // Assert
        assertTrue(result);
    }

    @Test
    void canEditReview_OldPendingReview_ShouldReturnFalse() {
        // Arrange
        Instant oldDate = Instant.now().minus(25, ChronoUnit.HOURS);

        // Act
        boolean result = reviewBusinessRules.canEditReview(oldDate, TrangThaiDanhGia.CHO_DUYET);

        // Assert
        assertFalse(result);
    }

    @Test
    void canEditReview_ApprovedReview_ShouldReturnFalse() {
        // Arrange
        Instant recentDate = Instant.now().minus(1, ChronoUnit.HOURS);

        // Act
        boolean result = reviewBusinessRules.canEditReview(recentDate, TrangThaiDanhGia.DA_DUYET);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateReviewRating_ValidRating_ShouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewRating(5));
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewRating(1));
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewRating(3));
    }

    @Test
    void validateReviewRating_InvalidRating_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reviewBusinessRules.validateReviewRating(0));
        assertThrows(IllegalArgumentException.class, () -> reviewBusinessRules.validateReviewRating(6));
        assertThrows(IllegalArgumentException.class, () -> reviewBusinessRules.validateReviewRating(null));
    }

    @Test
    void validateReviewContent_ValidContent_ShouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewContent("Sản phẩm tốt"));
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewContent(null));
        assertDoesNotThrow(() -> reviewBusinessRules.validateReviewContent(""));
    }

    @Test
    void validateReviewContent_TooLongContent_ShouldThrowException() {
        // Arrange
        String longContent = "a".repeat(1001);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewBusinessRules.validateReviewContent(longContent)
        );

        assertEquals("Nội dung đánh giá không được vượt quá 1000 ký tự", exception.getMessage());
    }
}
