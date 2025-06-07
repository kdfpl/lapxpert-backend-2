package com.lapxpert.backend.dotgiamgia.domain.repository;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGiaAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for DotGiamGiaAuditHistory entity.
 * Provides methods to query audit history for discount campaigns.
 */
@Repository
public interface DotGiamGiaAuditHistoryRepository extends JpaRepository<DotGiamGiaAuditHistory, Long> {

    /**
     * Find all audit history for a specific discount campaign, ordered by timestamp descending
     * @param dotGiamGiaId ID of the discount campaign
     * @return List of audit history entries
     */
    @Query("SELECT h FROM DotGiamGiaAuditHistory h WHERE h.dotGiamGiaId = :dotGiamGiaId ORDER BY h.thoiGianThayDoi DESC")
    List<DotGiamGiaAuditHistory> findByDotGiamGiaIdOrderByThoiGianThayDoiDesc(@Param("dotGiamGiaId") Long dotGiamGiaId);

    /**
     * Find audit history for a specific discount campaign with pagination
     * @param dotGiamGiaId ID of the discount campaign
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<DotGiamGiaAuditHistory> findByDotGiamGiaIdOrderByThoiGianThayDoiDesc(Long dotGiamGiaId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param dotGiamGiaId ID of the discount campaign
     * @param hanhDong Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE)
     * @return List of audit history entries
     */
    List<DotGiamGiaAuditHistory> findByDotGiamGiaIdAndHanhDongOrderByThoiGianThayDoiDesc(Long dotGiamGiaId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param dotGiamGiaId ID of the discount campaign
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM DotGiamGiaAuditHistory h WHERE h.dotGiamGiaId = :dotGiamGiaId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<DotGiamGiaAuditHistory> findByDotGiamGiaIdAndDateRange(
            @Param("dotGiamGiaId") Long dotGiamGiaId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user
     * @param dotGiamGiaId ID of the discount campaign
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<DotGiamGiaAuditHistory> findByDotGiamGiaIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long dotGiamGiaId, String nguoiThucHien);

    /**
     * Count total audit entries for a discount campaign
     * @param dotGiamGiaId ID of the discount campaign
     * @return Number of audit entries
     */
    long countByDotGiamGiaId(Long dotGiamGiaId);

    /**
     * Find recent audit history across all discount campaigns (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM DotGiamGiaAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<DotGiamGiaAuditHistory> findRecentAuditHistory(Pageable pageable);

    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM DotGiamGiaAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
