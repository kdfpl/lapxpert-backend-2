package com.lapxpert.backend.common.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MinioService {
    private static final Logger logger = LoggerFactory.getLogger(MinioService.class);
    private final MinioClient minioClient;

    @Value("${minio.presigned.url.expiry.seconds:3600}") // Default to 1 hour if not set
    private int presignedUrlExpirySeconds;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Uploads multiple files to the specified MinIO bucket.
     *
     * @param bucketName The name of the bucket.
     * @param files      Array of MultipartFile to upload.
     * @return List of unique object names (filenames) for the uploaded files.
     * @throws Exception if any error occurs during upload.
     */
    public List<String> uploadFiles(String bucketName, MultipartFile[] files) throws Exception {
        List<String> objectNames = new ArrayList<>();

        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket '{}' created successfully.", bucketName);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Skipping empty file: {}", file.getOriginalFilename());
                continue;
            }
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown_file";
            String objectName = UUID.randomUUID().toString() + "_"
                    + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
                objectNames.add(objectName);
                logger.info("File '{}' uploaded successfully as '{}' to bucket '{}'.", originalFilename, objectName,
                        bucketName);
            } catch (Exception e) {
                logger.error("Error uploading file '{}' as '{}' to bucket '{}': {}", originalFilename, objectName,
                        bucketName, e.getMessage(), e);
                // Optionally, rethrow a custom exception or handle partial uploads
                throw new MinioException("Failed to upload file: " + originalFilename + ". " + e.getMessage());
            }
        }
        return objectNames;
    }

    /**
     * Generates a pre-signed URL for accessing an object in MinIO.
     *
     * @param bucketName The name of the bucket.
     * @param objectName The name of the object.
     * @return The pre-signed URL as a String.
     * @throws MinioException if an error occurs during URL generation.
     */
    public String getPresignedObjectUrl(String bucketName, String objectName) throws MinioException {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedUrlExpirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            logger.error("Error generating presigned URL for object '{}' in bucket '{}': {}", objectName, bucketName,
                    e.getMessage(), e);
            throw new MinioException("Could not generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Generates pre-signed URLs for a list of objects in MinIO.
     *
     * @param bucketName  The name of the bucket.
     * @param objectNames List of object names.
     * @return List of pre-signed URLs.
     */
    public List<String> getPresignedObjectUrls(String bucketName, List<String> objectNames) {
        return objectNames.stream()
                .map(objectName -> {
                    try {
                        return getPresignedObjectUrl(bucketName, objectName);
                    } catch (MinioException e) {
                        // Log and return null or a placeholder, or collect errors
                        logger.warn("Failed to generate presigned URL for object '{}': {}", objectName, e.getMessage());
                        return null; // Or some error indicator
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an object from the specified MinIO bucket.
     *
     * @param bucketName The name of the bucket.
     * @param objectName The name of the object to delete.
     * @throws MinioException if an error occurs during deletion.
     */
    public void deleteObject(String bucketName, String objectName) throws MinioException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            logger.info("Object '{}' deleted successfully from bucket '{}'.", objectName, bucketName);
        } catch (Exception e) {
            logger.error("Error deleting object '{}' from bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new MinioException("Could not delete object: " + e.getMessage());
        }
    }

    /**
     * Deletes multiple objects from the specified MinIO bucket.
     *
     * @param bucketName  The name of the bucket.
     * @param objectNames List of object names to delete.
     * @throws MinioException if an error occurs during deletion or if any object
     *                        fails to delete.
     */
    public void deleteObjects(String bucketName, List<String> objectNames) throws MinioException { // <--- ADDED "throws
                                                                                                   // MinioException"
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }
        List<DeleteObject> objectsToDelete = new LinkedList<>();
        for (String objectName : objectNames) {
            objectsToDelete.add(new DeleteObject(objectName));
        }

        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder().bucket(bucketName).objects(objectsToDelete).build());
            // Check for errors and potentially collect them or throw a summary exception
            List<String> errorMessages = new ArrayList<>();
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get(); // This call can throw exceptions
                errorMessages.add("Failed to delete object '" + error.objectName() + "': " + error.message());
                logger.warn("Error deleting object '{}': {}", error.objectName(), error.message());
            }
            if (!errorMessages.isEmpty()) {
                // Consolidate error messages and throw a single MinioException
                throw new MinioException("Errors occurred during bulk deletion: " + String.join("; ", errorMessages));
            }
            logger.info("Successfully attempted to delete {} objects from bucket '{}'.", objectNames.size(),
                    bucketName);
        } catch (Exception e) { // Catch generic Exception from minioClient or result.get()
            logger.error("Critical error during bulk deletion from bucket '{}': {}", bucketName, e.getMessage(), e);
            // Rethrow as MinioException to ensure the caller handles it
            throw new MinioException("Critical error during bulk object deletion: " + e.getMessage());
        }
    }
}