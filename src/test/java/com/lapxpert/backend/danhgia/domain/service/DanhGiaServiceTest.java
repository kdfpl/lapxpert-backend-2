package com.lapxpert.backend.danhgia.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.application.dto.CreateReviewDto;
import com.lapxpert.backend.danhgia.application.dto.DanhGiaDto;
import com.lapxpert.backend.danhgia.application.mapper.DanhGiaMapper;
import com.lapxpert.backend.danhgia.domain.entity.DanhGia;
import com.lapxpert.backend.danhgia.domain.repository.DanhGiaRepository;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonChiTiet;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonChiTietRepository;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DanhGiaService
 * Tests core functionality including business rules validation and auto-moderation
 */
@ExtendWith(MockitoExtension.class)
class DanhGiaServiceTest {

    @InjectMocks
    private DanhGiaService danhGiaService;

    @Mock
    private DanhGiaRepository danhGiaRepository;

    @Mock
    private DanhGiaMapper danhGiaMapper;

    @Mock
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private SanPhamRepository sanPhamRepository;

    @Mock
    private ReviewEligibilityService eligibilityService;

    @Mock
    private ReviewBusinessRules businessRules;

    @Mock
    private ProductRatingCacheService ratingCacheService;

    private CreateReviewDto validCreateDto;
    private DanhGia validReview;
    private HoaDonChiTiet validOrderItem;
    private NguoiDung validUser;
    private SanPham validProduct;
    private HoaDon validOrder;

    @BeforeEach
    void setUp() {
        // Setup valid user
        validUser = new NguoiDung();
        validUser.setId(1L);
        validUser.setHoTen("Nguyễn Văn A");

        // Setup valid product
        validProduct = new SanPham();
        validProduct.setId(1L);
        validProduct.setTenSanPham("Laptop Dell XPS 13");

        // Setup valid order
        validOrder = new HoaDon();
        validOrder.setId(1L);
        validOrder.setTrangThaiDonHang(TrangThaiDonHang.HOAN_THANH);
        validOrder.setNgayCapNhat(Instant.now().minus(10, ChronoUnit.DAYS));
        validOrder.setKhachHang(validUser);

        // Setup valid order item
        validOrderItem = new HoaDonChiTiet();
        validOrderItem.setId(1L);
        validOrderItem.setHoaDon(validOrder);

        // Setup valid review entity
        validReview = DanhGia.builder()
                .id(1L)
                .nguoiDung(validUser)
                .sanPham(validProduct)
                .hoaDonChiTiet(validOrderItem)
                .diemDanhGia(5)
                .noiDung("Sản phẩm rất tốt, chất lượng cao")
                .tieuDe("Laptop tuyệt vời")
                .hinhAnh(Arrays.asList("image1.jpg", "image2.jpg"))
                .trangThai(TrangThaiDanhGia.CHO_DUYET)
                .build();

        // Setup valid create DTO
        validCreateDto = CreateReviewDto.builder()
                .nguoiDungId(1L)
                .sanPhamId(1L)
                .hoaDonChiTietId(1L)
                .diemDanhGia(5)
                .noiDung("Sản phẩm rất tốt, chất lượng cao")
                .tieuDe("Laptop tuyệt vời")
                .hinhAnh(Arrays.asList("image1.jpg", "image2.jpg"))
                .build();
    }

    @Test
    void taoMoiDanhGia_ValidInput_ShouldCreateReview() {
        // Arrange
        when(hoaDonChiTietRepository.findById(1L)).thenReturn(Optional.of(validOrderItem));
        when(nguoiDungRepository.findById(1L)).thenReturn(Optional.of(validUser));
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(validProduct));
        when(danhGiaRepository.existsByHoaDonChiTietId(1L)).thenReturn(false);
        when(danhGiaMapper.toEntity(validCreateDto)).thenReturn(validReview);
        when(businessRules.applyAutoModeration(any(DanhGia.class))).thenReturn(TrangThaiDanhGia.DA_DUYET);
        when(danhGiaRepository.save(any(DanhGia.class))).thenReturn(validReview);
        when(danhGiaMapper.toDto(validReview)).thenReturn(new DanhGiaDto());

        ReviewEligibilityService.ReviewEligibilityResult eligibilityResult =
            ReviewEligibilityService.ReviewEligibilityResult.eligible(validOrderItem);
        when(eligibilityService.checkOrderItemEligibility(validOrderItem)).thenReturn(eligibilityResult);

        // Act
        DanhGiaDto result = danhGiaService.taoMoiDanhGia(validCreateDto);

