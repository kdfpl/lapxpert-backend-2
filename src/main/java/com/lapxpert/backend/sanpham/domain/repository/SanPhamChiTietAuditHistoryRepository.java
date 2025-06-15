package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.SanPhamChiTietAuditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for SanPhamChiTietAuditHistory entity.
 * Provides methods to query audit history for product variants.
 */
@Repository
public interface SanPhamChiTietAuditHistoryRepository extends JpaRepository<SanPhamChiTietAuditHistory, Long> {

    /**
     * Find all audit history for a specific product variant, ordered by timestamp descending
     * @param sanPhamChiTietId ID of the product variant
     * @return List of audit history entries
     */
    @Query("SELECT h FROM SanPhamChiTietAuditHistory h WHERE h.sanPhamChiTietId = :sanPhamChiTietId ORDER BY h.thoiGianThayDoi DESC")
    List<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdOrderByThoiGianThayDoiDesc(@Param("sanPhamChiTietId") Long sanPhamChiTietId);

    /**
     * Find audit history for a specific product variant with pagination
     * @param sanPhamChiTietId ID of the product variant
     * @param pageable Pagination information
     * @return Page of audit history entries
     */
    Page<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdOrderByThoiGianThayDoiDesc(Long sanPhamChiTietId, Pageable pageable);

    /**
     * Find audit history by action type
     * @param sanPhamChiTietId ID of the product variant
     * @param hanhDong Action type (CREATE, UPDATE, DELETE, STATUS_CHANGE, PRICE_CHANGE, DISCOUNT_ASSIGNMENT)
     * @return List of audit history entries
     */
    List<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdAndHanhDongOrderByThoiGianThayDoiDesc(Long sanPhamChiTietId, String hanhDong);

    /**
     * Find audit history within a date range
     * @param sanPhamChiTietId ID of the product variant
     * @param startDate Start date
     * @param endDate End date
     * @return List of audit history entries
     */
    @Query("SELECT h FROM SanPhamChiTietAuditHistory h WHERE h.sanPhamChiTietId = :sanPhamChiTietId " +
           "AND h.thoiGianThayDoi BETWEEN :startDate AND :endDate ORDER BY h.thoiGianThayDoi DESC")
    List<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdAndDateRange(
            @Param("sanPhamChiTietId") Long sanPhamChiTietId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find audit history by user
     * @param sanPhamChiTietId ID of the product variant
     * @param nguoiThucHien User who performed the action
     * @return List of audit history entries
     */
    List<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdAndNguoiThucHienOrderByThoiGianThayDoiDesc(Long sanPhamChiTietId, String nguoiThucHien);

    /**
     * Count total audit entries for a product variant
     * @param sanPhamChiTietId ID of the product variant
     * @return Number of audit entries
     */
    long countBySanPhamChiTietId(Long sanPhamChiTietId);

    /**
     * Find recent audit history across all product variants (for admin dashboard)
     * @param pageable Pagination information
     * @return Page of recent audit history entries
     */
    @Query("SELECT h FROM SanPhamChiTietAuditHistory h ORDER BY h.thoiGianThayDoi DESC")
    Page<SanPhamChiTietAuditHistory> findRecentAuditHistory(Pageable pageable);

    /**
     * Find price change audit history
     * @param pageable Pagination information
     * @return Page of price change audit history entries
     */
    @Query("SELECT h FROM SanPhamChiTietAuditHistory h WHERE h.hanhDong = 'PRICE_CHANGE' ORDER BY h.thoiGianThayDoi DESC")
    Page<SanPhamChiTietAuditHistory> findPriceChangeHistory(Pageable pageable);

    /**
     * Find discount assignment audit history
     * @param pageable Pagination information
     * @return Page of discount assignment audit history entries
     */
    @Query("SELECT h FROM SanPhamChiTietAuditHistory h WHERE h.hanhDong = 'DISCOUNT_ASSIGNMENT' ORDER BY h.thoiGianThayDoi DESC")
    Page<SanPhamChiTietAuditHistory> findDiscountAssignmentHistory(Pageable pageable);

    /**
     * Find audit history by multiple action types
     * @param sanPhamChiTietId ID of the product variant
     * @param hanhDongs List of action types
     * @return List of audit history entries
     */
    List<SanPhamChiTietAuditHistory> findBySanPhamChiTietIdAndHanhDongInOrderByThoiGianThayDoiDesc(Long sanPhamChiTietId, List<String> hanhDongs);

    /**
     * Delete old audit history entries (for data retention)
     * @param cutoffDate Date before which to delete entries
     * @return Number of deleted entries
     */
    @Query("DELETE FROM SanPhamChiTietAuditHistory h WHERE h.thoiGianThayDoi < :cutoffDate")
    int deleteOldAuditHistory(@Param("cutoffDate") Instant cutoffDate);
}
