package com.lapxpert.backend.common.service;

import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private MinioService minioService;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(minioService);
    }

    @Test
    void buildObjectUrl_WithValidFilename_ReturnsPresignedUrl() throws MinioException {
        // Given
        String bucketName = "avatars";
        String objectName = "test-avatar.jpg";
        String expectedUrl = "https://lapxpert-storage-api.khoalda.dev/avatars/test-avatar.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        
        when(minioService.getPresignedObjectUrl(bucketName, objectName))
            .thenReturn(expectedUrl);

        // When
        String result = urlService.buildObjectUrl(bucketName, objectName);

        // Then
        assertEquals(expectedUrl, result);
        verify(minioService).getPresignedObjectUrl(bucketName, objectName);
    }

    @Test
    void buildObjectUrl_WithEmptyFilename_ReturnsNull() {
        // When
        String result = urlService.buildObjectUrl("avatars", "");

        // Then
        assertNull(result);
        verifyNoInteractions(minioService);
    }

    @Test
    void buildObjectUrl_WithNullFilename_ReturnsNull() {
        // When
        String result = urlService.buildObjectUrl("avatars", null);

        // Then
        assertNull(result);
        verifyNoInteractions(minioService);
    }

    @Test
    void buildObjectUrl_WithMinioException_ReturnsNull() throws MinioException {
        // Given
        String bucketName = "avatars";
        String objectName = "test-avatar.jpg";
        
        when(minioService.getPresignedObjectUrl(bucketName, objectName))
            .thenThrow(new MinioException("MinIO error"));

        // When
        String result = urlService.buildObjectUrl(bucketName, objectName);

        // Then
        assertNull(result);
        verify(minioService).getPresignedObjectUrl(bucketName, objectName);
    }

    @Test
    void buildAvatarUrl_WithValidFilename_ReturnsPresignedUrl() throws MinioException {
        // Given
        String filename = "avatar.jpg";
        String expectedUrl = "https://lapxpert-storage-api.khoalda.dev/avatars/avatar.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        
        when(minioService.getPresignedObjectUrl("avatars", filename))
            .thenReturn(expectedUrl);

        // When
        String result = urlService.buildAvatarUrl(filename);

        // Then
        assertEquals(expectedUrl, result);
        verify(minioService).getPresignedObjectUrl("avatars", filename);
    }

    @Test
    void extractFilenameFromUrl_WithPresignedUrl_ReturnsFilename() {
        // Given
        String presignedUrl = "https://lapxpert-storage-api.khoalda.dev/avatars/test-avatar.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...";
        String expectedFilename = "test-avatar.jpg";

        // When
        String result = urlService.extractFilenameFromUrl(presignedUrl);

        // Then
        assertEquals(expectedFilename, result);
    }

    @Test
    void extractFilenameFromUrl_WithRegularUrl_ReturnsFilename() {
        // Given
        String url = "https://lapxpert-storage-api.khoalda.dev/avatars/test-avatar.jpg";
        String expectedFilename = "test-avatar.jpg";

        // When
        String result = urlService.extractFilenameFromUrl(url);

        // Then
        assertEquals(expectedFilename, result);
    }

    @Test
    void extractFilenameFromUrl_WithFilename_ReturnsFilename() {
        // Given
        String filename = "test-avatar.jpg";

        // When
        String result = urlService.extractFilenameFromUrl(filename);

        // Then
        assertEquals(filename, result);
    }

    @Test
    void extractFilenameFromUrl_WithEmptyString_ReturnsNull() {
        // When
        String result = urlService.extractFilenameFromUrl("");

        // Then
        assertNull(result);
    }

    @Test
    void extractFilenameFromUrl_WithNull_ReturnsNull() {
        // When
        String result = urlService.extractFilenameFromUrl(null);

        // Then
        assertNull(result);
    }

    @Test
    void isFullUrl_WithHttpUrl_ReturnsTrue() {
        // Given
        String url = "https://lapxpert-storage-api.khoalda.dev/avatars/test.jpg";

        // When
        boolean result = urlService.isFullUrl(url);

        // Then
        assertTrue(result);
    }

    @Test
    void isFullUrl_WithFilename_ReturnsFalse() {
        // Given
        String filename = "test-avatar.jpg";

        // When
        boolean result = urlService.isFullUrl(filename);

        // Then
        assertFalse(result);
    }

    @Test
    void ensureAvatarUrl_WithFilename_ReturnsPresignedUrl() throws MinioException {
        // Given
        String filename = "test-avatar.jpg";
        String expectedUrl = "https://lapxpert-storage-api.khoalda.dev/avatars/test-avatar.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        
        when(minioService.getPresignedObjectUrl("avatars", filename))
            .thenReturn(expectedUrl);

        // When
        String result = urlService.ensureAvatarUrl(filename);

        // Then
        assertEquals(expectedUrl, result);
        verify(minioService).getPresignedObjectUrl("avatars", filename);
    }

    @Test
    void ensureAvatarUrl_WithExistingUrl_ReturnsUrl() {
        // Given
        String existingUrl = "https://lapxpert-storage-api.khoalda.dev/avatars/test-avatar.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";

        // When
        String result = urlService.ensureAvatarUrl(existingUrl);

        // Then
        assertEquals(existingUrl, result);
        verifyNoInteractions(minioService);
    }
}