        // Assert
        assertNotNull(result);
        verify(danhGiaRepository).save(any(DanhGia.class));
        verify(ratingCacheService).invalidateProductRating(1L);
        verify(businessRules).validateReviewRating(5);
        verify(businessRules).validateReviewContent("Sản phẩm rất tốt, chất lượng cao");
        verify(businessRules).validateReviewImages(Arrays.asList("image1.jpg", "image2.jpg"));
    }

    @Test
    void taoMoiDanhGia_ExistingReview_ShouldThrowException() {
        // Arrange
        when(hoaDonChiTietRepository.findById(1L)).thenReturn(Optional.of(validOrderItem));
        when(danhGiaRepository.existsByHoaDonChiTietId(1L)).thenReturn(true);

        ReviewEligibilityService.ReviewEligibilityResult eligibilityResult =
            ReviewEligibilityService.ReviewEligibilityResult.eligible(validOrderItem);
        when(eligibilityService.checkOrderItemEligibility(validOrderItem)).thenReturn(eligibilityResult);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> danhGiaService.taoMoiDanhGia(validCreateDto)
        );

        assertEquals("Đã có đánh giá cho sản phẩm này", exception.getMessage());
        verify(danhGiaRepository, never()).save(any(DanhGia.class));
    }

    @Test
    void taoMoiDanhGia_InvalidRating_ShouldThrowException() {
        // Arrange
        validCreateDto.setDiemDanhGia(6); // Invalid rating
        when(hoaDonChiTietRepository.findById(1L)).thenReturn(Optional.of(validOrderItem));
        when(danhGiaRepository.existsByHoaDonChiTietId(1L)).thenReturn(false);

        ReviewEligibilityService.ReviewEligibilityResult eligibilityResult =
            ReviewEligibilityService.ReviewEligibilityResult.eligible(validOrderItem);
        when(eligibilityService.checkOrderItemEligibility(validOrderItem)).thenReturn(eligibilityResult);

        doThrow(new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5"))
            .when(businessRules).validateReviewRating(6);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> danhGiaService.taoMoiDanhGia(validCreateDto)
        );

        assertEquals("Điểm đánh giá phải từ 1 đến 5", exception.getMessage());
    }

    @Test
    void taoMoiDanhGia_TooManyImages_ShouldThrowException() {
        // Arrange
        validCreateDto.setHinhAnh(Arrays.asList("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg", "img6.jpg"));
        when(hoaDonChiTietRepository.findById(1L)).thenReturn(Optional.of(validOrderItem));
        when(danhGiaRepository.existsByHoaDonChiTietId(1L)).thenReturn(false);

        ReviewEligibilityService.ReviewEligibilityResult eligibilityResult =
            ReviewEligibilityService.ReviewEligibilityResult.eligible(validOrderItem);
        when(eligibilityService.checkOrderItemEligibility(validOrderItem)).thenReturn(eligibilityResult);

        doThrow(new IllegalArgumentException("Số lượng hình ảnh không được vượt quá 5"))
            .when(businessRules).validateReviewImages(any());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> danhGiaService.taoMoiDanhGia(validCreateDto)
        );

        assertEquals("Số lượng hình ảnh không được vượt quá 5", exception.getMessage());
    }

    @Test
    void taoMoiDanhGia_AutoApproval_ShouldSetApprovedStatus() {
        // Arrange
        when(hoaDonChiTietRepository.findById(1L)).thenReturn(Optional.of(validOrderItem));
        when(nguoiDungRepository.findById(1L)).thenReturn(Optional.of(validUser));
        when(sanPhamRepository.findById(1L)).thenReturn(Optional.of(validProduct));
        when(danhGiaRepository.existsByHoaDonChiTietId(1L)).thenReturn(false);
        when(danhGiaMapper.toEntity(validCreateDto)).thenReturn(validReview);
        when(businessRules.applyAutoModeration(any(DanhGia.class))).thenReturn(TrangThaiDanhGia.DA_DUYET);
        when(danhGiaRepository.save(any(DanhGia.class))).thenReturn(validReview);
        when(danhGiaMapper.toDto(validReview)).thenReturn(new DanhGiaDto());

        ReviewEligibilityService.ReviewEligibilityResult eligibilityResult =
            ReviewEligibilityService.ReviewEligibilityResult.eligible(validOrderItem);
        when(eligibilityService.checkOrderItemEligibility(validOrderItem)).thenReturn(eligibilityResult);

        // Act
        danhGiaService.taoMoiDanhGia(validCreateDto);

        // Assert
        verify(businessRules).applyAutoModeration(any(DanhGia.class));
    }
}
