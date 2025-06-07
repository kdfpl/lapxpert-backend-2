package com.lapxpert.backend.nguoidung.domain.repository;

import com.lapxpert.backend.nguoidung.domain.entity.NguoiDungAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for NguoiDungAuditHistory entity.
 * Provides methods to query audit history for users.
 */
@Repository
public interface NguoiDungAuditHistoryRepository extends JpaRepository<NguoiDungAuditHistory, Long> {

    /**
     * Find all audit history for a specific user, ordered by timestamp descending
     * @param nguoiDungId ID of the user
     * @return List of audit history entries
     */
    @Query("SELECT h FROM NguoiDungAuditHistory h WHERE h.nguoiDungId = :nguoiDungId ORDER BY h.thoiGianThayDoi DESC")
    List<NguoiDungAuditHistory> findByNguoiDungIdOrderByThoiGianThayDoiDesc(@Param("nguoiDungId") Long nguoiDungId);

    /**
     * Find audit history for a specific user with pagination
     * @param nguoiDungId ID of the user
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<NguoiDungAuditHistory> findByNguoiDungIdOrderByThoiGianThayDoiDesc(Long nguoiDungId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param nguoiDungId ID of the user
     * @param hanhDong Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE, ROLE_CHANGE)
     * @return List of audit history entries
     */
    List<NguoiDungAuditHistory> findByNguoiDungIdAndHanhDongOrderByThoiGianThayDoiDesc(Long nguoiDungId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param nguoiDungId ID of the user
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM NguoiDungAuditHistory h WHERE h.nguoiDungId = :nguoiDungId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<NguoiDungAuditHistory> findByNguoiDungIdAndDateRange(
            @Param("nguoiDungId") Long nguoiDungId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user who performed the action
     * @param nguoiDungId ID of the user being audited
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<NguoiDungAuditHistory> findByNguoiDungIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long nguoiDungId, String nguoiThucHien);

    /**
     * Count total audit entries for a user
     * @param nguoiDungId ID of the user
     * @return Number of audit entries
     */
    long countByNguoiDungId(Long nguoiDungId);

    /**
     * Find recent audit history across all users (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM NguoiDungAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<NguoiDungAuditHistory> findRecentAuditHistory(Pageable pageable);

    /**
     * Find role change audit history
     * @param pageable Pagination information
     * @return Page of role change audit history entries
     */
    @Query("SELECT h FROM NguoiDungAuditHistory h WHERE h.hanhDong = 'ROLE_CHANGE' ORDER BY h.thoiGianThayDoi DESC")
    Page<NguoiDungAuditHistory> findRoleChangeHistory(Pageable pageable);

    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM NguoiDungAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
