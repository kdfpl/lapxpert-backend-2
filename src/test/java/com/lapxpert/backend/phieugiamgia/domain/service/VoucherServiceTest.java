package com.lapxpert.backend.phieugiamgia.domain.service;

import com.lapxpert.backend.common.service.EmailService;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonPhieuGiamGia;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonPhieuGiamGiaRepository;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import com.lapxpert.backend.nguoidung.domain.repository.NguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaNguoiDungRepository;
import com.lapxpert.backend.phieugiamgia.domain.repository.PhieuGiamGiaRepository;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private PhieuGiamGiaNguoiDungRepository phieuGiamGiaNguoiDungRepository;

    @Mock
    private HoaDonPhieuGiamGiaRepository hoaDonPhieuGiamGiaRepository;

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PhieuGiamGiaService phieuGiamGiaService;

    private PhieuGiamGia validVoucher;
    private PhieuGiamGia expiredVoucher;
    private PhieuGiamGia privateVoucher;
    private NguoiDung customer;
    private HoaDon hoaDon;

    @BeforeEach
    void setUp() {
        // Create test customer
        customer = new NguoiDung();
        customer.setId(1L);
        customer.setEmail("test@example.com");

        // Create test order
        hoaDon = new HoaDon();
        hoaDon.setId(1L);
        hoaDon.setKhachHang(customer);

        // Create valid voucher (fixed amount)
        validVoucher = new PhieuGiamGia();
        validVoucher.setId(1L);
        validVoucher.setMaPhieuGiamGia("VALID10");
        validVoucher.setLoaiGiamGia(LoaiGiamGia.SO_TIEN_CO_DINH); // Fixed amount
        validVoucher.setGiaTriGiam(new BigDecimal("10.00"));
        validVoucher.setGiaTriDonHangToiThieu(new BigDecimal("50.00"));
        validVoucher.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        validVoucher.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));
        validVoucher.setSoLuongBanDau(100);
        validVoucher.setSoLuongDaDung(10);

        // Create expired voucher
        expiredVoucher = new PhieuGiamGia();
        expiredVoucher.setId(2L);
        expiredVoucher.setMaPhieuGiamGia("EXPIRED10");
        expiredVoucher.setLoaiGiamGia(LoaiGiamGia.SO_TIEN_CO_DINH);
        expiredVoucher.setGiaTriGiam(new BigDecimal("10.00"));
        expiredVoucher.setNgayBatDau(Instant.now().minus(10, ChronoUnit.DAYS));
        expiredVoucher.setNgayKetThuc(Instant.now().minus(1, ChronoUnit.DAYS));
        expiredVoucher.setSoLuongBanDau(100);
        expiredVoucher.setSoLuongDaDung(10);

        // Create private voucher (percentage type)
        privateVoucher = new PhieuGiamGia();
        privateVoucher.setId(3L);
        privateVoucher.setMaPhieuGiamGia("PRIVATE20");
        privateVoucher.setLoaiGiamGia(LoaiGiamGia.PHAN_TRAM); // Percentage
        privateVoucher.setGiaTriGiam(new BigDecimal("20.00"));
        privateVoucher.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        privateVoucher.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));
        privateVoucher.setSoLuongBanDau(50);
        privateVoucher.setSoLuongDaDung(5);

        // Create user assignment for private voucher
        PhieuGiamGiaNguoiDung assignment = new PhieuGiamGiaNguoiDung();
        assignment.setPhieuGiamGia(privateVoucher);
        assignment.setNguoiDung(customer);
        assignment.setDaSuDung(false);
        privateVoucher.getDanhSachNguoiDung().add(assignment);
    }

    @Test
    void validateVoucher_ValidFixedAmountVoucher_ShouldReturnValid() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("100.00");
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("VALID10")).thenReturn(Optional.of(validVoucher));

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("VALID10", customer, orderTotal);

        // Assert
        assertTrue(result.isValid());
        assertEquals(new BigDecimal("10.00"), result.getDiscountAmount());
        assertEquals(validVoucher, result.getVoucher());
        assertNull(result.getErrorMessage());
    }

    @Test
    void validateVoucher_ValidPercentageVoucher_ShouldCalculateCorrectDiscount() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("100.00");
        PhieuGiamGia percentageVoucher = new PhieuGiamGia();
        percentageVoucher.setId(4L);
        percentageVoucher.setMaPhieuGiamGia("PERCENT20");
        percentageVoucher.setLoaiGiamGia(LoaiGiamGia.PHAN_TRAM); // Percentage
        percentageVoucher.setGiaTriGiam(new BigDecimal("20.00")); // 20%
        percentageVoucher.setNgayBatDau(Instant.now().minus(1, ChronoUnit.DAYS));
        percentageVoucher.setNgayKetThuc(Instant.now().plus(1, ChronoUnit.DAYS));
        percentageVoucher.setSoLuongBanDau(100);
        percentageVoucher.setSoLuongDaDung(10);

        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("PERCENT20")).thenReturn(Optional.of(percentageVoucher));

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("PERCENT20", customer, orderTotal);

        // Assert
        assertTrue(result.isValid());
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getDiscountAmount())); // 20% of 100
    }

    @Test
    void validateVoucher_NonExistentVoucher_ShouldReturnInvalid() {
        // Arrange
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("NONEXISTENT")).thenReturn(Optional.empty());

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("NONEXISTENT", customer, new BigDecimal("100.00"));

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Voucher code not found"));
    }

    @Test
    void validateVoucher_ExpiredVoucher_ShouldReturnInvalid() {
        // Arrange
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("EXPIRED10")).thenReturn(Optional.of(expiredVoucher));

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("EXPIRED10", customer, new BigDecimal("100.00"));

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not currently active"));
    }

    @Test
    void validateVoucher_OrderBelowMinimum_ShouldReturnInvalid() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("30.00"); // Below minimum of 50
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("VALID10")).thenReturn(Optional.of(validVoucher));

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("VALID10", customer, orderTotal);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("below minimum required"));
    }

    @Test
    void validateVoucher_UsageLimitExceeded_ShouldReturnInvalid() {
        // Arrange
        validVoucher.setSoLuongDaDung(100); // Equal to soLuongBanDau
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("VALID10")).thenReturn(Optional.of(validVoucher));

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("VALID10", customer, new BigDecimal("100.00"));

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("usage limit exceeded"));
    }

    @Test
    void validateVoucher_PrivateVoucherEligibleCustomer_ShouldReturnValid() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("100.00");
        PhieuGiamGiaNguoiDungId eligibilityId = new PhieuGiamGiaNguoiDungId(privateVoucher.getId(), customer.getId());

        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("PRIVATE20")).thenReturn(Optional.of(privateVoucher));
        when(phieuGiamGiaNguoiDungRepository.existsById(eligibilityId)).thenReturn(true);

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("PRIVATE20", customer, orderTotal);

        // Assert
        assertTrue(result.isValid());
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getDiscountAmount())); // 20% of 100
    }

    @Test
    void validateVoucher_PrivateVoucherIneligibleCustomer_ShouldReturnInvalid() {
        // Arrange
        BigDecimal orderTotal = new BigDecimal("100.00");
        PhieuGiamGiaNguoiDungId eligibilityId = new PhieuGiamGiaNguoiDungId(privateVoucher.getId(), customer.getId());

        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("PRIVATE20")).thenReturn(Optional.of(privateVoucher));
        when(phieuGiamGiaNguoiDungRepository.existsById(eligibilityId)).thenReturn(false);

        // Act
        PhieuGiamGiaService.VoucherValidationResult result = phieuGiamGiaService.validateVoucher("PRIVATE20", customer, orderTotal);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("not eligible for this private voucher"));
    }

    @Test
    void applyVoucherToOrder_ShouldCreateRelationshipAndIncrementUsage() {
        // Arrange
        BigDecimal discountAmount = new BigDecimal("10.00");
        int initialUsage = validVoucher.getSoLuongDaDung();

        // Act
        phieuGiamGiaService.applyVoucherToOrder(validVoucher, hoaDon, discountAmount);

        // Assert
        verify(hoaDonPhieuGiamGiaRepository).save(any(HoaDonPhieuGiamGia.class));
        verify(phieuGiamGiaRepository).save(validVoucher);
        assertEquals(initialUsage + 1, validVoucher.getSoLuongDaDung());
    }

    @Test
    void isCustomerEligible_PublicVoucher_ShouldReturnTrue() {
        // Arrange
        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("VALID10")).thenReturn(Optional.of(validVoucher));

        // Act
        boolean result = phieuGiamGiaService.isCustomerEligible("VALID10", customer.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isCustomerEligible_PrivateVoucherEligibleCustomer_ShouldReturnTrue() {
        // Arrange
        PhieuGiamGiaNguoiDungId eligibilityId = new PhieuGiamGiaNguoiDungId(privateVoucher.getId(), customer.getId());

        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("PRIVATE20")).thenReturn(Optional.of(privateVoucher));
        when(phieuGiamGiaNguoiDungRepository.existsById(eligibilityId)).thenReturn(true);

        // Act
        boolean result = phieuGiamGiaService.isCustomerEligible("PRIVATE20", customer.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isCustomerEligible_PrivateVoucherIneligibleCustomer_ShouldReturnFalse() {
        // Arrange
        PhieuGiamGiaNguoiDungId eligibilityId = new PhieuGiamGiaNguoiDungId(privateVoucher.getId(), customer.getId());

        when(phieuGiamGiaRepository.findByMaPhieuGiamGia("PRIVATE20")).thenReturn(Optional.of(privateVoucher));
        when(phieuGiamGiaNguoiDungRepository.existsById(eligibilityId)).thenReturn(false);

        // Act
        boolean result = phieuGiamGiaService.isCustomerEligible("PRIVATE20", customer.getId());

        // Assert
        assertFalse(result);
    }
}
