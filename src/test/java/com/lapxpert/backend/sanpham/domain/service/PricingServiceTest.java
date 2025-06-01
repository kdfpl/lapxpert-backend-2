package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PricingService to verify dynamic pricing calculations.
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @InjectMocks
    private PricingService pricingService;

    private SanPhamChiTiet productItem;
    private DotGiamGia activeCampaign;
    private DotGiamGia expiredCampaign;
    private DotGiamGia hiddenCampaign;

    @BeforeEach
    void setUp() {
        // Create test product
        SanPham sanPham = new SanPham();
        sanPham.setId(1L);
        sanPham.setTenSanPham("Test Laptop");

        productItem = new SanPhamChiTiet();
        productItem.setId(1L);
        productItem.setSerialNumber("LAPTOP001");
        productItem.setGiaBan(new BigDecimal("1000.00"));
        productItem.setTrangThai(TrangThaiSanPham.AVAILABLE);
        productItem.setSanPham(sanPham);
        productItem.setDotGiamGias(new LinkedHashSet<>());

        // Create active campaign (20% discount)
        activeCampaign = new DotGiamGia();
        activeCampaign.setId(1L);
        activeCampaign.setMaDotGiamGia("SALE20");
        activeCampaign.setTenDotGiamGia("20% Off Sale");
        activeCampaign.setPhanTramGiam(new BigDecimal("20.00"));
        activeCampaign.setTrangThai(TrangThaiCampaign.DA_DIEN_RA);
        activeCampaign.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        activeCampaign.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));

        // Create expired campaign
        expiredCampaign = new DotGiamGia();
        expiredCampaign.setId(2L);
        expiredCampaign.setMaDotGiamGia("EXPIRED30");
        expiredCampaign.setTenDotGiamGia("30% Off Expired");
        expiredCampaign.setPhanTramGiam(new BigDecimal("30.00"));
        expiredCampaign.setTrangThai(TrangThaiCampaign.KET_THUC);
        expiredCampaign.setNgayBatDau(Instant.now().minus(2, ChronoUnit.DAYS));
        expiredCampaign.setNgayKetThuc(Instant.now().minus(1, ChronoUnit.HOURS));

        // Create cancelled campaign
        hiddenCampaign = new DotGiamGia();
        hiddenCampaign.setId(3L);
        hiddenCampaign.setMaDotGiamGia("CANCELLED15");
        hiddenCampaign.setTenDotGiamGia("15% Off Cancelled");
        hiddenCampaign.setPhanTramGiam(new BigDecimal("15.00"));
        hiddenCampaign.setTrangThai(TrangThaiCampaign.BI_HUY); // Cancelled
        hiddenCampaign.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        hiddenCampaign.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void testCalculateEffectivePrice_NoDiscounts() {
        // Act
        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(productItem);

        // Assert
        assertEquals(new BigDecimal("1000.00"), effectivePrice);
    }

    @Test
    void testCalculateEffectivePrice_WithActiveDiscount() {
        // Arrange
        productItem.getDotGiamGias().add(activeCampaign);

        // Act
        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(productItem);

        // Assert
        assertEquals(new BigDecimal("800.00"), effectivePrice); // 1000 - (1000 * 20%)
    }

    @Test
    void testCalculateEffectivePrice_IgnoresExpiredCampaign() {
        // Arrange
        productItem.getDotGiamGias().add(expiredCampaign);

        // Act
        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(productItem);

        // Assert
        assertEquals(new BigDecimal("1000.00"), effectivePrice); // No discount applied
    }

    @Test
    void testCalculateEffectivePrice_IgnoresHiddenCampaign() {
        // Arrange
        productItem.getDotGiamGias().add(hiddenCampaign);

        // Act
        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(productItem);

        // Assert
        assertEquals(new BigDecimal("1000.00"), effectivePrice); // No discount applied
    }

    @Test
    void testCalculateEffectivePrice_MultipleCampaigns_ChoosesBest() {
        // Arrange - Add multiple campaigns, should choose the highest discount
        DotGiamGia campaign10 = new DotGiamGia();
        campaign10.setId(4L);
        campaign10.setPhanTramGiam(new BigDecimal("10.00"));
        campaign10.setTrangThai(TrangThaiCampaign.DA_DIEN_RA);
        campaign10.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        campaign10.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));

        productItem.getDotGiamGias().add(activeCampaign); // 20%
        productItem.getDotGiamGias().add(campaign10); // 10%

        // Act
        BigDecimal effectivePrice = pricingService.calculateEffectivePrice(productItem);

        // Assert
        assertEquals(new BigDecimal("800.00"), effectivePrice); // Should use 20% discount
    }

    @Test
    void testCalculateDetailedPrice_WithDiscount() {
        // Arrange
        productItem.getDotGiamGias().add(activeCampaign);

        // Act
        PricingService.PricingResult result = pricingService.calculateDetailedPrice(productItem);

        // Assert
        assertTrue(result.isHasDiscount());
        assertEquals(new BigDecimal("1000.00"), result.getOriginalPrice());
        assertEquals(new BigDecimal("800.00"), result.getEffectivePrice());
        assertEquals(new BigDecimal("200.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("20.00"), result.getDiscountPercentage());
        assertEquals(activeCampaign, result.getAppliedCampaign());
    }

    @Test
    void testCalculateDetailedPrice_NoDiscount() {
        // Act
        PricingService.PricingResult result = pricingService.calculateDetailedPrice(productItem);

        // Assert
        assertFalse(result.isHasDiscount());
        assertEquals(new BigDecimal("1000.00"), result.getOriginalPrice());
        assertEquals(new BigDecimal("1000.00"), result.getEffectivePrice());
        assertEquals(new BigDecimal("0"), result.getDiscountAmount());
        assertEquals(new BigDecimal("0"), result.getDiscountPercentage());
        assertNull(result.getAppliedCampaign());
    }

    @Test
    void testHasActiveDiscount() {
        // Test without discount
        assertFalse(pricingService.hasActiveDiscount(productItem));

        // Test with active discount
        productItem.getDotGiamGias().add(activeCampaign);
        assertTrue(pricingService.hasActiveDiscount(productItem));

        // Test with expired discount
        productItem.getDotGiamGias().clear();
        productItem.getDotGiamGias().add(expiredCampaign);
        assertFalse(pricingService.hasActiveDiscount(productItem));
    }

    @Test
    void testGetBestDiscountPercentage() {
        // Test without discount
        Optional<BigDecimal> discount = pricingService.getBestDiscountPercentage(productItem);
        assertTrue(discount.isEmpty());

        // Test with active discount
        productItem.getDotGiamGias().add(activeCampaign);
        discount = pricingService.getBestDiscountPercentage(productItem);
        assertTrue(discount.isPresent());
        assertEquals(new BigDecimal("20.00"), discount.get());
    }

    @Test
    void testCalculateEffectivePrice_NullProduct() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pricingService.calculateEffectivePrice(null)
        );

        assertTrue(exception.getMessage().contains("Product item and base price cannot be null"));
    }

    @Test
    void testCalculateEffectivePrice_NullBasePrice() {
        // Arrange
        productItem.setGiaBan(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pricingService.calculateEffectivePrice(productItem)
        );

        assertTrue(exception.getMessage().contains("Product item and base price cannot be null"));
    }
}
