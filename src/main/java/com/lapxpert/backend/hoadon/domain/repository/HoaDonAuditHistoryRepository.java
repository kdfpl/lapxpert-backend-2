package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for HoaDonAuditHistory entity.
 * Provides methods to query audit history for orders.
 */
@Repository
public interface HoaDonAuditHistoryRepository extends JpaRepository<HoaDonAuditHistory, Long> {

    /**
     * Find all audit history for a specific order, ordered by timestamp descending
     * @param hoaDonId ID of the order
     * @return List of audit history entries
     */
    @Query("SELECT h FROM HoaDonAuditHistory h WHERE h.hoaDonId = :hoaDonId ORDER BY h.thoiGianThayDoi DESC")
    List<HoaDonAuditHistory> findByHoaDonIdOrderByThoiGianThayDoiDesc(@Param("hoaDonId") Long hoaDonId);

    /**
     * Find audit history for a specific order with pagination
     * @param hoaDonId ID of the order
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<HoaDonAuditHistory> findByHoaDonIdOrderByThoiGianThayDoiDesc(Long hoaDonId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param hoaDonId ID of the order
     * @param hanhDong Action type (CREATE, UPDATE, CANCEL, STATUS_CHANGE, PAYMENT_STATUS_CHANGE)
     * @return List of audit history entries
     */
    List<HoaDonAuditHistory> findByHoaDonIdAndHanhDongOrderByThoiGianThayDoiDesc(Long hoaDonId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param hoaDonId ID of the order
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM HoaDonAuditHistory h WHERE h.hoaDonId = :hoaDonId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<HoaDonAuditHistory> findByHoaDonIdAndDateRange(
            @Param("hoaDonId") Long hoaDonId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user
     * @param hoaDonId ID of the order
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<HoaDonAuditHistory> findByHoaDonIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long hoaDonId, String nguoiThucHien);

    /**
     * Count total audit entries for an order
     * @param hoaDonId ID of the order
     * @return Number of audit entries
     */
    long countByHoaDonId(Long hoaDonId);

    /**
     * Find recent audit history across all orders (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM HoaDonAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<HoaDonAuditHistory> findRecentAuditHistory(Pageable pageable);

    /**
     * Find payment status change audit history
     * @param pageable Pagination information
     * @return Page of payment status change audit history entries
     */
    @Query("SELECT h FROM HoaDonAuditHistory h WHERE h.hanhDong = 'PAYMENT_STATUS_CHANGE' ORDER BY h.thoiGianThayDoi DESC")
    Page<HoaDonAuditHistory> findPaymentStatusChangeHistory(Pageable pageable);

    /**
     * Find order cancellation audit history
     * @param pageable Pagination information
     * @return Page of order cancellation audit history entries
     */
    @Query("SELECT h FROM HoaDonAuditHistory h WHERE h.hanhDong = 'CANCEL' ORDER BY h.thoiGianThayDoi DESC")
    Page<HoaDonAuditHistory> findOrderCancellationHistory(Pageable pageable);

    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM HoaDonAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
