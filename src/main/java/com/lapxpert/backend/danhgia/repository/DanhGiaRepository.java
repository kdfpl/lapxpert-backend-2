package com.lapxpert.backend.danhgia.repository;

import com.lapxpert.backend.common.enums.TrangThaiDanhGia;
import com.lapxpert.backend.danhgia.entity.DanhGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DanhGia entity
 * Provides comprehensive data access methods for review management
 * Includes rating aggregation, filtering, and moderation queries
 */
@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {

    // ==================== BASIC QUERIES ====================

    /**
     * Find reviews by customer and status
     * @param nguoiDungId customer ID
     * @param trangThai review status
     * @return list of reviews
     */
    List<DanhGia> findByNguoiDungIdAndTrangThai(Long nguoiDungId, TrangThaiDanhGia trangThai);

    /**
     * Find all reviews by customer (all statuses)
     * @param nguoiDungId customer ID
     * @return list of reviews
     */
    List<DanhGia> findByNguoiDungId(Long nguoiDungId);

    /**
     * Find reviews by product and status with pagination
     * @param sanPhamId product ID
     * @param trangThai review status
     * @param pageable pagination parameters
     * @return page of reviews
     */
    Page<DanhGia> findBySanPhamIdAndTrangThai(Long sanPhamId, TrangThaiDanhGia trangThai, Pageable pageable);

    /**
     * Find review by order item ID (unique constraint)
     * @param hoaDonChiTietId order item ID
     * @return optional review
     */
    Optional<DanhGia> findByHoaDonChiTietId(Long hoaDonChiTietId);

    /**
     * Check if review exists for order item
     * @param hoaDonChiTietId order item ID
     * @return true if review exists
     */
    boolean existsByHoaDonChiTietId(Long hoaDonChiTietId);

    // ==================== RATING AGGREGATION QUERIES ====================

    /**
     * Calculate average rating for a product
     * @param sanPhamId product ID
     * @param trangThai review status (typically DA_DUYET)
     * @return average rating or empty if no reviews
     */
    @Query("SELECT AVG(d.diemDanhGia) FROM DanhGia d WHERE d.sanPham.id = :sanPhamId AND d.trangThai = :trangThai")
    Optional<Double> calculateAverageRating(@Param("sanPhamId") Long sanPhamId, 
                                          @Param("trangThai") TrangThaiDanhGia trangThai);

    /**
     * Count reviews for a product by status
     * @param sanPhamId product ID
     * @param trangThai review status
     * @return review count
     */
    @Query("SELECT COUNT(d) FROM DanhGia d WHERE d.sanPham.id = :sanPhamId AND d.trangThai = :trangThai")
    Long countReviewsByProduct(@Param("sanPhamId") Long sanPhamId, 
                              @Param("trangThai") TrangThaiDanhGia trangThai);

    /**
     * Get rating distribution for a product (count by star rating)
     * @param sanPhamId product ID
     * @param trangThai review status
     * @return list of [rating, count] pairs
     */
    @Query("SELECT d.diemDanhGia, COUNT(d) FROM DanhGia d WHERE d.sanPham.id = :sanPhamId AND d.trangThai = :trangThai GROUP BY d.diemDanhGia ORDER BY d.diemDanhGia")
    List<Object[]> getRatingDistribution(@Param("sanPhamId") Long sanPhamId, 
                                        @Param("trangThai") TrangThaiDanhGia trangThai);

    /**
     * Batch calculate ratings for multiple products
     * @param productIds list of product IDs
     * @param trangThai review status
     * @return list of [productId, avgRating, count] tuples
     */
    @Query("SELECT d.sanPham.id, AVG(d.diemDanhGia), COUNT(d) " +
           "FROM DanhGia d WHERE d.sanPham.id IN :productIds AND d.trangThai = :trangThai " +
           "GROUP BY d.sanPham.id")
    List<Object[]> calculateBatchRatings(@Param("productIds") List<Long> productIds,
                                        @Param("trangThai") TrangThaiDanhGia trangThai);

    // ==================== ADVANCED FILTERING QUERIES ====================

    /**
     * Find reviews with comprehensive filtering
     * @param sanPhamId product ID (optional)
     * @param nguoiDungId customer ID (optional)
     * @param trangThai review status (optional)
     * @param minRating minimum rating (optional)
     * @param maxRating maximum rating (optional)
     * @param fromDate start date (optional)
     * @param toDate end date (optional)
     * @param pageable pagination parameters
     * @return page of filtered reviews
     */
    @Query("SELECT d FROM DanhGia d WHERE " +
           "(:sanPhamId IS NULL OR d.sanPham.id = :sanPhamId) AND " +
           "(:nguoiDungId IS NULL OR d.nguoiDung.id = :nguoiDungId) AND " +
           "(:trangThai IS NULL OR d.trangThai = :trangThai) AND " +
           "(:minRating IS NULL OR d.diemDanhGia >= :minRating) AND " +
           "(:maxRating IS NULL OR d.diemDanhGia <= :maxRating) AND " +
           "(:fromDate IS NULL OR d.ngayTao >= :fromDate) AND " +
           "(:toDate IS NULL OR d.ngayTao <= :toDate)")
    Page<DanhGia> findWithFilters(@Param("sanPhamId") Long sanPhamId,
                                 @Param("nguoiDungId") Long nguoiDungId,
                                 @Param("trangThai") TrangThaiDanhGia trangThai,
                                 @Param("minRating") Integer minRating,
                                 @Param("maxRating") Integer maxRating,
                                 @Param("fromDate") Instant fromDate,
                                 @Param("toDate") Instant toDate,
                                 Pageable pageable);

    /**
     * Find reviews by product with optimized fetch joins for display
     * @param sanPhamId product ID
     * @param trangThai review status
     * @param pageable pagination parameters
     * @return page of reviews with loaded relationships
     */
    @Query("SELECT d FROM DanhGia d " +
           "LEFT JOIN FETCH d.nguoiDung " +
           "WHERE d.sanPham.id = :sanPhamId AND d.trangThai = :trangThai " +
           "ORDER BY d.ngayTao DESC")
    Page<DanhGia> findBySanPhamWithDetails(@Param("sanPhamId") Long sanPhamId,
                                          @Param("trangThai") TrangThaiDanhGia trangThai,
                                          Pageable pageable);

    // ==================== MODERATION QUERIES ====================

    /**
     * Find reviews by status ordered by creation date (for moderation queue)
     * @param trangThai review status
     * @return list of reviews ordered by creation date
     */
    List<DanhGia> findByTrangThaiOrderByNgayTaoAsc(TrangThaiDanhGia trangThai);

    /**
     * Find pending reviews older than specified date (for auto-processing)
     * @param trangThai review status
     * @param cutoffDate cutoff date
     * @return list of old pending reviews
     */
    @Query("SELECT d FROM DanhGia d WHERE d.trangThai = :trangThai AND d.ngayTao < :cutoffDate")
    List<DanhGia> findPendingReviewsOlderThan(@Param("trangThai") TrangThaiDanhGia trangThai,
                                             @Param("cutoffDate") Instant cutoffDate);

    /**
     * Count reviews by status for admin dashboard
     * @param trangThai review status
     * @return count of reviews
     */
    Long countByTrangThai(TrangThaiDanhGia trangThai);

    // ==================== CUSTOMER HISTORY QUERIES ====================

    /**
     * Find customer reviews with pagination
     * @param nguoiDungId customer ID
     * @param pageable pagination parameters
     * @return page of customer reviews
     */
    Page<DanhGia> findByNguoiDungIdOrderByNgayTaoDesc(Long nguoiDungId, Pageable pageable);

    /**
     * Count customer reviews by status
     * @param nguoiDungId customer ID
     * @param trangThai review status
     * @return count of reviews
     */
    Long countByNguoiDungIdAndTrangThai(Long nguoiDungId, TrangThaiDanhGia trangThai);

    /**
     * Find recent reviews by customer (within specified days)
     * @param nguoiDungId customer ID
     * @param sinceDate date threshold
     * @return list of recent reviews
     */
    @Query("SELECT d FROM DanhGia d WHERE d.nguoiDung.id = :nguoiDungId AND d.ngayTao >= :sinceDate ORDER BY d.ngayTao DESC")
    List<DanhGia> findRecentReviewsByCustomer(@Param("nguoiDungId") Long nguoiDungId,
                                             @Param("sinceDate") Instant sinceDate);
}
