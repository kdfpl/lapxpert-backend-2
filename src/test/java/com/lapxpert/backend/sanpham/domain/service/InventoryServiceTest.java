package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonChiTietDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService to verify proper inventory management.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private SanPhamChiTiet availableItem1;
    private SanPhamChiTiet availableItem2;
    private HoaDonChiTietDto orderItem;

    @BeforeEach
    void setUp() {
        // Create test product items
        availableItem1 = new SanPhamChiTiet();
        availableItem1.setId(1L);
        availableItem1.setSerialNumber("LAPTOP001");
        availableItem1.setTrangThai(TrangThaiSanPham.AVAILABLE);
        availableItem1.setGiaBan(new BigDecimal("1000.00"));

        availableItem2 = new SanPhamChiTiet();
        availableItem2.setId(2L);
        availableItem2.setSerialNumber("LAPTOP002");
        availableItem2.setTrangThai(TrangThaiSanPham.AVAILABLE);
        availableItem2.setGiaBan(new BigDecimal("1000.00"));

        // Create test order item
        orderItem = new HoaDonChiTietDto();
        orderItem.setSanPhamChiTietId(100L); // Product variant ID
        orderItem.setSoLuong(2);
    }

    @Test
    void testReserveItems_Success() {
        // Arrange
        List<HoaDonChiTietDto> orderItems = Arrays.asList(orderItem);
        List<SanPhamChiTiet> availableItems = Arrays.asList(availableItem1, availableItem2);

        when(sanPhamChiTietRepository.findAvailableItemsByProductVariant(100L, 2))
            .thenReturn(availableItems);

        // Act
        List<Long> reservedIds = inventoryService.reserveItems(orderItems);

        // Assert
        assertEquals(2, reservedIds.size());
        assertTrue(reservedIds.contains(1L));
        assertTrue(reservedIds.contains(2L));

        // Verify items were marked as reserved
        verify(sanPhamChiTietRepository, times(2)).save(any(SanPhamChiTiet.class));
        assertEquals(TrangThaiSanPham.RESERVED, availableItem1.getTrangThai());
        assertEquals(TrangThaiSanPham.RESERVED, availableItem2.getTrangThai());
    }

    @Test
    void testReserveItems_InsufficientInventory() {
        // Arrange
        List<HoaDonChiTietDto> orderItems = Arrays.asList(orderItem);
        List<SanPhamChiTiet> availableItems = Arrays.asList(availableItem1); // Only 1 available, need 2

        when(sanPhamChiTietRepository.findAvailableItemsByProductVariant(100L, 2))
            .thenReturn(availableItems);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> inventoryService.reserveItems(orderItems)
        );

        assertTrue(exception.getMessage().contains("Insufficient inventory"));
        assertTrue(exception.getMessage().contains("Requested: 2, Available: 1"));
    }

    @Test
    void testReleaseReservation_Success() {
        // Arrange
        List<Long> reservedIds = Arrays.asList(1L, 2L);

        when(sanPhamChiTietRepository.findById(1L)).thenReturn(Optional.of(availableItem1));
        when(sanPhamChiTietRepository.findById(2L)).thenReturn(Optional.of(availableItem2));

        // Set items as reserved
        availableItem1.setTrangThai(TrangThaiSanPham.RESERVED);
        availableItem2.setTrangThai(TrangThaiSanPham.RESERVED);

        // Act
        inventoryService.releaseReservation(reservedIds);

        // Assert
        assertEquals(TrangThaiSanPham.AVAILABLE, availableItem1.getTrangThai());
        assertEquals(TrangThaiSanPham.AVAILABLE, availableItem2.getTrangThai());
        verify(sanPhamChiTietRepository, times(2)).save(any(SanPhamChiTiet.class));
    }

    @Test
    void testIsInventoryAvailable_True() {
        // Arrange
        List<HoaDonChiTietDto> orderItems = Arrays.asList(orderItem);
        when(sanPhamChiTietRepository.countAvailableItemsByProductVariant(100L)).thenReturn(5);

        // Act
        boolean isAvailable = inventoryService.isInventoryAvailable(orderItems);

        // Assert
        assertTrue(isAvailable);
    }

    @Test
    void testIsInventoryAvailable_False() {
        // Arrange
        List<HoaDonChiTietDto> orderItems = Arrays.asList(orderItem);
        when(sanPhamChiTietRepository.countAvailableItemsByProductVariant(100L)).thenReturn(1); // Need 2, only 1 available

        // Act
        boolean isAvailable = inventoryService.isInventoryAvailable(orderItems);

        // Assert
        assertFalse(isAvailable);
    }

    @Test
    void testGetAvailableQuantity() {
        // Arrange
        when(sanPhamChiTietRepository.countAvailableItemsByProductVariant(100L)).thenReturn(3);

        // Act
        int quantity = inventoryService.getAvailableQuantity(100L);

        // Assert
        assertEquals(3, quantity);
    }

    @Test
    void testConfirmSale() {
        // Arrange
        List<Long> reservedIds = Arrays.asList(1L, 2L);
        availableItem1.setTrangThai(TrangThaiSanPham.RESERVED);
        availableItem2.setTrangThai(TrangThaiSanPham.RESERVED);

        when(sanPhamChiTietRepository.findById(1L)).thenReturn(Optional.of(availableItem1));
        when(sanPhamChiTietRepository.findById(2L)).thenReturn(Optional.of(availableItem2));

        // Act
        assertDoesNotThrow(() -> inventoryService.confirmSale(reservedIds));

        // Assert - items should be marked as sold
        assertEquals(TrangThaiSanPham.SOLD, availableItem1.getTrangThai());
        assertEquals(TrangThaiSanPham.SOLD, availableItem2.getTrangThai());
        verify(sanPhamChiTietRepository, times(2)).findById(anyLong());
        verify(sanPhamChiTietRepository, times(2)).save(any(SanPhamChiTiet.class));
    }
}
