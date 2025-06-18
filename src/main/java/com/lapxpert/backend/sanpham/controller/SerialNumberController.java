package com.lapxpert.backend.sanpham.controller;

import com.lapxpert.backend.common.response.ApiResponse;
import com.lapxpert.backend.sanpham.dto.BatchOperationResult;
import com.lapxpert.backend.sanpham.dto.SerialNumberDto;
import com.lapxpert.backend.sanpham.dto.SerialNumberAuditHistoryDto;
import com.lapxpert.backend.sanpham.mapper.SerialNumberMapper;
import com.lapxpert.backend.sanpham.mapper.SerialNumberAuditHistoryMapper;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import com.lapxpert.backend.sanpham.entity.SerialNumberAuditHistory;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.service.SerialNumberService;
import com.lapxpert.backend.sanpham.service.SerialNumberBulkService;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.sanpham.repository.SerialNumberAuditHistoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Serial Number management.
 * Provides comprehensive API for serial number lifecycle management.
 */
@RestController
@RequestMapping("/api/v1/serial-numbers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SerialNumberController {

    private final SerialNumberService serialNumberService;
    private final SerialNumberBulkService serialNumberBulkService;
    private final SerialNumberRepository serialNumberRepository;
    private final SerialNumberAuditHistoryRepository auditHistoryRepository;
    private final SerialNumberMapper serialNumberMapper;
    private final SerialNumberAuditHistoryMapper auditHistoryMapper;

    // CRUD Operations

    /**
     * Get all serial numbers with pagination and filtering
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<SerialNumberDto>>> getAllSerialNumbers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) TrangThaiSerialNumber status,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String supplier) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SerialNumber> serialNumbers = serialNumberRepository.searchSerialNumbers(
            serialNumber, status, variantId, batchNumber, supplier, pageable
        );
        
        Page<SerialNumberDto> serialNumberDtos = serialNumbers.map(serialNumberMapper::toDto);
        
        return ResponseEntity.ok(ApiResponse.success(serialNumberDtos));
    }

    /**
     * Get serial number by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<SerialNumberDto>> getSerialNumberById(@PathVariable Long id) {
        SerialNumber serialNumber = serialNumberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serial number not found"));
        
        SerialNumberDto serialNumberDto = serialNumberMapper.toDto(serialNumber);
        return ResponseEntity.ok(ApiResponse.success(serialNumberDto));
    }

    /**
     * Get serial number by value
     */
    @GetMapping("/by-value/{serialNumberValue}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<SerialNumberDto>> getSerialNumberByValue(@PathVariable String serialNumberValue) {
        SerialNumber serialNumber = serialNumberRepository.findBySerialNumberValue(serialNumberValue)
                .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberValue));
        
        SerialNumberDto serialNumberDto = serialNumberMapper.toDto(serialNumber);
        return ResponseEntity.ok(ApiResponse.success(serialNumberDto));
    }

    /**
     * Create new serial number
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<SerialNumberDto>> createSerialNumber(
            @Valid @RequestBody SerialNumberDto serialNumberDto,
            Principal principal) {
        
        SerialNumber serialNumber = serialNumberMapper.toEntity(serialNumberDto);
        SerialNumber savedSerialNumber = serialNumberService.createSerialNumber(
            serialNumber, 
            principal.getName(), 
            "Tạo serial number mới qua API"
        );
        
        SerialNumberDto responseDto = serialNumberMapper.toDto(savedSerialNumber);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    /**
     * Update serial number
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<SerialNumberDto>> updateSerialNumber(
            @PathVariable Long id,
            @Valid @RequestBody SerialNumberDto serialNumberDto,
            Principal principal) {
        
        SerialNumber updatedSerialNumber = serialNumberMapper.toEntity(serialNumberDto);
        SerialNumber savedSerialNumber = serialNumberService.updateSerialNumber(
            id, 
            updatedSerialNumber, 
            principal.getName(), 
            "Cập nhật serial number qua API"
        );
        
        SerialNumberDto responseDto = serialNumberMapper.toDto(savedSerialNumber);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    /**
     * Delete serial number (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> deleteSerialNumber(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Principal principal) {

        serialNumberService.deleteSerialNumber(
            id,
            principal.getName(),
            reason != null ? reason : "Xóa serial number qua API"
        );

        return ResponseEntity.ok(ApiResponse.success("Đã xóa serial number thành công"));
    }

    /**
     * Change serial number status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<SerialNumberDto>> changeStatus(
            @PathVariable Long id,
            @RequestParam TrangThaiSerialNumber newStatus,
            @RequestParam(required = false) String reason,
            Principal principal) {
        
        SerialNumber updatedSerialNumber = serialNumberService.changeStatus(
            id, 
            newStatus, 
            principal.getName(), 
            reason
        );
        
        SerialNumberDto responseDto = serialNumberMapper.toDto(updatedSerialNumber);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    // Inventory Management

    /**
     * Reserve serial numbers for an order
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> reserveSerialNumbers(
            @RequestParam Long variantId,
            @RequestParam int quantity,
            @RequestParam String channel,
            @RequestParam String orderId,
            Principal principal) {
        
        List<SerialNumber> reservedSerialNumbers = serialNumberService.reserveSerialNumbers(
            variantId, quantity, channel, orderId, principal.getName()
        );
        
        List<SerialNumberDto> responseDto = serialNumberMapper.toOrderDtoList(reservedSerialNumbers);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    /**
     * Confirm sale of reserved serial numbers
     */
    @PostMapping("/confirm-sale")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> confirmSale(
            @RequestBody List<Long> serialNumberIds,
            @RequestParam String orderId,
            Principal principal) {
        
        serialNumberService.confirmSale(serialNumberIds, orderId, principal.getName());
        
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận bán " + serialNumberIds.size() + " serial numbers"));
    }

    /**
     * Release reservations
     */
    @PostMapping("/release-reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> releaseReservations(
            @RequestBody List<Long> serialNumberIds,
            @RequestParam(required = false) String reason,
            Principal principal) {
        
        serialNumberService.releaseReservations(serialNumberIds, principal.getName(), reason);
        
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đặt trước " + serialNumberIds.size() + " serial numbers"));
    }

    // Bulk Operations

    /**
     * Generate serial numbers for a product variant
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> generateSerialNumbers(
            @RequestParam Long variantId,
            @RequestParam int quantity,
            @RequestParam String pattern,
            Principal principal) {
        
        List<SerialNumber> generatedSerialNumbers = serialNumberService.generateSerialNumbers(
            variantId, quantity, pattern, principal.getName()
        );
        
        List<SerialNumberDto> responseDto = serialNumberMapper.toBulkDtoList(generatedSerialNumbers);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    /**
     * Bulk update status
     */
    @PostMapping("/bulk-update-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResult>> bulkUpdateStatus(
            @RequestBody List<Long> serialNumberIds,
            @RequestParam TrangThaiSerialNumber newStatus,
            @RequestParam(required = false) String reason,
            Principal principal) {
        
        BatchOperationResult result = serialNumberBulkService.bulkUpdateStatus(
            serialNumberIds, newStatus, principal.getName(), reason
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Import/Export Operations

    /**
     * Import serial numbers from CSV
     */
    @PostMapping("/import/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResult>> importFromCsv(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        BatchOperationResult result = serialNumberBulkService.importFromCsv(file, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Import serial numbers from Excel
     */
    @PostMapping("/import/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<BatchOperationResult>> importFromExcel(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        BatchOperationResult result = serialNumberBulkService.importFromExcel(file, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Export serial numbers to CSV
     */
    @PostMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<byte[]> exportToCsv(@RequestBody List<Long> serialNumberIds) {
        ByteArrayOutputStream outputStream = serialNumberBulkService.exportToCsv(serialNumberIds);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "serial-numbers.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    /**
     * Export serial numbers to Excel
     */
    @PostMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<byte[]> exportToExcel(@RequestBody List<Long> serialNumberIds) {
        ByteArrayOutputStream outputStream = serialNumberBulkService.exportToExcel(serialNumberIds);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "serial-numbers.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    // Audit History Operations

    /**
     * Get audit history for a serial number
     */
    @GetMapping("/{id}/audit-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<SerialNumberAuditHistoryDto>>> getAuditHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("thoiGianThayDoi").descending());
        Page<SerialNumberAuditHistory> auditHistory = auditHistoryRepository.findBySerialNumberId(id, pageable);

        Page<SerialNumberAuditHistoryDto> auditHistoryDtos = auditHistory.map(auditHistoryMapper::toDto);
        return ResponseEntity.ok(ApiResponse.success(auditHistoryDtos));
    }

    /**
     * Get audit timeline for a serial number
     */
    @GetMapping("/{id}/audit-timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<SerialNumberAuditHistoryDto.TimelineEntry>>> getAuditTimeline(
            @PathVariable Long id) {

        List<SerialNumberAuditHistory> auditHistory = auditHistoryRepository
                .findBySerialNumberIdOrderByThoiGianThayDoiDesc(id);

        List<SerialNumberAuditHistoryDto> auditHistoryDtos = auditHistoryMapper.toTimelineDtoList(auditHistory);
        List<SerialNumberAuditHistoryDto.TimelineEntry> timeline = auditHistoryMapper.toTimelineEntryList(auditHistoryDtos);

        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    // Statistics and Reporting

    /**
     * Get inventory statistics by status
     */
    @GetMapping("/statistics/by-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInventoryStatsByStatus() {
        List<Object[]> stats = serialNumberRepository.getInventoryStatsByStatus();

        List<Map<String, Object>> result = stats.stream()
                .map(row -> Map.of(
                    "status", row[0],
                    "count", row[1],
                    "statusDisplay", ((TrangThaiSerialNumber) row[0]).getDescription()
                ))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get low stock variants
     */
    @GetMapping("/statistics/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLowStockVariants(
            @RequestParam(defaultValue = "5") int threshold) {

        List<Object[]> lowStockVariants = serialNumberRepository.findLowStockVariants(threshold);

        List<Map<String, Object>> result = lowStockVariants.stream()
                .map(row -> Map.of(
                    "variantId", row[0],
                    "productName", row[1],
                    "availableCount", row[2]
                ))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get warranty expiring soon
     */
    @GetMapping("/statistics/warranty-expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> getWarrantyExpiringSoon(
            @RequestParam(defaultValue = "30") int days) {

        Instant startDate = Instant.now();
        Instant endDate = startDate.plus(days, ChronoUnit.DAYS);

        List<SerialNumber> expiringSoon = serialNumberRepository.findWarrantyExpiringSoon(startDate, endDate);
        List<SerialNumberDto> result = serialNumberMapper.toWarrantyDtoList(expiringSoon);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get all serial numbers for a variant (for management purposes)
     */
    @GetMapping("/variant/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> getSerialNumbersByVariant(
            @PathVariable Long variantId) {

        List<SerialNumber> serialNumbers = serialNumberRepository.findBySanPhamChiTietId(variantId);
        List<SerialNumberDto> result = serialNumberMapper.toDtoList(serialNumbers);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get available serial numbers for a variant
     */
    @GetMapping("/available/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> getAvailableSerialNumbers(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "10") int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<SerialNumber> availableSerialNumbers = serialNumberRepository.findAvailableByVariant(variantId, pageable);

        List<SerialNumberDto> result = serialNumberMapper.toInventoryDtoList(availableSerialNumbers);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get serial numbers for a specific order
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<List<SerialNumberDto>>> getSerialNumbersByOrder(@PathVariable String orderId) {
        List<SerialNumber> serialNumbers = serialNumberRepository.findByDonHangDatTruoc(orderId);
        List<SerialNumberDto> result = serialNumberMapper.toDtoList(serialNumbers);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Check serial number availability
     */
    @GetMapping("/check-availability/{serialNumberValue}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAvailability(@PathVariable String serialNumberValue) {
        boolean exists = serialNumberRepository.existsBySerialNumberValue(serialNumberValue);

        Map<String, Object> result = Map.of(
            "exists", exists,
            "available", false
        );

        if (exists) {
            SerialNumber serialNumber = serialNumberRepository.findBySerialNumberValue(serialNumberValue).get();
            result = Map.of(
                "exists", true,
                "available", serialNumber.isAvailable(),
                "status", serialNumber.getTrangThai(),
                "statusDisplay", serialNumber.getTrangThai().getDescription()
            );
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
