package com.lapxpert.backend.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for DistributedLockService to verify Redisson integration
 * Tests Vietnamese error messages and proper lock behavior
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private DistributedLockService distributedLockService;

    @Test
    void testExecuteWithLock_Success() throws InterruptedException {
        // Arrange
        ReflectionTestUtils.setField(distributedLockService, "defaultWaitTime", 10L);
        ReflectionTestUtils.setField(distributedLockService, "defaultLeaseTime", 30L);
        
        when(redissonClient.getLock("lapxpert:lock:test-key")).thenReturn(rLock);
        when(rLock.tryLock(10L, 30L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // Act
        String result = distributedLockService.executeWithLock("test-key", () -> "success");

        // Assert
        assertEquals("success", result);
        verify(rLock).tryLock(10L, 30L, TimeUnit.SECONDS);
        verify(rLock).unlock();
    }

    @Test
    void testExecuteWithLock_LockAcquisitionFailed() throws InterruptedException {
        // Arrange
        ReflectionTestUtils.setField(distributedLockService, "defaultWaitTime", 10L);
        ReflectionTestUtils.setField(distributedLockService, "defaultLeaseTime", 30L);
        
        when(redissonClient.getLock("lapxpert:lock:test-key")).thenReturn(rLock);
        when(rLock.tryLock(10L, 30L, TimeUnit.SECONDS)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            distributedLockService.executeWithLock("test-key", () -> "success"));
        
        assertTrue(exception.getMessage().contains("Không thể lấy khóa phân tán"));
        verify(rLock, never()).unlock();
    }

    @Test
    void testExecuteWithLock_OperationThrowsException() throws InterruptedException {
        // Arrange
        ReflectionTestUtils.setField(distributedLockService, "defaultWaitTime", 10L);
        ReflectionTestUtils.setField(distributedLockService, "defaultLeaseTime", 30L);
        
        when(redissonClient.getLock("lapxpert:lock:test-key")).thenReturn(rLock);
        when(rLock.tryLock(10L, 30L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            distributedLockService.executeWithLock("test-key", () -> {
                throw new RuntimeException("Test exception");
            }));
        
        assertTrue(exception.getMessage().contains("Lỗi thực hiện thao tác với khóa phân tán"));
        verify(rLock).unlock(); // Lock should still be released
    }

    @Test
    void testIsLocked() {
        // Arrange
        when(redissonClient.getLock("lapxpert:lock:test-key")).thenReturn(rLock);
        when(rLock.isLocked()).thenReturn(true);

        // Act
        boolean result = distributedLockService.isLocked("test-key");

        // Assert
        assertTrue(result);
        verify(rLock).isLocked();
    }

    @Test
    void testGetInventoryLockKey() {
        // Act
        String lockKey = distributedLockService.getInventoryLockKey(123L);

        // Assert
        assertEquals("inventory:variant:123", lockKey);
    }
}
