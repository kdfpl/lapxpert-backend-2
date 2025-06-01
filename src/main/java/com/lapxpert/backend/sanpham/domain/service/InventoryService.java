package com.lapxpert.backend.sanpham.domain.service;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonChiTietDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing individual product item inventory.
 * Handles reservation, confirmation, and release of product items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    /**
     * Reservation timeout in minutes (configurable via application properties)
     */
    @Value("${inventory.reservation.timeout.minutes:15}")
    private long reservationTimeoutMinutes;

    /**
     * POS reservation priority timeout in minutes
     */
    @Value("${inventory.reservation.pos.timeout.minutes:30}")
    private long posReservationTimeoutMinutes;

    /**
     * Reserve product items for an order with channel tracking.
     * This marks items as RESERVED but doesn't finalize the sale.
     *
     * @param orderItems List of items to reserve
     * @param channel Channel making the reservation (POS, ONLINE, etc.)
     * @param orderId Order ID for tracking
     * @return List of reserved item IDs for tracking
     * @throws IllegalArgumentException if insufficient inventory
     */
    @Transactional
    public List<Long> reserveItemsWithTracking(List<HoaDonChiTietDto> orderItems, String channel, String orderId) {
        List<Long> reservedItemIds = new ArrayList<>();

        for (HoaDonChiTietDto orderItem : orderItems) {
            Long productVariantId = orderItem.getSanPhamChiTietId();
            Integer quantity = orderItem.getSoLuong();

            // Find available items for this product variant
            List<SanPhamChiTiet> availableItems = sanPhamChiTietRepository
                .findAvailableItemsByProductVariant(productVariantId, quantity);

            if (availableItems.size() < quantity) {
                // Release any items already reserved in this transaction
                releaseReservationSafely(reservedItemIds);
                throw new IllegalArgumentException(
                    String.format("Insufficient inventory for product variant %d. Required: %d, Available: %d",
                        productVariantId, quantity, availableItems.size()));
            }

            // Reserve the required quantity
            for (int i = 0; i < quantity; i++) {
                SanPhamChiTiet item = availableItems.get(i);
                item.reserveWithTracking(channel, orderId);
                sanPhamChiTietRepository.save(item);
                reservedItemIds.add(item.getId());

                log.debug("Reserved item {} (Serial: {}) for order {} via channel {}",
                    item.getId(), item.getSerialNumber(), orderId, channel);
            }
        }

        log.info("Reserved {} items for order {} via channel {}", reservedItemIds.size(), orderId, channel);
        return reservedItemIds;
    }

    /**
     * Reserve product items for an order (legacy method for backward compatibility).
     * This marks items as RESERVED but doesn't finalize the sale.
     *
     * @param orderItems List of items to reserve
     * @return List of reserved item IDs
     * @throws IllegalArgumentException if insufficient inventory
     */
    @Transactional
    public List<Long> reserveItems(List<HoaDonChiTietDto> orderItems) {
        List<Long> reservedItemIds = new ArrayList<>();

        try {
            for (HoaDonChiTietDto orderItem : orderItems) {
                List<Long> itemIds = reserveItemsForProduct(
                    orderItem.getSanPhamChiTietId(),
                    orderItem.getSoLuong()
                );
                reservedItemIds.addAll(itemIds);
            }

            log.info("Successfully reserved {} items for order", reservedItemIds.size());
            return reservedItemIds;

        } catch (Exception e) {
            // If any reservation fails, release all previously reserved items
            releaseReservation(reservedItemIds);
            throw e;
        }
    }

    /**
     * Reserve specific quantity of items for a product variant.
     *
     * @param sanPhamChiTietId Product variant ID (this represents the product configuration)
     * @param quantity Number of items to reserve
     * @return List of reserved individual item IDs
     */
    private List<Long> reserveItemsForProduct(Long sanPhamChiTietId, Integer quantity) {
        // Find available individual items for this product configuration
        List<SanPhamChiTiet> availableItems = sanPhamChiTietRepository
            .findAvailableItemsByProductVariant(sanPhamChiTietId, quantity);

        if (availableItems.size() < quantity) {
            throw new IllegalArgumentException(
                String.format("Insufficient inventory for product %d. Requested: %d, Available: %d",
                    sanPhamChiTietId, quantity, availableItems.size())
            );
        }

        List<Long> reservedIds = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            SanPhamChiTiet item = availableItems.get(i);
            item.reserve(); // Use proper enum-based method to set status to RESERVED
            sanPhamChiTietRepository.save(item);
            reservedIds.add(item.getId());

            log.debug("Reserved item {} (Serial: {}) for product variant {}",
                item.getId(), item.getSerialNumber(), sanPhamChiTietId);
        }

        return reservedIds;
    }

    /**
     * Confirm sale of reserved items (called after successful payment).
     * Items are already marked as unavailable, this is for audit/tracking.
     *
     * @param reservedItemIds List of item IDs that were reserved
     */
    @Transactional
    public void confirmSale(List<Long> reservedItemIds) {
        for (Long itemId : reservedItemIds) {
            sanPhamChiTietRepository.findById(itemId).ifPresent(item -> {
                // Mark items as sold using proper enum-based method
                item.markAsSold();
                sanPhamChiTietRepository.save(item);
                log.debug("Confirmed sale of item {} (Serial: {})", item.getId(), item.getSerialNumber());
            });
        }

        log.info("Confirmed sale of {} items", reservedItemIds.size());
    }

    /**
     * Release reservation of items (called when order is cancelled or fails).
     *
     * @param reservedItemIds List of item IDs to release
     */
    @Transactional
    public void releaseReservation(List<Long> reservedItemIds) {
        for (Long itemId : reservedItemIds) {
            sanPhamChiTietRepository.findById(itemId).ifPresent(item -> {
                item.releaseReservation(); // Use proper enum-based method to mark as available
                sanPhamChiTietRepository.save(item);

                log.debug("Released reservation for item {} (Serial: {})", item.getId(), item.getSerialNumber());
            });
        }

        log.info("Released reservation for {} items", reservedItemIds.size());
    }

    /**
     * Safely release reservation of items (called when order creation fails).
     * This method checks item status before attempting to release reservations.
     *
     * @param reservedItemIds List of item IDs to release
     */
    @Transactional
    public void releaseReservationSafely(List<Long> reservedItemIds) {
        int releasedCount = 0;
        int skippedCount = 0;

        for (Long itemId : reservedItemIds) {
            Optional<SanPhamChiTiet> itemOpt = sanPhamChiTietRepository.findById(itemId);
            if (itemOpt.isPresent()) {
                SanPhamChiTiet item = itemOpt.get();
                if (item.isReserved()) {
                    try {
                        item.releaseReservation();
                        sanPhamChiTietRepository.save(item);
                        releasedCount++;
                        log.debug("Released reservation for item {} (Serial: {})", item.getId(), item.getSerialNumber());
                    } catch (Exception e) {
                        skippedCount++;
                        log.warn("Failed to release reservation for item {} (Serial: {}): {}",
                            item.getId(), item.getSerialNumber(), e.getMessage());
                    }
                } else {
                    skippedCount++;
                    log.debug("Skipped releasing reservation for item {} (Serial: {}) - current status: {}",
                        item.getId(), item.getSerialNumber(), item.getTrangThai());
                }
            } else {
                skippedCount++;
                log.debug("Skipped releasing reservation for item {} - item not found", itemId);
            }
        }

        log.info("Safely released reservation for {} items, skipped {} items", releasedCount, skippedCount);
    }

    /**
     * Check available quantity for a product variant.
     *
     * @param sanPhamChiTietId Product variant ID
     * @return Number of available individual items
     */
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long sanPhamChiTietId) {
        return sanPhamChiTietRepository.countAvailableItemsByProductVariant(sanPhamChiTietId);
    }

    /**
     * Check if sufficient quantity is available for an order.
     *
     * @param orderItems List of items to check
     * @return true if all items are available in sufficient quantity
     */
    @Transactional(readOnly = true)
    public boolean isInventoryAvailable(List<HoaDonChiTietDto> orderItems) {
        // Handle null input gracefully
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("Order items list is null or empty, cannot validate inventory");
            return false;
        }

        for (HoaDonChiTietDto orderItem : orderItems) {
            if (orderItem == null || orderItem.getSanPhamChiTietId() == null) {
                log.warn("Order item or product variant ID is null, skipping validation");
                continue;
            }

            int available = getAvailableQuantity(orderItem.getSanPhamChiTietId());
            if (available < orderItem.getSoLuong()) {
                log.warn("Insufficient inventory for product {}: requested {}, available {}",
                    orderItem.getSanPhamChiTietId(), orderItem.getSoLuong(), available);
                return false;
            }
        }
        return true;
    }

    /**
     * Get list of available items for a product variant.
     *
     * @param sanPhamChiTietId Product variant ID
     * @return List of available individual items
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTiet> getAvailableItems(Long sanPhamChiTietId) {
        return sanPhamChiTietRepository.findAvailableItemsByProductVariant(sanPhamChiTietId, Integer.MAX_VALUE);
    }

    /**
     * Get list of sold items for a product variant.
     *
     * @param sanPhamChiTietId Product variant ID
     * @param maxQuantity Maximum number of items to return
     * @return List of sold individual items
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTiet> getSoldItems(Long sanPhamChiTietId, Integer maxQuantity) {
        return sanPhamChiTietRepository.findByTrangThai(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.SOLD)
            .stream()
            .filter(item -> item.getId().equals(sanPhamChiTietId))
            .limit(maxQuantity)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Release items from sold status back to available (for refunds).
     *
     * @param soldItemIds List of sold item IDs to release back to available
     */
    @Transactional
    public void releaseFromSold(List<Long> soldItemIds) {
        for (Long itemId : soldItemIds) {
            sanPhamChiTietRepository.findById(itemId).ifPresent(item -> {
                if (item.isSold()) {
                    item.releaseFromSold(); // Mark as available again
                    sanPhamChiTietRepository.save(item);
                    log.debug("Released item {} (Serial: {}) from sold back to available",
                             item.getId(), item.getSerialNumber());
                } else {
                    log.warn("Attempted to release item {} (Serial: {}) from sold, but current status is: {}",
                            item.getId(), item.getSerialNumber(), item.getTrangThai());
                }
            });
        }

        log.info("Released {} items from sold back to available", soldItemIds.size());
    }

    /**
     * Get items that are reserved for a specific order.
     * This method finds items that are reserved for the given order ID.
     *
     * @param sanPhamChiTietId Product variant ID
     * @param orderId Order ID to search for
     * @param maxQuantity Maximum number of items to return
     * @return List of reserved items for the order
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTiet> getReservedItemsForOrder(Long sanPhamChiTietId, String orderId, Integer maxQuantity) {
        // Find all reserved items for this product variant
        List<SanPhamChiTiet> allReservedItems = sanPhamChiTietRepository.findByTrangThai(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED);

        // Filter items that are reserved for this specific order
        return allReservedItems.stream()
            .filter(item -> item.getId().equals(sanPhamChiTietId) &&
                           item.isReservedForOrder(orderId))
            .limit(maxQuantity)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get all item IDs that are reserved for a specific order.
     * This method finds all items that are reserved for the given order ID.
     *
     * @param orderId Order ID to search for
     * @return List of reserved item IDs for the order
     */
    @Transactional(readOnly = true)
    public List<Long> getReservedItemsForOrder(String orderId) {
        // Find all reserved items
        List<SanPhamChiTiet> allReservedItems = sanPhamChiTietRepository.findByTrangThai(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED);

        // Filter items that are reserved for this specific order and return their IDs
        return allReservedItems.stream()
            .filter(item -> item.isReservedForOrder(orderId))
            .map(SanPhamChiTiet::getId)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update the order ID for reserved items.
     * This is used to update temporary order IDs with actual order IDs after order creation.
     *
     * @param reservedItemIds List of reserved item IDs
     * @param oldOrderId Old order ID to replace
     * @param newOrderId New order ID to set
     */
    @Transactional
    public void updateReservationOrderId(List<Long> reservedItemIds, String oldOrderId, String newOrderId) {
        for (Long itemId : reservedItemIds) {
            sanPhamChiTietRepository.findById(itemId).ifPresent(item -> {
                if (item.isReserved() && oldOrderId.equals(item.getDonHangDatTruoc())) {
                    item.setDonHangDatTruoc(newOrderId);
                    sanPhamChiTietRepository.save(item);
                    log.debug("Updated order ID for item {} (Serial: {}) from {} to {}",
                        item.getId(), item.getSerialNumber(), oldOrderId, newOrderId);
                }
            });
        }
        log.info("Updated order ID for {} reserved items from {} to {}", reservedItemIds.size(), oldOrderId, newOrderId);
    }

    /**
     * Scheduled task to clean up expired reservations.
     * Runs every 5 minutes to release expired reservations.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupExpiredReservations() {
        try {
            // Find items with expired reservations
            List<SanPhamChiTiet> allReservedItems = sanPhamChiTietRepository.findByTrangThai(
                com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED);

            int cleanedCount = 0;
            for (SanPhamChiTiet item : allReservedItems) {
                // Check if reservation is actually expired
                if (item.isReservationExpired(reservationTimeoutMinutes)) {
                    try {
                        item.releaseReservation();
                        sanPhamChiTietRepository.save(item);
                        cleanedCount++;

                        log.debug("Released expired reservation for item {} (Serial: {}) - reserved at {} by channel {}",
                            item.getId(), item.getSerialNumber(), item.getThoiGianDatTruoc(), item.getKenhDatTruoc());
                    } catch (Exception e) {
                        log.warn("Failed to release expired reservation for item {} (Serial: {}): {}",
                            item.getId(), item.getSerialNumber(), e.getMessage());
                    }
                }
            }

            if (cleanedCount > 0) {
                log.info("Cleaned up {} expired reservations", cleanedCount);
            }
        } catch (Exception e) {
            log.error("Error during expired reservation cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get reservation statistics for monitoring
     * @return Reservation statistics
     */
    @Transactional(readOnly = true)
    public ReservationStats getReservationStats() {
        long totalReserved = sanPhamChiTietRepository.countByTrangThai(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED);

        long posReserved = sanPhamChiTietRepository.countByTrangThaiAndKenhDatTruoc(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED, "POS");

        long onlineReserved = sanPhamChiTietRepository.countByTrangThaiAndKenhDatTruoc(
            com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.RESERVED, "ONLINE");

        return new ReservationStats(totalReserved, posReserved, onlineReserved);
    }

    /**
     * Data class for reservation statistics
     */
    public static class ReservationStats {
        public final long totalReserved;
        public final long posReserved;
        public final long onlineReserved;

        public ReservationStats(long totalReserved, long posReserved, long onlineReserved) {
            this.totalReserved = totalReserved;
            this.posReserved = posReserved;
            this.onlineReserved = onlineReserved;
        }
    }
}
