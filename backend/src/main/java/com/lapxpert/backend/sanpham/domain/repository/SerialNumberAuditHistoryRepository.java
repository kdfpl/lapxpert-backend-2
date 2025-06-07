package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.SerialNumberAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for SerialNumberAuditHistory entity.
 * Provides methods to query audit history for serial numbers.
 */
@Repository
public interface SerialNumberAuditHistoryRepository extends JpaRepository<SerialNumberAuditHistory, Long> {

    /**
     * Find all audit history for a specific serial number, ordered by timestamp descending
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.serialNumberId = :serialNumberId ORDER BY h.thoiGianThayDoi DESC")
    List<SerialNumberAuditHistory> findBySerialNumberIdOrderByThoiGianThayDoiDesc(@Param("serialNumberId") Long serialNumberId);

    /**
     * Find audit history for a specific serial number with pagination
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.serialNumberId = :serialNumberId ORDER BY h.thoiGianThayDoi DESC")
    Page<SerialNumberAuditHistory> findBySerialNumberId(@Param("serialNumberId") Long serialNumberId, Pageable pageable);

    /**
     * Find audit history by action type
     */
    List<SerialNumberAuditHistory> findByHanhDong(String hanhDong);

    /**
     * Find audit history by user
     */
    List<SerialNumberAuditHistory> findByNguoiThucHien(String nguoiThucHien);

    /**
     * Find audit history by batch operation ID
     */
    List<SerialNumberAuditHistory> findByBatchOperationId(String batchOperationId);

    /**
     * Find audit history by order ID
     */
    List<SerialNumberAuditHistory> findByOrderId(String orderId);

    /**
     * Find audit history by channel
     */
    List<SerialNumberAuditHistory> findByChannel(String channel);

    /**
     * Find audit history within date range
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<SerialNumberAuditHistory> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find recent audit history (last N days)
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.thoiGianThayDoi >= :since ORDER BY h.thoiGianThayDoi DESC")
    List<SerialNumberAuditHistory> findRecentHistory(@Param("since") Instant since);

    /**
     * Find audit history for multiple serial numbers
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.serialNumberId IN :serialNumberIds ORDER BY h.thoiGianThayDoi DESC")
    List<SerialNumberAuditHistory> findBySerialNumberIds(@Param("serialNumberIds") List<Long> serialNumberIds);

    /**
     * Search audit history with filters
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE " +
           "(:serialNumberId IS NULL OR h.serialNumberId = :serialNumberId) AND " +
           "(:action IS NULL OR h.hanhDong = :action) AND " +
           "(:user IS NULL OR LOWER(h.nguoiThucHien) LIKE LOWER(CONCAT('%', :user, '%'))) AND " +
           "(:startDate IS NULL OR h.thoiGianThayDoi >= :startDate) AND " +
           "(:endDate IS NULL OR h.thoiGianThayDoi <= :endDate) AND " +
           "(:channel IS NULL OR h.channel = :channel) " +
           "ORDER BY h.thoiGianThayDoi DESC")
    Page<SerialNumberAuditHistory> searchAuditHistory(
        @Param("serialNumberId") Long serialNumberId,
        @Param("action") String action,
        @Param("user") String user,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("channel") String channel,
        Pageable pageable
    );

    /**
     * Get audit statistics by action type
     */
    @Query("SELECT h.hanhDong, COUNT(h) FROM SerialNumberAuditHistory h GROUP BY h.hanhDong ORDER BY COUNT(h) DESC")
    List<Object[]> getAuditStatsByAction();

    /**
     * Get audit statistics by user
     */
    @Query("SELECT h.nguoiThucHien, COUNT(h) FROM SerialNumberAuditHistory h WHERE h.nguoiThucHien IS NOT NULL GROUP BY h.nguoiThucHien ORDER BY COUNT(h) DESC")
    List<Object[]> getAuditStatsByUser();

    /**
     * Get audit statistics by channel
     */
    @Query("SELECT h.channel, COUNT(h) FROM SerialNumberAuditHistory h WHERE h.channel IS NOT NULL GROUP BY h.channel ORDER BY COUNT(h) DESC")
    List<Object[]> getAuditStatsByChannel();

