package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.hoadon.dto.HoaDonChiTietDto;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import com.lapxpert.backend.sanpham.entity.SerialNumberAuditHistory;

import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.repository.SerialNumberAuditHistoryRepository;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.common.service.DistributedLockService;
import com.lapxpert.backend.common.service.OptimisticLockingService;
import com.lapxpert.backend.sanpham.service.SerialNumberService;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify the fix for inventory reservation issue where multiple items
 * of the same variant were processed separately, causing inventory conflicts.
 */
@ExtendWith(MockitoExtension.class)
class SerialNumberServiceInventoryFixTest {

    @Mock
    private SerialNumberRepository serialNumberRepository;

    @Mock
    private SerialNumberAuditHistoryRepository auditHistoryRepository;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private OptimisticLockingService optimisticLockingService;

    @Mock
    private Logger log;

    @InjectMocks
    private SerialNumberService serialNumberService;

    private List<HoaDonChiTietDto> testOrderItems;
    private List<SerialNumber> mockAvailableSerialNumbers;

    @BeforeEach
    void setUp() {
        // Create test order items - 2 items of the same variant (ID 6)
        testOrderItems = Arrays.asList(
            createOrderItem(6L, 1, 8550000L, null),
            createOrderItem(6L, 1, 9025000L, null)
        );

        // Create mock available serial numbers
        mockAvailableSerialNumbers = Arrays.asList(
            createAvailableSerialNumber(1L, 6L, "SN001"),
            createAvailableSerialNumber(2L, 6L, "SN002")
        );
    }

    @Test
    void testReserveItemsWithTracking_MultipleItemsSameVariant_ShouldAggregateQuantities() {
        // Arrange
        String channel = "ORDER";
        String orderId = "HD005305001";
        String user = "test-user";

        // Mock distributed lock execution
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), any(Runnable.class));

        // Mock optimistic locking execution
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(optimisticLockingService).executeWithRetry(any(Runnable.class));

        // Mock the findAvailableByVariant method to return available serial numbers
        // This should be called ONCE with total quantity 2, not twice with quantity 1
        when(serialNumberRepository.findAvailableByVariant(eq(6L), any(Pageable.class)))
            .thenReturn(mockAvailableSerialNumbers);

        // Mock save operations
        when(serialNumberRepository.save(any(SerialNumber.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditHistoryRepository.save(any(SerialNumberAuditHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Long> result = serialNumberService.reserveItemsWithTracking(
            testOrderItems, channel, orderId, user
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify that findAvailableByVariant was called ONCE with the variant ID
        // This proves the fix is working - quantities are aggregated
        verify(serialNumberRepository, times(1))
            .findAvailableByVariant(eq(6L), any(Pageable.class));

        // Verify that save was called for each serial number
        verify(serialNumberRepository, times(2)).save(any(SerialNumber.class));

        // Verify audit trail creation
        verify(auditHistoryRepository, times(2)).save(any(SerialNumberAuditHistory.class));
    }

    @Test
    void testReserveItemsWithTracking_SingleItem_ShouldWorkAsExpected() {
        // Arrange
        List<HoaDonChiTietDto> singleItem = Arrays.asList(
            createOrderItem(6L, 1, 8550000L, null)
        );

        String channel = "ORDER";
        String orderId = "HD005305002";
        String user = "test-user";

        // Mock distributed lock execution
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(distributedLockService).executeWithLock(anyString(), any(Runnable.class));

        // Mock optimistic locking execution
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(optimisticLockingService).executeWithRetry(any(Runnable.class));

        // Mock the findAvailableByVariant method
        when(serialNumberRepository.findAvailableByVariant(eq(6L), any(Pageable.class)))
            .thenReturn(Arrays.asList(mockAvailableSerialNumbers.get(0)));

        // Mock save operations
        when(serialNumberRepository.save(any(SerialNumber.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditHistoryRepository.save(any(SerialNumberAuditHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Long> result = serialNumberService.reserveItemsWithTracking(
            singleItem, channel, orderId, user
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify that findAvailableByVariant was called once
        verify(serialNumberRepository, times(1))
            .findAvailableByVariant(eq(6L), any(Pageable.class));
    }

    private HoaDonChiTietDto createOrderItem(Long variantId, Integer quantity, Long price, Long serialNumberId) {
        HoaDonChiTietDto item = new HoaDonChiTietDto();
        item.setSanPhamChiTietId(variantId);
        item.setSoLuong(quantity);
        item.setGiaBan(java.math.BigDecimal.valueOf(price));
        item.setSerialNumberId(serialNumberId);
        return item;
    }

    private SerialNumber createAvailableSerialNumber(Long id, Long variantId, String serialValue) {
        SerialNumber serialNumber = new SerialNumber();
        serialNumber.setId(id);
        serialNumber.setSerialNumberValue(serialValue);
        serialNumber.setTrangThai(TrangThaiSerialNumber.AVAILABLE);
        return serialNumber;
    }
}
