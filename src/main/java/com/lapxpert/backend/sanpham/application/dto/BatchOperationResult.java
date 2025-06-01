package com.lapxpert.backend.sanpham.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for batch operation results
 * Provides detailed information about batch operation outcomes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {
    
    /**
     * Type of operation performed
     */
    private String operationType;
    
    /**
     * Number of items successfully processed
     */
    private int successCount;
    
    /**
     * Number of items that failed to process
     */
    private int failureCount;
    
    /**
     * Total number of items processed
     */
    private int totalCount;
    
    /**
     * Human-readable message describing the result
     */
    private String message;
    
    /**
     * Whether the operation was completely successful
     */
    private boolean success;
    
    /**
     * Constructor for creating result with calculated totals
     */
    public BatchOperationResult(String operationType, int successCount, int failureCount, String message) {
        this.operationType = operationType;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.totalCount = successCount + failureCount;
        this.message = message;
        this.success = failureCount == 0;
    }
}