    /**
     * Get daily audit activity
     */
    @Query(value = """
        SELECT DATE(thoi_gian_thay_doi) as audit_date, 
               hanh_dong, 
               COUNT(*) as count
        FROM serial_number_audit_history 
        WHERE thoi_gian_thay_doi >= :startDate 
        GROUP BY DATE(thoi_gian_thay_doi), hanh_dong 
        ORDER BY audit_date DESC, count DESC
        """, nativeQuery = true)
    List<Object[]> getDailyAuditActivity(@Param("startDate") Instant startDate);

    /**
     * Find suspicious activities (multiple actions by same user in short time)
     */
    @Query(value = """
        SELECT nguoi_thuc_hien, 
               COUNT(*) as action_count,
               MIN(thoi_gian_thay_doi) as first_action,
               MAX(thoi_gian_thay_doi) as last_action
        FROM serial_number_audit_history 
        WHERE thoi_gian_thay_doi >= :since
        GROUP BY nguoi_thuc_hien
        HAVING COUNT(*) > :threshold
        ORDER BY action_count DESC
        """, nativeQuery = true)
    List<Object[]> findSuspiciousActivity(@Param("since") Instant since, @Param("threshold") Integer threshold);

    /**
     * Get batch operation summary
     */
    @Query("SELECT h.batchOperationId, h.hanhDong, COUNT(h), MIN(h.thoiGianThayDoi), MAX(h.thoiGianThayDoi) " +
           "FROM SerialNumberAuditHistory h " +
           "WHERE h.batchOperationId IS NOT NULL " +
           "GROUP BY h.batchOperationId, h.hanhDong " +
           "ORDER BY MIN(h.thoiGianThayDoi) DESC")
    List<Object[]> getBatchOperationSummary();

    /**
     * Find failed operations (based on metadata or specific patterns)
     */
    @Query(value = """
    SELECT * FROM serial_number_audit_history 
    WHERE 
        CAST(metadata AS TEXT) ILIKE '%error%' OR 
        CAST(metadata AS TEXT) ILIKE '%failed%' OR 
        ly_do_thay_doi ILIKE '%error%' OR 
        ly_do_thay_doi ILIKE '%failed%'
    ORDER BY thoi_gian_thay_doi DESC
    """, nativeQuery = true)
    List<SerialNumberAuditHistory> findFailedOperations();


    /**
     * Get audit trail for a specific order
     */
    @Query("SELECT h FROM SerialNumberAuditHistory h WHERE h.orderId = :orderId ORDER BY h.thoiGianThayDoi ASC")
    List<SerialNumberAuditHistory> getOrderAuditTrail(@Param("orderId") String orderId);

    /**
     * Count audit entries by serial number
     */
    @Query("SELECT COUNT(h) FROM SerialNumberAuditHistory h WHERE h.serialNumberId = :serialNumberId")
    long countBySerialNumberId(@Param("serialNumberId") Long serialNumberId);

    /**
     * Find most active serial numbers (most audit entries)
     */
    @Query("SELECT h.serialNumberId, COUNT(h) as audit_count " +
           "FROM SerialNumberAuditHistory h " +
           "GROUP BY h.serialNumberId " +
           "ORDER BY audit_count DESC")
    List<Object[]> findMostActiveSerialNumbers(Pageable pageable);

    /**
     * Get audit summary for dashboard
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_actions,
            COUNT(DISTINCT serial_number_id) as affected_serials,
            COUNT(DISTINCT nguoi_thuc_hien) as active_users,
            COUNT(DISTINCT hanh_dong) as action_types
        FROM serial_number_audit_history 
        WHERE thoi_gian_thay_doi >= :since
        """, nativeQuery = true)
    Object[] getAuditSummary(@Param("since") Instant since);

    /**
     * Delete old audit entries (for cleanup)
     */
    @Query("DELETE FROM SerialNumberAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditEntries(@Param("cutoffDate") Instant cutoffDate);
}
