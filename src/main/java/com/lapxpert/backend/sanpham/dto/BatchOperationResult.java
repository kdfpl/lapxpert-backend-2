package com.lapxpert.backend.sanpham.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for batch operation results
 * Provides detailed information about batch operation outcomes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {

    /**
     * Unique batch ID for tracking
     */
    private String batchId;

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
     * Start time of the operation
     */
    private Instant startTime;

    /**
     * End time of the operation
     */
    private Instant endTime;

    /**
     * List of successfully processed items
     */
    private List<String> successItems = new ArrayList<>();

    /**
     * List of errors encountered
     */
    private List<String> errors = new ArrayList<>();
    
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

    // Helper methods for serial number operations

    /**
     * Add an error with line number
     */
    public void addError(int lineNumber, String error) {
        errors.add("Line " + lineNumber + ": " + error);
        failureCount++;
        updateTotals();
    }

    /**
     * Add a success item
     */
    public void addSuccess(String item) {
        successItems.add(item);
        successCount++;
        updateTotals();
    }

    /**
     * Add a generic error
     */
    public void addError(String error) {
        errors.add(error);
        failureCount++;
        updateTotals();
    }

    /**
     * Update calculated fields
     */
    private void updateTotals() {
        totalCount = successCount + failureCount;
        success = failureCount == 0;
    }

    /**
     * Get error count
     */
    public int getErrorCount() {
        return failureCount;
    }

    /**
     * Get duration in milliseconds
     */
    public long getDurationMs() {
        if (startTime != null && endTime != null) {
            return endTime.toEpochMilli() - startTime.toEpochMilli();
        }
        return 0;
    }

    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (totalCount == 0) return 0.0;
        return (double) successCount / totalCount * 100.0;
    }
}
