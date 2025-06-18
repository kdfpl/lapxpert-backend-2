package com.lapxpert.backend.sanpham.service;

import com.lapxpert.backend.sanpham.dto.BatchOperationResult;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import com.lapxpert.backend.sanpham.entity.SerialNumberAuditHistory;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.repository.SerialNumberAuditHistoryRepository;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.sanpham.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for bulk operations on serial numbers including CSV/Excel import/export.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SerialNumberBulkService {

    private final SerialNumberRepository serialNumberRepository;
    private final SerialNumberAuditHistoryRepository auditHistoryRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    // CSV/Excel Import Operations

    /**
     * Import serial numbers from CSV file
     */
    public BatchOperationResult importFromCsv(MultipartFile file, String user) {
        String batchId = "IMPORT-CSV-" + System.currentTimeMillis();
        BatchOperationResult result = new BatchOperationResult();
        result.setBatchId(batchId);
        result.setStartTime(Instant.now());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header row
                }

                try {
                    SerialNumber serialNumber = parseCsvLine(line, batchId);
                    if (serialNumber != null) {
                        SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);
                        result.addSuccess(savedSerialNumber.getSerialNumberValue());

                        // Create audit trail
                        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.importEntry(
                            savedSerialNumber.getId(),
                            batchId,
                            user,
                            "Import từ CSV file"
                        );
                        auditHistoryRepository.save(auditEntry);
                    }
                } catch (Exception e) {
                    result.addError(lineNumber, "Lỗi xử lý dòng " + lineNumber + ": " + e.getMessage());
                    log.warn("Error processing CSV line {}: {}", lineNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            result.addError(0, "Lỗi đọc file CSV: " + e.getMessage());
            log.error("Error reading CSV file", e);
        }

        result.setEndTime(Instant.now());
        log.info("CSV import completed. Batch ID: {}, Success: {}, Errors: {}", 
                batchId, result.getSuccessCount(), result.getErrorCount());

        return result;
    }

    /**
     * Import serial numbers from Excel file
     */
    public BatchOperationResult importFromExcel(MultipartFile file, String user) {
        String batchId = "IMPORT-EXCEL-" + System.currentTimeMillis();
        BatchOperationResult result = new BatchOperationResult();
        result.setBatchId(batchId);
        result.setStartTime(Instant.now());

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowNumber = 0;

            for (Row row : sheet) {
                rowNumber++;
                
                if (rowNumber == 1) {
                    continue; // Skip header row
                }

                try {
                    SerialNumber serialNumber = parseExcelRow(row, batchId);
                    if (serialNumber != null) {
                        SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);
                        result.addSuccess(savedSerialNumber.getSerialNumberValue());

                        // Create audit trail
                        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.importEntry(
                            savedSerialNumber.getId(),
                            batchId,
                            user,
                            "Import từ Excel file"
                        );
                        auditHistoryRepository.save(auditEntry);
                    }
                } catch (Exception e) {
                    result.addError(rowNumber, "Lỗi xử lý dòng " + rowNumber + ": " + e.getMessage());
                    log.warn("Error processing Excel row {}: {}", rowNumber, e.getMessage());
                }
            }
        } catch (IOException e) {
            result.addError(0, "Lỗi đọc file Excel: " + e.getMessage());
            log.error("Error reading Excel file", e);
        }

        result.setEndTime(Instant.now());
        log.info("Excel import completed. Batch ID: {}, Success: {}, Errors: {}", 
                batchId, result.getSuccessCount(), result.getErrorCount());

        return result;
    }

    // CSV/Excel Export Operations

    /**
     * Export serial numbers to CSV
     */
    public ByteArrayOutputStream exportToCsv(List<Long> serialNumberIds) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            // Write CSV header
            writer.println("Serial Number,Product Name,Variant,Status,Batch Number,Supplier,Manufacturing Date,Warranty Expiry,Notes");

            // Write data rows
            List<SerialNumber> serialNumbers = serialNumberRepository.findByIdIn(serialNumberIds);
            for (SerialNumber serialNumber : serialNumbers) {
                writer.println(formatCsvRow(serialNumber));
            }
        }

        log.info("Exported {} serial numbers to CSV", serialNumberIds.size());
        return outputStream;
    }

    /**
     * Export serial numbers to Excel
     */
    public ByteArrayOutputStream exportToExcel(List<Long> serialNumberIds) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Serial Numbers");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Serial Number", "Product Name", "Variant", "Status", "Batch Number", 
                               "Supplier", "Manufacturing Date", "Warranty Expiry", "Notes"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                // Style header
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            List<SerialNumber> serialNumbers = serialNumberRepository.findByIdIn(serialNumberIds);
            int rowNum = 1;
            
            for (SerialNumber serialNumber : serialNumbers) {
                Row row = sheet.createRow(rowNum++);
                populateExcelRow(row, serialNumber, workbook);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error creating Excel export", e);
            throw new RuntimeException("Lỗi tạo file Excel: " + e.getMessage());
        }

        log.info("Exported {} serial numbers to Excel", serialNumberIds.size());
        return outputStream;
    }

    // Bulk Status Operations

    /**
     * Bulk update status of serial numbers
     */
    public BatchOperationResult bulkUpdateStatus(List<Long> serialNumberIds, 
                                                TrangThaiSerialNumber newStatus, 
                                                String user, 
                                                String reason) {
        String batchId = "BULK-STATUS-" + System.currentTimeMillis();
        BatchOperationResult result = new BatchOperationResult();
        result.setBatchId(batchId);
        result.setStartTime(Instant.now());

        for (Long serialNumberId : serialNumberIds) {
            try {
                SerialNumber serialNumber = serialNumberRepository.findById(serialNumberId)
                        .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberId));

                TrangThaiSerialNumber oldStatus = serialNumber.getTrangThai();
                serialNumber.setTrangThai(newStatus);
                serialNumberRepository.save(serialNumber);

                result.addSuccess(serialNumber.getSerialNumberValue());

                // Create audit trail
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.bulkOperationEntry(
                    serialNumber.getId(),
                    "BULK_STATUS_UPDATE",
                    batchId,
                    user,
                    reason != null ? reason : "Cập nhật trạng thái hàng loạt"
                );
                auditEntry.setGiaTriCu("{\"trangThai\":\"" + oldStatus + "\"}");
                auditEntry.setGiaTriMoi("{\"trangThai\":\"" + newStatus + "\"}");
                auditHistoryRepository.save(auditEntry);

            } catch (Exception e) {
                result.addError(serialNumberId.intValue(), "Lỗi cập nhật serial number " + serialNumberId + ": " + e.getMessage());
                log.warn("Error updating serial number {}: {}", serialNumberId, e.getMessage());
            }
        }

        result.setEndTime(Instant.now());
        log.info("Bulk status update completed. Batch ID: {}, Success: {}, Errors: {}", 
                batchId, result.getSuccessCount(), result.getErrorCount());

        return result;
    }

    // Helper Methods

    private SerialNumber parseCsvLine(String line, String batchId) {
        String[] fields = line.split(",");
        if (fields.length < 3) {
            throw new IllegalArgumentException("CSV line must have at least 3 fields");
        }

        String serialNumberValue = fields[0].trim();
        Long variantId = Long.parseLong(fields[1].trim());
        String statusStr = fields[2].trim();

        // Validate serial number doesn't exist
        if (serialNumberRepository.existsBySerialNumberValue(serialNumberValue)) {
            throw new IllegalArgumentException("Serial number already exists: " + serialNumberValue);
        }

        // Get variant
        SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));

        // Parse status
        TrangThaiSerialNumber status;
        try {
            status = TrangThaiSerialNumber.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            status = TrangThaiSerialNumber.AVAILABLE;
        }

        SerialNumber serialNumber = SerialNumber.builder()
                .serialNumberValue(serialNumberValue)
                .sanPhamChiTiet(variant)
                .trangThai(status)
                .importBatchId(batchId)
                .build();

        // Parse optional fields
        if (fields.length > 3 && !fields[3].trim().isEmpty()) {
            serialNumber.setBatchNumber(fields[3].trim());
        }
        if (fields.length > 4 && !fields[4].trim().isEmpty()) {
            serialNumber.setNhaCungCap(fields[4].trim());
        }
        if (fields.length > 5 && !fields[5].trim().isEmpty()) {
            serialNumber.setGhiChu(fields[5].trim());
        }

        return serialNumber;
    }

    private SerialNumber parseExcelRow(Row row, String batchId) {
        if (row.getCell(0) == null || row.getCell(1) == null || row.getCell(2) == null) {
            throw new IllegalArgumentException("Excel row must have at least 3 columns");
        }

        String serialNumberValue = getCellValueAsString(row.getCell(0));
        Long variantId = (long) row.getCell(1).getNumericCellValue();
        String statusStr = getCellValueAsString(row.getCell(2));

        // Validate serial number doesn't exist
        if (serialNumberRepository.existsBySerialNumberValue(serialNumberValue)) {
            throw new IllegalArgumentException("Serial number already exists: " + serialNumberValue);
        }

        // Get variant
        SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found: " + variantId));

        // Parse status
        TrangThaiSerialNumber status;
        try {
            status = TrangThaiSerialNumber.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            status = TrangThaiSerialNumber.AVAILABLE;
        }

        SerialNumber serialNumber = SerialNumber.builder()
                .serialNumberValue(serialNumberValue)
                .sanPhamChiTiet(variant)
                .trangThai(status)
                .importBatchId(batchId)
                .build();

        // Parse optional fields
        if (row.getCell(3) != null) {
            serialNumber.setBatchNumber(getCellValueAsString(row.getCell(3)));
        }
        if (row.getCell(4) != null) {
            serialNumber.setNhaCungCap(getCellValueAsString(row.getCell(4)));
        }
        if (row.getCell(5) != null) {
            serialNumber.setGhiChu(getCellValueAsString(row.getCell(5)));
        }

        return serialNumber;
    }

    private String formatCsvRow(SerialNumber serialNumber) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        return String.join(",",
            escapeCSV(serialNumber.getSerialNumberValue()),
            escapeCSV(serialNumber.getSanPhamChiTiet().getSanPham().getTenSanPham()),
            escapeCSV(getVariantName(serialNumber.getSanPhamChiTiet())),
            escapeCSV(serialNumber.getTrangThai().getDescription()),
            escapeCSV(serialNumber.getBatchNumber() != null ? serialNumber.getBatchNumber() : ""),
            escapeCSV(serialNumber.getNhaCungCap() != null ? serialNumber.getNhaCungCap() : ""),
            serialNumber.getNgaySanXuat() != null ? formatter.format(serialNumber.getNgaySanXuat()) : "",
            serialNumber.getNgayHetBaoHanh() != null ? formatter.format(serialNumber.getNgayHetBaoHanh()) : "",
            escapeCSV(serialNumber.getGhiChu() != null ? serialNumber.getGhiChu() : "")
        );
    }

    private void populateExcelRow(Row row, SerialNumber serialNumber, Workbook workbook) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        row.createCell(0).setCellValue(serialNumber.getSerialNumberValue());
        row.createCell(1).setCellValue(serialNumber.getSanPhamChiTiet().getSanPham().getTenSanPham());
        row.createCell(2).setCellValue(getVariantName(serialNumber.getSanPhamChiTiet()));
        row.createCell(3).setCellValue(serialNumber.getTrangThai().getDescription());
        row.createCell(4).setCellValue(serialNumber.getBatchNumber() != null ? serialNumber.getBatchNumber() : "");
        row.createCell(5).setCellValue(serialNumber.getNhaCungCap() != null ? serialNumber.getNhaCungCap() : "");
        row.createCell(6).setCellValue(serialNumber.getNgaySanXuat() != null ? formatter.format(serialNumber.getNgaySanXuat()) : "");
        row.createCell(7).setCellValue(serialNumber.getNgayHetBaoHanh() != null ? formatter.format(serialNumber.getNgayHetBaoHanh()) : "");
        row.createCell(8).setCellValue(serialNumber.getGhiChu() != null ? serialNumber.getGhiChu() : "");
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getVariantName(SanPhamChiTiet variant) {
        StringBuilder name = new StringBuilder();
        if (variant.getRam() != null) {
            name.append(variant.getRam().getMoTaRam());
        }
        if (variant.getBoNho() != null) {
            if (name.length() > 0) name.append("/");
            name.append(variant.getBoNho().getMoTaBoNho());
        }
        if (variant.getMauSac() != null) {
            if (name.length() > 0) name.append(" - ");
            name.append(variant.getMauSac().getMoTaMauSac());
        }
        return name.toString();
    }
}
