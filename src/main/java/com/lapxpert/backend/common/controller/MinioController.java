package com.lapxpert.backend.common.controller;

import com.lapxpert.backend.common.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@CrossOrigin(origins = "*")
@Slf4j
public class MinioController {
    private final MinioService minioService;

    public MinioController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<String>> upload(
            @RequestParam("bucket") String bucket,
            @RequestParam("files") MultipartFile[] files) {

        log.info("File upload request - Bucket: {}, Files count: {}", bucket, files != null ? files.length : 0);

        if (bucket == null || bucket.isBlank()) {
            log.warn("Upload failed: Bucket name is required");
            return ResponseEntity.badRequest().body(Collections.singletonList("Bucket name is required"));
        }
        if (files == null || files.length == 0) {
            log.warn("Upload failed: No files provided");
            return ResponseEntity.badRequest().body(Collections.singletonList("No files provided for upload"));
        }

        // Log file sizes for debugging
        for (MultipartFile file : files) {
            log.debug("Uploading file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
        }

        try {
            List<String> objectNames = minioService.uploadFiles(bucket, files);
            log.info("Successfully uploaded {} files to bucket '{}'", objectNames.size(), bucket);
            return ResponseEntity.ok(objectNames);
        } catch (Exception e) {
            log.error("Failed to upload files to bucket '{}': {}", bucket, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonList("Failed to upload files: " + e.getMessage()));
        }
    }

    /**
     * Get presigned URL for accessing a file
     */
    @GetMapping("/url")
    public ResponseEntity<String> getPresignedUrl(
            @RequestParam("bucket") String bucket,
            @RequestParam("objectName") String objectName) {

        log.info("Presigned URL request - Bucket: {}, Object: {}", bucket, objectName);

        if (bucket == null || bucket.isBlank()) {
            log.warn("Presigned URL failed: Bucket name is required");
            return ResponseEntity.badRequest().body("Bucket name is required");
        }
        if (objectName == null || objectName.isBlank()) {
            log.warn("Presigned URL failed: Object name is required");
            return ResponseEntity.badRequest().body("Object name is required");
        }

        try {
            String presignedUrl = minioService.getPresignedObjectUrl(bucket, objectName);
            log.info("Successfully generated presigned URL for object '{}' in bucket '{}'", objectName, bucket);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object '{}' in bucket '{}': {}", objectName, bucket, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    /**
     * Handle file upload size exceeded exception
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("File upload size exceeded: {}", e.getMessage());

        String message = String.format(
            "File size exceeds maximum allowed limit. Maximum allowed size is %s per file and %s per request.",
            "50MB", "50MB"
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of(
                "error", "FILE_SIZE_EXCEEDED",
                "message", message,
                "maxFileSize", "50MB",
                "maxRequestSize", "50MB"
            ));
    }
}
