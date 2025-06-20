package com.lapxpert.backend.sanpham.service;

import com.lapxpert.backend.hoadon.dto.HoaDonChiTietDto;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import com.lapxpert.backend.sanpham.entity.SerialNumberAuditHistory;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import com.lapxpert.backend.sanpham.repository.SerialNumberAuditHistoryRepository;
import com.lapxpert.backend.sanpham.repository.SerialNumberRepository;
import com.lapxpert.backend.sanpham.repository.SanPhamChiTietRepository;
import com.lapxpert.backend.common.service.DistributedLockService;
import com.lapxpert.backend.common.service.OptimisticLockingService;
import com.lapxpert.backend.common.event.InventoryUpdateEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing serial numbers and inventory tracking.
 * Provides comprehensive serial number lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SerialNumberService {

    private final SerialNumberRepository serialNumberRepository;
    private final SerialNumberAuditHistoryRepository auditHistoryRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final DistributedLockService distributedLockService;
    private final OptimisticLockingService optimisticLockingService;
    private final ApplicationEventPublisher eventPublisher;

    // Serial Number CRUD Operations

    /**
     * Create a new serial number
     */
    public SerialNumber createSerialNumber(SerialNumber serialNumber, String user, String reason) {
        // Validate serial number doesn't exist
        if (serialNumberRepository.existsBySerialNumberValue(serialNumber.getSerialNumberValue())) {
            throw new IllegalArgumentException("Serial number already exists: " + serialNumber.getSerialNumberValue());
        }

        // Set default status if not provided
        if (serialNumber.getTrangThai() == null) {
            serialNumber.setTrangThai(TrangThaiSerialNumber.AVAILABLE);
        }

        // Save serial number
        SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);

        // Create audit trail
        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.createEntry(
            savedSerialNumber.getId(),
            buildAuditJson(savedSerialNumber),
            user,
            reason != null ? reason : "Tạo serial number mới"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Created serial number: {} for variant: {}",
                savedSerialNumber.getSerialNumberValue(),
                savedSerialNumber.getSanPhamChiTiet().getId());

        // Publish inventory update event for WebSocket notifications
        try {
            SanPhamChiTiet variant = savedSerialNumber.getSanPhamChiTiet();
            Long variantId = variant.getId();

            int newAvailableQuantity = getAvailableQuantityByVariant(variantId);
            int oldAvailableQuantity = newAvailableQuantity - 1;

            InventoryUpdateEvent event = InventoryUpdateEvent.builder()
                    .variantId(variantId)
                    .sku(variant.getSku())
                    .tenSanPham(variant.getSanPham().getTenSanPham())
                    .soLuongTonKhoCu(oldAvailableQuantity)
                    .soLuongTonKhoMoi(newAvailableQuantity)
                    .loaiThayDoi("CREATED")
                    .nguoiThucHien(user)
                    .lyDoThayDoi("Tạo serial number mới: " + savedSerialNumber.getSerialNumberValue())
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("Published inventory update event for variant {} serial number creation", variantId);
        } catch (Exception e) {
            log.error("Failed to publish inventory update event for serial number creation: {}", e.getMessage(), e);
        }

        return savedSerialNumber;
    }

    /**
     * Update serial number
     */
    public SerialNumber updateSerialNumber(Long id, SerialNumber updatedSerialNumber, String user, String reason) {
        SerialNumber existingSerialNumber = serialNumberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serial number not found"));

        String oldValues = buildAuditJson(existingSerialNumber);

        // Update fields including serial number value
        if (updatedSerialNumber.getSerialNumberValue() != null &&
            !updatedSerialNumber.getSerialNumberValue().equals(existingSerialNumber.getSerialNumberValue())) {
            // Check if new serial number value already exists
            if (serialNumberRepository.existsBySerialNumberValue(updatedSerialNumber.getSerialNumberValue())) {
                throw new IllegalArgumentException("Serial number already exists: " + updatedSerialNumber.getSerialNumberValue());
            }
            existingSerialNumber.setSerialNumberValue(updatedSerialNumber.getSerialNumberValue());
        }

        existingSerialNumber.setBatchNumber(updatedSerialNumber.getBatchNumber());
        existingSerialNumber.setNgaySanXuat(updatedSerialNumber.getNgaySanXuat());
        existingSerialNumber.setNgayHetBaoHanh(updatedSerialNumber.getNgayHetBaoHanh());
        existingSerialNumber.setNhaCungCap(updatedSerialNumber.getNhaCungCap());
        existingSerialNumber.setGhiChu(updatedSerialNumber.getGhiChu());

        SerialNumber savedSerialNumber = serialNumberRepository.save(existingSerialNumber);

        // Create audit trail
        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.updateEntry(
            savedSerialNumber.getId(),
            oldValues,
            buildAuditJson(savedSerialNumber),
            user,
            reason != null ? reason : "Cập nhật thông tin serial number"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Updated serial number: {} for variant: {}",
                savedSerialNumber.getSerialNumberValue(),
                savedSerialNumber.getSanPhamChiTiet().getId());

        return savedSerialNumber;
    }

    /**
     * Delete serial number (soft delete by marking as DISPOSED)
     */
    public void deleteSerialNumber(Long id, String user, String reason) {
        SerialNumber serialNumber = serialNumberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serial number not found"));

        // Only allow deletion if serial number is AVAILABLE or DAMAGED
        if (serialNumber.getTrangThai() != TrangThaiSerialNumber.AVAILABLE &&
            serialNumber.getTrangThai() != TrangThaiSerialNumber.DAMAGED) {
            throw new IllegalStateException("Cannot delete serial number with status: " + serialNumber.getTrangThai());
        }

        String oldValues = buildAuditJson(serialNumber);

        // Soft delete by marking as DISPOSED
        serialNumber.setTrangThai(TrangThaiSerialNumber.DISPOSED);
        SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);

        // Create audit trail
        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.updateEntry(
            savedSerialNumber.getId(),
            oldValues,
            buildAuditJson(savedSerialNumber),
            user,
            reason != null ? reason : "Xóa serial number"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Deleted (soft) serial number: {} for variant: {}",
                savedSerialNumber.getSerialNumberValue(),
                savedSerialNumber.getSanPhamChiTiet().getId());
    }

    /**
     * Change serial number status
     */
    public SerialNumber changeStatus(Long id, TrangThaiSerialNumber newStatus, String user, String reason) {
        SerialNumber serialNumber = serialNumberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serial number not found"));

        TrangThaiSerialNumber oldStatus = serialNumber.getTrangThai();
        
        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        serialNumber.setTrangThai(newStatus);
        SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);

        // Create audit trail
        SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.statusChangeEntry(
            savedSerialNumber.getId(),
            oldStatus.name(),
            newStatus.name(),
            user,
            reason != null ? reason : "Thay đổi trạng thái serial number"
        );
        auditHistoryRepository.save(auditEntry);

        log.info("Changed status of serial number {} from {} to {}", 
                serialNumber.getSerialNumberValue(), oldStatus, newStatus);

        return savedSerialNumber;
    }

    // Inventory Management

    /**
     * Get available quantity for a product variant
     */
    @Transactional(readOnly = true)
    public int getAvailableQuantityByVariant(Long variantId) {
        return (int) serialNumberRepository.countAvailableByVariant(variantId);
    }

    /**
     * Reserve serial numbers for an order with distributed locking to prevent race conditions
     */
    public List<SerialNumber> reserveSerialNumbers(Long variantId, int quantity, String channel, String orderId, String user) {
        String lockKey = distributedLockService.getInventoryLockKey(variantId);

        return distributedLockService.executeWithLock(lockKey, () -> {
            return optimisticLockingService.executeWithRetry(() -> {
                List<SerialNumber> availableSerialNumbers = serialNumberRepository.findAvailableByVariant(
                    variantId, PageRequest.of(0, quantity)
                );

                if (availableSerialNumbers.size() < quantity) {
                    throw new IllegalArgumentException(
                        String.format("Không đủ hàng tồn kho. Yêu cầu: %d, Có sẵn: %d",
                                     quantity, availableSerialNumbers.size())
                    );
                }

                List<SerialNumber> reservedSerialNumbers = new ArrayList<>();

                for (int i = 0; i < quantity; i++) {
                    SerialNumber serialNumber = availableSerialNumbers.get(i);
                    serialNumber.reserveWithTracking(channel, orderId);
                    SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);
                    reservedSerialNumbers.add(savedSerialNumber);

                    // Create audit trail
                    SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.reservationEntry(
                        savedSerialNumber.getId(),
                        channel,
                        orderId,
                        user,
                        "Đặt trước serial number cho đơn hàng"
                    );
                    auditHistoryRepository.save(auditEntry);

                    log.debug("Reserved serial number {} for order {} via channel {}",
                             serialNumber.getSerialNumberValue(), orderId, channel);
                }

                log.info("Reserved {} serial numbers for order {} via channel {} with distributed lock",
                        quantity, orderId, channel);

                // Publish inventory update event for WebSocket notifications
                try {
                    if (!reservedSerialNumbers.isEmpty()) {
                        SerialNumber firstSerial = reservedSerialNumbers.get(0);
                        SanPhamChiTiet variant = firstSerial.getSanPhamChiTiet();

                        int newAvailableQuantity = getAvailableQuantityByVariant(variantId);
                        int oldAvailableQuantity = newAvailableQuantity + quantity;

                        InventoryUpdateEvent event = InventoryUpdateEvent.builder()
                                .variantId(variantId)
                                .sku(variant.getSku())
                                .tenSanPham(variant.getSanPham().getTenSanPham())
                                .soLuongTonKhoCu(oldAvailableQuantity)
                                .soLuongTonKhoMoi(newAvailableQuantity)
                                .loaiThayDoi("RESERVED")
                                .nguoiThucHien(user)
                                .lyDoThayDoi("Đặt trước " + quantity + " sản phẩm cho đơn hàng " + orderId)
                                .timestamp(Instant.now())
                                .build();

                        eventPublisher.publishEvent(event);
                        log.debug("Published inventory update event for variant {} reservation", variantId);
                    }
                } catch (Exception e) {
                    log.error("Failed to publish inventory update event for reservation: {}", e.getMessage(), e);
                }

                return reservedSerialNumbers;
            });
        }, 15, 45); // 15 seconds wait, 45 seconds lease for inventory operations
    }

    /**
     * Confirm sale of reserved serial numbers
     */
    public void confirmSale(List<Long> serialNumberIds, String orderId, String user) {
        for (Long serialNumberId : serialNumberIds) {
            SerialNumber serialNumber = serialNumberRepository.findById(serialNumberId)
                    .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberId));

            serialNumber.markAsSold();
            serialNumberRepository.save(serialNumber);

            // Create audit trail
            SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.saleEntry(
                serialNumber.getId(),
                orderId,
                user,
                "Xác nhận bán serial number"
            );
            auditHistoryRepository.save(auditEntry);

            log.debug("Confirmed sale of serial number {}", serialNumber.getSerialNumberValue());
        }

        log.info("Confirmed sale of {} serial numbers for order {}", serialNumberIds.size(), orderId);

        // Publish inventory update event for WebSocket notifications
        try {
            if (!serialNumberIds.isEmpty()) {
                // Get the first serial number to extract variant information
                SerialNumber firstSerial = serialNumberRepository.findById(serialNumberIds.get(0))
                        .orElse(null);
                if (firstSerial != null) {
                    SanPhamChiTiet variant = firstSerial.getSanPhamChiTiet();
                    Long variantId = variant.getId();

                    int newAvailableQuantity = getAvailableQuantityByVariant(variantId);
                    int oldAvailableQuantity = newAvailableQuantity + serialNumberIds.size();

                    InventoryUpdateEvent event = InventoryUpdateEvent.builder()
                            .variantId(variantId)
                            .sku(variant.getSku())
                            .tenSanPham(variant.getSanPham().getTenSanPham())
                            .soLuongTonKhoCu(oldAvailableQuantity)
                            .soLuongTonKhoMoi(newAvailableQuantity)
                            .loaiThayDoi("SOLD")
                            .nguoiThucHien(user)
                            .lyDoThayDoi("Xác nhận bán " + serialNumberIds.size() + " sản phẩm cho đơn hàng " + orderId)
                            .timestamp(Instant.now())
                            .build();

                    eventPublisher.publishEvent(event);
                    log.debug("Published inventory update event for variant {} sale confirmation", variantId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish inventory update event for sale confirmation: {}", e.getMessage(), e);
        }
    }

    /**
     * Release reservations
     */
    public void releaseReservations(List<Long> serialNumberIds, String user, String reason) {
        for (Long serialNumberId : serialNumberIds) {
            SerialNumber serialNumber = serialNumberRepository.findById(serialNumberId)
                    .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberId));

            if (serialNumber.isReserved()) {
                serialNumber.releaseReservation();
                serialNumberRepository.save(serialNumber);

                // Create audit trail
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.releaseEntry(
                    serialNumber.getId(),
                    user,
                    reason != null ? reason : "Hủy đặt trước serial number"
                );
                auditHistoryRepository.save(auditEntry);

                log.debug("Released reservation for serial number {}", serialNumber.getSerialNumberValue());
            }
        }

        log.info("Released reservations for {} serial numbers", serialNumberIds.size());

        // Publish inventory update event for WebSocket notifications
        try {
            if (!serialNumberIds.isEmpty()) {
                // Get the first serial number to extract variant information
                SerialNumber firstSerial = serialNumberRepository.findById(serialNumberIds.get(0))
                        .orElse(null);
                if (firstSerial != null) {
                    SanPhamChiTiet variant = firstSerial.getSanPhamChiTiet();
                    Long variantId = variant.getId();

                    int newAvailableQuantity = getAvailableQuantityByVariant(variantId);
                    int oldAvailableQuantity = newAvailableQuantity - serialNumberIds.size();

                    InventoryUpdateEvent event = InventoryUpdateEvent.builder()
                            .variantId(variantId)
                            .sku(variant.getSku())
                            .tenSanPham(variant.getSanPham().getTenSanPham())
                            .soLuongTonKhoCu(oldAvailableQuantity)
                            .soLuongTonKhoMoi(newAvailableQuantity)
                            .loaiThayDoi("RELEASED")
                            .nguoiThucHien(user)
                            .lyDoThayDoi("Hủy đặt trước " + serialNumberIds.size() + " sản phẩm: " + reason)
                            .timestamp(Instant.now())
                            .build();

                    eventPublisher.publishEvent(event);
                    log.debug("Published inventory update event for variant {} reservation release", variantId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish inventory update event for reservation release: {}", e.getMessage(), e);
        }
    }

    /**
     * Get reserved serial number IDs for a specific order
     * Replaces InventoryService.getReservedItemsForOrder()
     */
    @Transactional(readOnly = true)
    public List<Long> getReservedSerialNumberIdsForOrder(String orderId) {
        List<SerialNumber> reservedSerialNumbers = serialNumberRepository.findByDonHangDatTruoc(orderId);
        return reservedSerialNumbers.stream()
                .map(SerialNumber::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get sold serial numbers for a specific product variant and quantity
     * Replaces InventoryService.getSoldItems()
     */
    @Transactional(readOnly = true)
    public List<SerialNumber> getSoldSerialNumbers(Long variantId, int quantity) {
        List<SerialNumber> soldSerialNumbers = serialNumberRepository.findBySanPhamChiTietIdAndTrangThai(
            variantId, TrangThaiSerialNumber.SOLD);

        // Return only the requested quantity
        return soldSerialNumbers.stream()
                .limit(quantity)
                .collect(Collectors.toList());
    }

    /**
     * Release sold serial numbers back to available status (for refunds)
     * Replaces InventoryService.releaseFromSold()
     */
    @Transactional
    public void releaseFromSold(List<Long> serialNumberIds, String user, String reason) {
        for (Long serialNumberId : serialNumberIds) {
            SerialNumber serialNumber = serialNumberRepository.findById(serialNumberId)
                    .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberId));

            if (serialNumber.isSold() || serialNumber.isReturned()) {
                serialNumber.releaseFromSold();
                serialNumberRepository.save(serialNumber);

                // Create audit trail
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.releaseEntry(
                    serialNumber.getId(),
                    user,
                    reason != null ? reason : "Hoàn trả serial number từ trạng thái đã bán"
                );
                auditHistoryRepository.save(auditEntry);

                log.debug("Released serial number {} from sold status", serialNumber.getSerialNumberValue());
            }
        }

        log.info("Released {} serial numbers from sold status", serialNumberIds.size());

        // Publish inventory update event for WebSocket notifications
        try {
            if (!serialNumberIds.isEmpty()) {
                // Get the first serial number to extract variant information
                SerialNumber firstSerial = serialNumberRepository.findById(serialNumberIds.get(0))
                        .orElse(null);
                if (firstSerial != null) {
                    SanPhamChiTiet variant = firstSerial.getSanPhamChiTiet();
                    Long variantId = variant.getId();

                    int newAvailableQuantity = getAvailableQuantityByVariant(variantId);
                    int oldAvailableQuantity = newAvailableQuantity - serialNumberIds.size();

                    InventoryUpdateEvent event = InventoryUpdateEvent.builder()
                            .variantId(variantId)
                            .sku(variant.getSku())
                            .tenSanPham(variant.getSanPham().getTenSanPham())
                            .soLuongTonKhoCu(oldAvailableQuantity)
                            .soLuongTonKhoMoi(newAvailableQuantity)
                            .loaiThayDoi("RESTOCKED")
                            .nguoiThucHien(user)
                            .lyDoThayDoi("Hoàn trả " + serialNumberIds.size() + " sản phẩm từ trạng thái đã bán: " + reason)
                            .timestamp(Instant.now())
                            .build();

                    eventPublisher.publishEvent(event);
                    log.debug("Published inventory update event for variant {} restock from sold", variantId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish inventory update event for restock from sold: {}", e.getMessage(), e);
        }
    }

    /**
     * Get available serial numbers for a specific product variant
     * Replaces InventoryService.getAvailableItems()
     */
    @Transactional(readOnly = true)
    public List<SerialNumber> getAvailableSerialNumbers(Long variantId) {
        return serialNumberRepository.findBySanPhamChiTietIdAndTrangThai(
            variantId, TrangThaiSerialNumber.AVAILABLE);
    }

    /**
     * Update the order ID for reserved serial numbers
     * Replaces InventoryService.updateReservationOrderId()
     */
    @Transactional
    public void updateReservationOrderId(List<Long> serialNumberIds, String oldOrderId, String newOrderId) {
        for (Long serialNumberId : serialNumberIds) {
            SerialNumber serialNumber = serialNumberRepository.findById(serialNumberId)
                    .orElseThrow(() -> new RuntimeException("Serial number not found: " + serialNumberId));

            if (serialNumber.isReserved() && oldOrderId.equals(serialNumber.getDonHangDatTruoc())) {
                serialNumber.setDonHangDatTruoc(newOrderId);
                serialNumberRepository.save(serialNumber);

                // Create audit trail
                String oldValues = String.format("{\"orderId\":\"%s\"}", oldOrderId);
                String newValues = String.format("{\"orderId\":\"%s\"}", newOrderId);
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.updateEntry(
                    serialNumber.getId(),
                    oldValues,
                    newValues,
                    "system",
                    String.format("Cập nhật order ID từ %s thành %s", oldOrderId, newOrderId)
                );
                auditHistoryRepository.save(auditEntry);

                log.debug("Updated order ID for serial number {} from {} to {}",
                         serialNumber.getSerialNumberValue(), oldOrderId, newOrderId);
            }
        }

        log.info("Updated order ID for {} serial numbers from {} to {}",
                serialNumberIds.size(), oldOrderId, newOrderId);
    }

    /**
     * Check if sufficient inventory is available for order items
     * Handles both specific serial numbers and general quantity requests
     * Replaces InventoryService.isInventoryAvailable()
     */
    @Transactional(readOnly = true)
    public boolean isInventoryAvailable(List<HoaDonChiTietDto> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("Order items list is null or empty, cannot validate inventory");
            return false;
        }

        log.debug("Validating inventory availability for {} order items", orderItems.size());

        for (HoaDonChiTietDto item : orderItems) {
            if (item.getSanPhamChiTietId() == null || item.getSoLuong() == null || item.getSoLuong() <= 0) {
                log.warn("Invalid order item: variantId={}, quantity={}",
                        item.getSanPhamChiTietId(), item.getSoLuong());
                return false;
            }

            log.debug("Checking item: variantId={}, quantity={}, serialNumberId={}",
                     item.getSanPhamChiTietId(), item.getSoLuong(), item.getSerialNumberId());

            // CRITICAL FIX: Check if specific serial numbers are provided
            if (item.getSerialNumberId() != null) {
                log.debug("Validating specific serial number with ID: {}", item.getSerialNumberId());

                // Validate specific serial number
                Optional<SerialNumber> serialNumber = serialNumberRepository.findById(item.getSerialNumberId());
                if (serialNumber.isEmpty()) {
                    log.warn("Serial number with ID {} not found", item.getSerialNumberId());
                    return false;
                }

                SerialNumber sn = serialNumber.get();
                log.debug("Found serial number: {} with status: {} for variant: {}",
                         sn.getSerialNumberValue(), sn.getTrangThai(), sn.getSanPhamChiTiet().getId());

                // Check if serial number belongs to the correct variant
                if (!sn.getSanPhamChiTiet().getId().equals(item.getSanPhamChiTietId())) {
                    log.warn("Serial number {} belongs to variant {} but order item is for variant {}",
                            sn.getSerialNumberValue(), sn.getSanPhamChiTiet().getId(), item.getSanPhamChiTietId());
                    return false;
                }

                // Check if serial number is available or reserved for cart (cart reservations are OK for order creation)
                if (!sn.isAvailable() && !isCartReservation(sn)) {
                    log.warn("Serial number {} is not available for order (status: {}, channel: {})",
                            sn.getSerialNumberValue(), sn.getTrangThai(), sn.getKenhDatTruoc());
                    return false;
                }

                log.debug("Serial number {} validated successfully for order (status: {})",
                         sn.getSerialNumberValue(), sn.getTrangThai());
            } else {
                log.debug("No specific serial number provided, checking general availability for variant: {}",
                         item.getSanPhamChiTietId());

                // No specific serial number provided, check general availability
                int availableQuantity = getAvailableQuantityByVariant(item.getSanPhamChiTietId());
                log.debug("Available quantity for variant {}: {}, requested: {}",
                         item.getSanPhamChiTietId(), availableQuantity, item.getSoLuong());

                if (availableQuantity < item.getSoLuong()) {
                    log.warn("Insufficient inventory for variant {}: requested={}, available={}",
                            item.getSanPhamChiTietId(), item.getSoLuong(), availableQuantity);
                    return false;
                }
            }
        }

        log.debug("All {} order items passed inventory validation", orderItems.size());
        return true;
    }

    /**
     * Reserve items with tracking for an order with distributed locking to prevent race conditions
     * Handles both specific serial numbers and general quantity requests
     * Replaces InventoryService.reserveItemsWithTracking()
     */
    @Transactional
    public List<Long> reserveItemsWithTracking(List<HoaDonChiTietDto> orderItems, String channel, String orderId, String user) {
        // Group items by variant to minimize lock contention
        Map<Long, List<HoaDonChiTietDto>> itemsByVariant = orderItems.stream()
            .collect(Collectors.groupingBy(HoaDonChiTietDto::getSanPhamChiTietId));

        List<Long> reservedSerialNumberIds = new ArrayList<>();

        try {
            // Process each variant group with its own distributed lock
            for (Map.Entry<Long, List<HoaDonChiTietDto>> entry : itemsByVariant.entrySet()) {
                Long variantId = entry.getKey();
                List<HoaDonChiTietDto> variantItems = entry.getValue();

                String lockKey = distributedLockService.getInventoryLockKey(variantId);

                List<Long> variantReservedIds = distributedLockService.executeWithLock(lockKey, () ->
                    optimisticLockingService.executeWithRetry(() -> {
                        List<Long> variantReservations = new ArrayList<>();

                        // CRITICAL FIX: Separate items with and without specific serial numbers first
                        List<HoaDonChiTietDto> itemsWithSpecificSerialNumbers = variantItems.stream()
                            .filter(item -> item.getSerialNumberId() != null)
                            .collect(Collectors.toList());

                        List<HoaDonChiTietDto> itemsWithoutSpecificSerialNumbers = variantItems.stream()
                            .filter(item -> item.getSerialNumberId() == null)
                            .collect(Collectors.toList());

                        log.debug("Processing variant {}: {} items with specific serial numbers, {} items without",
                                 variantId, itemsWithSpecificSerialNumbers.size(), itemsWithoutSpecificSerialNumbers.size());

                        // Handle items with specific serial numbers first
                        for (HoaDonChiTietDto item : itemsWithSpecificSerialNumbers) {
                            log.debug("Processing item with specific serial number ID: {}", item.getSerialNumberId());

                            Optional<SerialNumber> serialNumberOpt = serialNumberRepository.findById(item.getSerialNumberId());
                            if (serialNumberOpt.isEmpty()) {
                                throw new IllegalArgumentException("Không tìm thấy serial number với ID " + item.getSerialNumberId());
                            }

                            SerialNumber serialNumber = serialNumberOpt.get();
                            log.debug("Found serial number: {} with status: {}",
                                     serialNumber.getSerialNumberValue(), serialNumber.getTrangThai());

                            // Verify serial number belongs to correct variant
                            if (!serialNumber.getSanPhamChiTiet().getId().equals(variantId)) {
                                throw new IllegalArgumentException(String.format(
                                    "Serial number %s belongs to variant %d but order item is for variant %d",
                                    serialNumber.getSerialNumberValue(),
                                    serialNumber.getSanPhamChiTiet().getId(),
                                    variantId));
                            }

                            // Handle different reservation scenarios
                            if (serialNumber.isReserved()) {
                                // Check if it's reserved for a cart session and we're converting to order
                                String currentReservationId = serialNumber.getDonHangDatTruoc();
                                String currentChannel = serialNumber.getKenhDatTruoc();

                                if ("CART".equals(currentChannel) && currentReservationId != null && currentReservationId.startsWith("CART-")) {
                                    // This is a cart reservation being converted to order - update the reservation
                                    log.debug("Converting cart reservation to order reservation for serial number {} (cart: {} -> order: {})",
                                             serialNumber.getSerialNumberValue(), currentReservationId, orderId);

                                    serialNumber.setDonHangDatTruoc(orderId);
                                    serialNumber.setKenhDatTruoc(channel);
                                    serialNumber.setThoiGianDatTruoc(Instant.now());
                                    serialNumberRepository.save(serialNumber);

                                    // Create audit trail for cart-to-order conversion
                                    SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.reservationEntry(
                                        serialNumber.getId(),
                                        channel,
                                        orderId,
                                        user,
                                        String.format("Chuyển đổi từ giỏ hàng (%s) sang đơn hàng", currentReservationId)
                                    );
                                    auditHistoryRepository.save(auditEntry);
                                } else {
                                    // Update existing reservation with actual order ID (for temp order IDs)
                                    log.debug("Updating existing reservation for serial number {} from {} to {}",
                                             serialNumber.getSerialNumberValue(), currentReservationId, orderId);

                                    serialNumber.setDonHangDatTruoc(orderId);
                                    serialNumber.setKenhDatTruoc(channel);
                                    serialNumber.setThoiGianDatTruoc(Instant.now());
                                    serialNumberRepository.save(serialNumber);
                                }
                            } else if (serialNumber.isAvailable()) {
                                // Reserve the available serial number
                                log.debug("Reserving available serial number: {}", serialNumber.getSerialNumberValue());

                                serialNumber.reserveWithTracking(channel, orderId);
                                serialNumberRepository.save(serialNumber);

                                // Create audit trail
                                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.reservationEntry(
                                    serialNumber.getId(),
                                    channel,
                                    orderId,
                                    user,
                                    "Đặt trước serial number cho đơn hàng"
                                );
                                auditHistoryRepository.save(auditEntry);
                            } else {
                                throw new IllegalArgumentException("Serial number " + serialNumber.getSerialNumberValue() +
                                                                 " không khả dụng để đặt trước (trạng thái: " + serialNumber.getTrangThai() + ")");
                            }

                            variantReservations.add(serialNumber.getId());
                            log.debug("Successfully processed serial number ID: {}", serialNumber.getId());
                        }

                        // Handle items without specific serial numbers - aggregate quantities to fix inventory issue
                        if (!itemsWithoutSpecificSerialNumbers.isEmpty()) {
                            // Calculate total quantity needed for this variant
                            int totalQuantityNeeded = itemsWithoutSpecificSerialNumbers.stream()
                                .mapToInt(HoaDonChiTietDto::getSoLuong)
                                .sum();

                            log.debug("Reserving {} total serial numbers for variant {} (from {} items without specific serial numbers)",
                                     totalQuantityNeeded, variantId, itemsWithoutSpecificSerialNumbers.size());

                            // Reserve all needed serial numbers in one call to prevent inventory conflicts
                            List<SerialNumber> reservedForVariant = reserveSerialNumbers(
                                variantId,
                                totalQuantityNeeded,
                                channel,
                                orderId,
                                user
                            );

                            for (SerialNumber serialNumber : reservedForVariant) {
                                variantReservations.add(serialNumber.getId());
                            }
                        }

                        log.debug("Completed processing variant {}: reserved {} serial numbers",
                                 variantId, variantReservations.size());
                        return variantReservations;
                    })
                , 15L, 45L); // 15 seconds wait, 45 seconds lease for inventory operations

                reservedSerialNumberIds.addAll(variantReservedIds);
            }

            log.info("Successfully reserved {} serial numbers for order {} via {} with distributed locking",
                    reservedSerialNumberIds.size(), orderId, channel);
            return reservedSerialNumberIds;

        } catch (Exception e) {
            // If any reservation fails, release all previously reserved items
            log.error("Error during reservation, releasing {} already reserved items: {}",
                     reservedSerialNumberIds.size(), e.getMessage());
            releaseReservationsSafely(reservedSerialNumberIds);
            throw e;
        }
    }

    /**
     * Safely release reservations (for error handling)
     * Replaces InventoryService.releaseReservationSafely()
     */
    @Transactional
    public void releaseReservationsSafely(List<Long> serialNumberIds) {
        try {
            releaseReservations(serialNumberIds, "system", "Order creation failed");
        } catch (Exception e) {
            log.warn("Failed to safely release reservations: {}", e.getMessage());
        }
    }

    // Bulk Operations

    /**
     * Generate serial numbers for a product variant
     */
    public List<SerialNumber> generateSerialNumbers(Long variantId, int quantity, String pattern, String user) {
        SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found"));

        List<SerialNumber> generatedSerialNumbers = new ArrayList<>();
        String batchId = "BATCH-" + System.currentTimeMillis();

        for (int i = 1; i <= quantity; i++) {
            String serialNumberValue = generateSerialNumberValue(pattern, i);
            
            // Check if serial number already exists
            if (serialNumberRepository.existsBySerialNumberValue(serialNumberValue)) {
                log.warn("Serial number {} already exists, skipping", serialNumberValue);
                continue;
            }

            SerialNumber serialNumber = SerialNumber.builder()
                    .serialNumberValue(serialNumberValue)
                    .sanPhamChiTiet(variant)
                    .trangThai(TrangThaiSerialNumber.AVAILABLE)
                    .importBatchId(batchId)
                    .build();

            SerialNumber savedSerialNumber = serialNumberRepository.save(serialNumber);
            generatedSerialNumbers.add(savedSerialNumber);

            // Create audit trail
            SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.bulkOperationEntry(
                savedSerialNumber.getId(),
                "GENERATE",
                batchId,
                user,
                "Tạo serial number hàng loạt"
            );
            auditHistoryRepository.save(auditEntry);
        }

        log.info("Generated {} serial numbers for variant {} with batch ID {}", 
                generatedSerialNumbers.size(), variantId, batchId);

        return generatedSerialNumbers;
    }

    // Scheduled Tasks

    /**
     * Clean up expired reservations (runs every 5 minutes)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredReservations() {
        Instant expiredBefore = Instant.now().minus(15, ChronoUnit.MINUTES);

        List<SerialNumber> expiredReservations = serialNumberRepository.findExpiredReservations(expiredBefore);

        if (!expiredReservations.isEmpty()) {
            int releasedCount = serialNumberRepository.releaseExpiredReservations(expiredBefore);

            // Create audit entries for released reservations
            for (SerialNumber serialNumber : expiredReservations) {
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.releaseEntry(
                    serialNumber.getId(),
                    "SYSTEM",
                    "Hết hạn đặt trước tự động"
                );
                auditHistoryRepository.save(auditEntry);
            }

            log.info("Released {} expired reservations", releasedCount);
        }

        // Also clean up temporary order IDs that are older than 30 minutes
        cleanupTemporaryOrderIds();

        // Clean up expired cart reservations
        cleanupExpiredCartReservations();
    }

    /**
     * Clean up reservations with temporary order IDs that are older than 30 minutes.
     * This prevents inventory deadlocks from failed order creation processes.
     */
    @Transactional
    public void cleanupTemporaryOrderIds() {
        Instant expiredBefore = Instant.now().minus(30, ChronoUnit.MINUTES);

        // Find reservations with temporary order IDs that are older than 30 minutes
        List<SerialNumber> tempReservations = serialNumberRepository.findByDonHangDatTruocStartingWith("TEMP-");

        List<SerialNumber> expiredTempReservations = tempReservations.stream()
            .filter(sn -> sn.getThoiGianDatTruoc() != null && sn.getThoiGianDatTruoc().isBefore(expiredBefore))
            .collect(Collectors.toList());

        if (!expiredTempReservations.isEmpty()) {
            for (SerialNumber serialNumber : expiredTempReservations) {
                String tempOrderId = serialNumber.getDonHangDatTruoc();
                serialNumber.releaseReservation();
                serialNumberRepository.save(serialNumber);

                // Create audit entry
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.releaseEntry(
                    serialNumber.getId(),
                    "SYSTEM",
                    String.format("Cleanup temporary order ID: %s", tempOrderId)
                );
                auditHistoryRepository.save(auditEntry);

                log.debug("Released expired temporary reservation for serial number {} with temp order ID {}",
                         serialNumber.getSerialNumberValue(), tempOrderId);
            }

            log.info("Cleaned up {} expired temporary order reservations", expiredTempReservations.size());
        }
    }

    /**
     * Clean up expired cart reservations that are older than 30 minutes.
     * This prevents inventory deadlocks from abandoned cart sessions.
     */
    @Transactional
    public void cleanupExpiredCartReservations() {
        Instant expiredBefore = Instant.now().minus(30, ChronoUnit.MINUTES);

        // Find reservations with cart order IDs that are older than 30 minutes
        List<SerialNumber> cartReservations = serialNumberRepository.findByDonHangDatTruocStartingWith("CART-");

        List<SerialNumber> expiredCartReservations = cartReservations.stream()
            .filter(sn -> sn.getThoiGianDatTruoc() != null && sn.getThoiGianDatTruoc().isBefore(expiredBefore))
            .collect(Collectors.toList());

        if (!expiredCartReservations.isEmpty()) {
            for (SerialNumber serialNumber : expiredCartReservations) {
                String cartSessionId = serialNumber.getDonHangDatTruoc();
                serialNumber.releaseReservation();
                serialNumberRepository.save(serialNumber);

                // Create audit entry
                SerialNumberAuditHistory auditEntry = SerialNumberAuditHistory.releaseEntry(
                    serialNumber.getId(),
                    "SYSTEM",
                    String.format("Cleanup expired cart session: %s", cartSessionId)
                );
                auditHistoryRepository.save(auditEntry);

                log.debug("Released expired cart reservation for serial number {} with cart session ID {}",
                         serialNumber.getSerialNumberValue(), cartSessionId);
            }

            log.info("Cleaned up {} expired cart reservations", expiredCartReservations.size());
        }
    }

    // Helper Methods

    /**
     * Check if a serial number is reserved for cart (cart reservations are valid for order creation)
     */
    private boolean isCartReservation(SerialNumber sn) {
        return sn.isReserved() && "CART".equals(sn.getKenhDatTruoc());
    }

    private void validateStatusTransition(TrangThaiSerialNumber from, TrangThaiSerialNumber to) {
        // Define valid transitions
        Map<TrangThaiSerialNumber, Set<TrangThaiSerialNumber>> validTransitions = Map.of(
            TrangThaiSerialNumber.AVAILABLE, Set.of(TrangThaiSerialNumber.RESERVED, TrangThaiSerialNumber.DAMAGED, TrangThaiSerialNumber.UNAVAILABLE, TrangThaiSerialNumber.DISPLAY_UNIT),
            TrangThaiSerialNumber.RESERVED, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.SOLD, TrangThaiSerialNumber.DAMAGED),
            TrangThaiSerialNumber.SOLD, Set.of(TrangThaiSerialNumber.RETURNED, TrangThaiSerialNumber.DAMAGED),
            TrangThaiSerialNumber.RETURNED, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.DAMAGED, TrangThaiSerialNumber.DISPOSED),
            TrangThaiSerialNumber.DAMAGED, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.DISPOSED),
            TrangThaiSerialNumber.UNAVAILABLE, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.DAMAGED),
            TrangThaiSerialNumber.DISPLAY_UNIT, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.DAMAGED),
            TrangThaiSerialNumber.QUALITY_CONTROL, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.DAMAGED),
            TrangThaiSerialNumber.IN_TRANSIT, Set.of(TrangThaiSerialNumber.AVAILABLE, TrangThaiSerialNumber.QUALITY_CONTROL)
        );

        Set<TrangThaiSerialNumber> allowedTransitions = validTransitions.get(from);
        if (allowedTransitions == null || !allowedTransitions.contains(to)) {
            throw new IllegalArgumentException(
                String.format("Invalid status transition from %s to %s", from, to)
            );
        }
    }

    private String generateSerialNumberValue(String pattern, int sequence) {
        // Replace placeholders in pattern
        return pattern
                .replace("{SEQ}", String.format("%04d", sequence))
                .replace("{TIMESTAMP}", String.valueOf(System.currentTimeMillis() % 100000));
    }

    private String buildAuditJson(SerialNumber serialNumber) {
        return String.format(
            "{\"serialNumberValue\":\"%s\",\"trangThai\":\"%s\",\"variantId\":%d,\"batchNumber\":\"%s\",\"supplier\":\"%s\"}",
            serialNumber.getSerialNumberValue(),
            serialNumber.getTrangThai(),
            serialNumber.getSanPhamChiTiet().getId(),
            serialNumber.getBatchNumber() != null ? serialNumber.getBatchNumber() : "",
            serialNumber.getNhaCungCap() != null ? serialNumber.getNhaCungCap() : ""
        );
    }
}
