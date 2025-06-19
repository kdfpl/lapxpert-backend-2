package com.lapxpert.backend.phieugiamgia.repository;

import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGiaAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for PhieuGiamGiaAuditHistory entity.
 * Provides methods to query audit history for vouchers.
 */
@Repository
public interface PhieuGiamGiaAuditHistoryRepository extends JpaRepository<PhieuGiamGiaAuditHistory, Long> {

    /**
     * Find all audit history for a specific voucher, ordered by timestamp descending
     * @param phieuGiamGiaId ID of the voucher
     * @return List of audit history entries
     */
    @Query("SELECT h FROM PhieuGiamGiaAuditHistory h WHERE h.phieuGiamGiaId = :phieuGiamGiaId ORDER BY h.thoiGianThayDoi DESC")
    List<PhieuGiamGiaAuditHistory> findByPhieuGiamGiaIdOrderByThoiGianThayDoiDesc(@Param("phieuGiamGiaId") Long phieuGiamGiaId);

    /**
     * Find audit history for a specific voucher with pagination
     * @param phieuGiamGiaId ID of the voucher
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<PhieuGiamGiaAuditHistory> findByPhieuGiamGiaIdOrderByThoiGianThayDoiDesc(Long phieuGiamGiaId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param phieuGiamGiaId ID of the voucher
     * @param hanhDong Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE)
     * @return List of audit history entries
     */
    List<PhieuGiamGiaAuditHistory> findByPhieuGiamGiaIdAndHanhDongOrderByThoiGianThayDoiDesc(Long phieuGiamGiaId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param phieuGiamGiaId ID of the voucher
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM PhieuGiamGiaAuditHistory h WHERE h.phieuGiamGiaId = :phieuGiamGiaId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<PhieuGiamGiaAuditHistory> findByPhieuGiamGiaIdAndDateRange(
            @Param("phieuGiamGiaId") Long phieuGiamGiaId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user
     * @param phieuGiamGiaId ID of the voucher
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<PhieuGiamGiaAuditHistory> findByPhieuGiamGiaIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long phieuGiamGiaId, String nguoiThucHien);

    /**
     * Count total audit entries for a voucher
     * @param phieuGiamGiaId ID of the voucher
     * @return Number of audit entries
     */
    long countByPhieuGiamGiaId(Long phieuGiamGiaId);

    /**
     * Find recent audit history across all vouchers (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM PhieuGiamGiaAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<PhieuGiamGiaAuditHistory> findRecentAuditHistory(Pageable pageable);



    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM PhieuGiamGiaAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
