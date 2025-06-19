package com.lapxpert.backend.sanpham.repository;

import com.lapxpert.backend.sanpham.entity.SanPhamAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for SanPhamAuditHistory entity.
 * Provides methods to query audit history for products.
 */
@Repository
public interface SanPhamAuditHistoryRepository extends JpaRepository<SanPhamAuditHistory, Long> {

    /**
     * Find all audit history for a specific product, ordered by timestamp descending
     * @param sanPhamId ID of the product
     * @return List of audit history entries
     */
    @Query("SELECT h FROM SanPhamAuditHistory h WHERE h.sanPhamId = :sanPhamId ORDER BY h.thoiGianThayDoi DESC")
    List<SanPhamAuditHistory> findBySanPhamIdOrderByThoiGianThayDoiDesc(@Param("sanPhamId") Long sanPhamId);

    /**
     * Find audit history for a specific product with pagination
     * @param sanPhamId ID of the product
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<SanPhamAuditHistory> findBySanPhamIdOrderByThoiGianThayDoiDesc(Long sanPhamId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param sanPhamId ID of the product
     * @param hanhDong Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE, CATEGORY_CHANGE)
     * @return List of audit history entries
     */
    List<SanPhamAuditHistory> findBySanPhamIdAndHanhDongOrderByThoiGianThayDoiDesc(Long sanPhamId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param sanPhamId ID of the product
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM SanPhamAuditHistory h WHERE h.sanPhamId = :sanPhamId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<SanPhamAuditHistory> findBySanPhamIdAndDateRange(
            @Param("sanPhamId") Long sanPhamId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user
     * @param sanPhamId ID of the product
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<SanPhamAuditHistory> findBySanPhamIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long sanPhamId, String nguoiThucHien);

    /**
     * Count total audit entries for a product
     * @param sanPhamId ID of the product
     * @return Number of audit entries
     */
    long countBySanPhamId(Long sanPhamId);

    /**
     * Find recent audit history across all products (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM SanPhamAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<SanPhamAuditHistory> findRecentAuditHistory(Pageable pageable);

    /**
     * Find category change audit history
     * @param pageable Pagination information
     * @return Page of category change audit history entries
     */
    @Query("SELECT h FROM SanPhamAuditHistory h WHERE h.hanhDong = 'CATEGORY_CHANGE' ORDER BY h.thoiGianThayDoi DESC")
    Page<SanPhamAuditHistory> findCategoryChangeHistory(Pageable pageable);

    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM SanPhamAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
