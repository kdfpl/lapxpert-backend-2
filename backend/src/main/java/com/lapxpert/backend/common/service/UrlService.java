package com.lapxpert.backend.common.service;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for handling URL generation and conversion
 * Converts MinIO object names to presigned URLs using MinioService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UrlService {

    private final MinioService minioService;

    /**
     * Convert a MinIO object name to a presigned URL
     * @param bucketName The bucket name (e.g., "avatars", "products")
     * @param objectName The object name/filename
     * @return Presigned URL to access the object, or null if objectName is empty
     */
    public String buildObjectUrl(String bucketName, String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return null;
        }

        try {
            String presignedUrl = minioService.getPresignedObjectUrl(bucketName, objectName);
            log.debug("Generated presigned URL: {} -> {}", objectName, presignedUrl);
            return presignedUrl;
        } catch (MinioException e) {
            log.error("Failed to generate presigned URL for object '{}' in bucket '{}': {}",
                     objectName, bucketName, e.getMessage());
            return null;
        }
    }

    /**
     * Convert an avatar filename to a full URL
     * @param avatarFilename The avatar filename
     * @return Full URL to access the avatar, or null if filename is empty
     */
    public String buildAvatarUrl(String avatarFilename) {
        return buildObjectUrl("avatars", avatarFilename);
    }

    /**
     * Convert a product image filename to a full URL
     * @param imageFilename The product image filename
     * @return Full URL to access the product image, or null if filename is empty
     */
    public String buildProductImageUrl(String imageFilename) {
        return buildObjectUrl("products", imageFilename);
    }

    /**
     * Extract the filename from a presigned URL or regular URL
     * This is useful when receiving full URLs and needing to store just the filename
     * @param fullUrl The full URL (presigned or regular)
     * @return Just the filename part, or the original string if it doesn't match URL pattern
     */
    public String extractFilenameFromUrl(String fullUrl) {
        if (!StringUtils.hasText(fullUrl)) {
            return null;
        }

        // If it's already just a filename (no protocol), return as is
        if (!fullUrl.startsWith("http")) {
            return fullUrl;
        }

        try {
            // For presigned URLs, extract the object name from the path
            // Example: https://domain/bucket/filename?X-Amz-Algorithm=...
            String urlPath = fullUrl.split("\\?")[0]; // Remove query parameters
            String[] pathParts = urlPath.split("/");

            // Find the filename (should be after the bucket name)
            if (pathParts.length >= 3) {
                // For MinIO URLs: https://domain/bucket/filename
                return pathParts[pathParts.length - 1]; // Return the last part (filename)
            }
        } catch (Exception e) {
            log.warn("Failed to extract filename from URL: {}", fullUrl, e);
        }

        return fullUrl; // Return original if extraction fails
    }

    /**
     * Check if a string is already a full URL
     * @param value The string to check
     * @return true if it's a full URL, false if it's just a filename
     */
    public boolean isFullUrl(String value) {
        return StringUtils.hasText(value) && value.startsWith("http");
    }

    /**
     * Convert filename to URL if needed, or return as-is if already a URL
     * @param bucketName The bucket name
     * @param filenameOrUrl The filename or existing URL
     * @return Full URL
     */
    public String ensureFullUrl(String bucketName, String filenameOrUrl) {
        if (!StringUtils.hasText(filenameOrUrl)) {
            return null;
        }

        if (isFullUrl(filenameOrUrl)) {
            return filenameOrUrl; // Already a full URL
        }

        return buildObjectUrl(bucketName, filenameOrUrl);
    }

    /**
     * Convert avatar filename to URL if needed, or return as-is if already a URL
     * @param avatarFilenameOrUrl The avatar filename or existing URL
     * @return Full avatar URL
     */
    public String ensureAvatarUrl(String avatarFilenameOrUrl) {
        return ensureFullUrl("avatars", avatarFilenameOrUrl);
    }
}
