package com.lapxpert.backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Global exception handler for common exceptions across the application
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle file upload size exceeded exception globally
     * This will catch MaxUploadSizeExceededException from any controller
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Global file upload size exceeded: {}", e.getMessage());
        
        // Extract the actual limits from the exception if possible
        String maxFileSize = "50MB";
        String maxRequestSize = "50MB";
        
        // Try to extract actual limits from exception message
        if (e.getMessage() != null) {
            if (e.getMessage().contains("maximum allowed size")) {
                // Parse the actual limits from the exception message if needed
                log.debug("Exception details: {}", e.getMessage());
            }
        }
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(Map.of(
                "error", "FILE_SIZE_EXCEEDED",
                "message", "File size exceeds maximum allowed limit",
                "details", String.format(
                    "Maximum allowed size is %s per file and %s per request", 
                    maxFileSize, maxRequestSize
                ),
                "maxFileSize", maxFileSize,
                "maxRequestSize", maxRequestSize,
                "timestamp", System.currentTimeMillis()
            ));
    }

    /**
     * Handle general file upload exceptions
     */
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(
            org.springframework.web.multipart.MultipartException e) {
        log.error("Multipart file upload error: {}", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "MULTIPART_ERROR",
                "message", "Error processing multipart file upload",
                "details", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
    }
}
