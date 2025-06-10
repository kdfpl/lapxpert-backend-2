package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.SerialNumber;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSerialNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SerialNumber entity.
 * Provides comprehensive methods for serial number management and inventory tracking.
 */
@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, Long> {

    // Basic CRUD and Search Operations

    /**
     * Find serial number by its value
     */
    Optional<SerialNumber> findBySerialNumberValue(String serialNumberValue);

    /**
     * Check if serial number value exists
     */
    boolean existsBySerialNumberValue(String serialNumberValue);

    /**
     * Find all serial numbers for a specific product variant
     */
    List<SerialNumber> findBySanPhamChiTietId(Long sanPhamChiTietId);

    /**
     * Find all serial numbers with specific status
     */
    List<SerialNumber> findByTrangThai(TrangThaiSerialNumber trangThai);

    /**
     * Find serial numbers by product variant and status
     */
    List<SerialNumber> findBySanPhamChiTietIdAndTrangThai(Long sanPhamChiTietId, TrangThaiSerialNumber trangThai);

    /**
     * Find serial numbers by multiple statuses
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE sn.trangThai IN :statuses")
    List<SerialNumber> findByTrangThaiIn(@Param("statuses") List<TrangThaiSerialNumber> statuses);

    // Inventory Management Queries

    /**
     * Count serial numbers by status
     */
    long countByTrangThai(TrangThaiSerialNumber trangThai);

    /**
     * Count available serial numbers for a specific product variant
     */
    @Query("SELECT COUNT(sn) FROM SerialNumber sn WHERE sn.sanPhamChiTiet.id = :variantId AND sn.trangThai = :status")
    long countByVariantAndStatus(@Param("variantId") Long variantId, @Param("status") TrangThaiSerialNumber status);

    /**
     * Count available serial numbers for a specific product variant (convenience method)
     */
    default long countAvailableByVariant(Long variantId) {
        return countByVariantAndStatus(variantId, TrangThaiSerialNumber.AVAILABLE);
    }

    /**
     * Count total serial numbers for a specific product variant
     */
    default long countByVariantId(Long variantId) {
        return countBySanPhamChiTietId(variantId);
    }

    /**
     * Count serial numbers by variant ID
     */
    long countBySanPhamChiTietId(Long sanPhamChiTietId);

    /**
     * Count reserved serial numbers for a specific product variant
     */
    default long countReservedByVariant(Long variantId) {
        return countByVariantAndStatus(variantId, TrangThaiSerialNumber.RESERVED);
    }

    /**
     * Count sold serial numbers for a specific product variant
     */
    default long countSoldByVariant(Long variantId) {
        return countByVariantAndStatus(variantId, TrangThaiSerialNumber.SOLD);
    }

    /**
     * Find available serial numbers for a specific product variant with limit
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE sn.sanPhamChiTiet.id = :variantId AND sn.trangThai = 'AVAILABLE' ORDER BY sn.ngayTao ASC")
    List<SerialNumber> findAvailableByVariant(@Param("variantId") Long variantId, Pageable pageable);

    /**
     * Find available serial numbers for similar product variants (same specs)
     */
    @Query(value = """
        SELECT sn.* FROM serial_number sn
        JOIN san_pham_chi_tiet spct ON sn.san_pham_chi_tiet_id = spct.id
        WHERE spct.san_pham_id = (
            SELECT ref.san_pham_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        )
        AND COALESCE(spct.mau_sac_id, 0) = COALESCE((
            SELECT ref.mau_sac_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        ), 0)
        AND COALESCE(spct.cpu_id, 0) = COALESCE((
            SELECT ref.cpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        ), 0)
        AND COALESCE(spct.ram_id, 0) = COALESCE((
            SELECT ref.ram_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        ), 0)
        AND COALESCE(spct.bo_nho_id, 0) = COALESCE((
            SELECT ref.bo_nho_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        ), 0)
        AND COALESCE(spct.gpu_id, 0) = COALESCE((
            SELECT ref.gpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :variantId
        ), 0)
        AND sn.trang_thai = 'AVAILABLE'
        ORDER BY sn.ngay_tao ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<SerialNumber> findAvailableBySimilarVariant(@Param("variantId") Long variantId, @Param("limit") Integer limit);

    // Reservation Management

    /**
     * Find expired reservations
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE sn.trangThai = 'RESERVED' AND sn.thoiGianDatTruoc < :expiredBefore")
    List<SerialNumber> findExpiredReservations(@Param("expiredBefore") Instant expiredBefore);

    /**
     * Find reservations by channel
     */
    List<SerialNumber> findByKenhDatTruoc(String channel);

    /**
     * Count reservations by channel
     */
    long countByKenhDatTruoc(String channel);

    /**
     * Find reservations by order ID
     */
    List<SerialNumber> findByDonHangDatTruoc(String orderId);

    /**
     * Find reservations by order ID pattern (for temporary order cleanup)
     */
    List<SerialNumber> findByDonHangDatTruocStartingWith(String orderIdPrefix);

    /**
     * Find reservations by order ID and variant ID
     */
    List<SerialNumber> findByDonHangDatTruocAndSanPhamChiTiet_Id(String orderId, Long variantId);

    /**
     * Count reservations by order ID prefix and variant ID (for cart tracking)
     */
    @Query("SELECT COUNT(sn) FROM SerialNumber sn WHERE sn.donHangDatTruoc LIKE CONCAT(:orderIdPrefix, '%') AND sn.sanPhamChiTiet.id = :variantId")
    int countByDonHangDatTruocStartingWithAndSanPhamChiTiet_Id(@Param("orderIdPrefix") String orderIdPrefix, @Param("variantId") Long variantId);

    /**
     * Release expired reservations (bulk update)
     */
    @Modifying
    @Query("UPDATE SerialNumber sn SET sn.trangThai = 'AVAILABLE', sn.thoiGianDatTruoc = null, sn.kenhDatTruoc = null, sn.donHangDatTruoc = null WHERE sn.trangThai = 'RESERVED' AND sn.thoiGianDatTruoc < :expiredBefore")
    int releaseExpiredReservations(@Param("expiredBefore") Instant expiredBefore);

    // Batch Operations

    /**
     * Find serial numbers by import batch ID
     */
    List<SerialNumber> findByImportBatchId(String importBatchId);

    /**
     * Find serial numbers by batch number
     */
    List<SerialNumber> findByBatchNumber(String batchNumber);

    /**
     * Find serial numbers by supplier
     */
    List<SerialNumber> findByNhaCungCap(String nhaCungCap);

    // Advanced Search and Filtering

    /**
     * Search serial numbers with filters
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE " +
           "(:serialNumber IS NULL OR LOWER(sn.serialNumberValue) LIKE LOWER(CONCAT('%', :serialNumber, '%'))) AND " +
           "(:status IS NULL OR sn.trangThai = :status) AND " +
           "(:variantId IS NULL OR sn.sanPhamChiTiet.id = :variantId) AND " +
           "(:batchNumber IS NULL OR LOWER(sn.batchNumber) LIKE LOWER(CONCAT('%', :batchNumber, '%'))) AND " +
           "(:supplier IS NULL OR LOWER(sn.nhaCungCap) LIKE LOWER(CONCAT('%', :supplier, '%')))")
    Page<SerialNumber> searchSerialNumbers(
        @Param("serialNumber") String serialNumber,
        @Param("status") TrangThaiSerialNumber status,
        @Param("variantId") Long variantId,
        @Param("batchNumber") String batchNumber,
        @Param("supplier") String supplier,
        Pageable pageable
    );

    /**
     * Find serial numbers with warranty expiring soon
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE sn.ngayHetBaoHanh BETWEEN :startDate AND :endDate AND sn.trangThai IN ('AVAILABLE', 'RESERVED', 'SOLD')")
    List<SerialNumber> findWarrantyExpiringSoon(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // Statistics and Reporting

    /**
     * Get inventory statistics by status
     */
    @Query("SELECT sn.trangThai, COUNT(sn) FROM SerialNumber sn GROUP BY sn.trangThai")
    List<Object[]> getInventoryStatsByStatus();

    /**
     * Get inventory statistics by product variant
     */
    @Query("SELECT sn.sanPhamChiTiet.id, sn.trangThai, COUNT(sn) FROM SerialNumber sn GROUP BY sn.sanPhamChiTiet.id, sn.trangThai")
    List<Object[]> getInventoryStatsByVariant();

    /**
     * Get inventory statistics by supplier
     */
    @Query("SELECT sn.nhaCungCap, sn.trangThai, COUNT(sn) FROM SerialNumber sn WHERE sn.nhaCungCap IS NOT NULL GROUP BY sn.nhaCungCap, sn.trangThai")
    List<Object[]> getInventoryStatsBySupplier();

    /**
     * Get low stock variants (less than minimum threshold)
     */
    @Query(value = """
        SELECT spct.id, sp.ten_san_pham, COUNT(sn.id) as available_count
        FROM san_pham_chi_tiet spct
        JOIN san_pham sp ON spct.san_pham_id = sp.id
        LEFT JOIN serial_number sn ON spct.id = sn.san_pham_chi_tiet_id AND sn.trang_thai = 'AVAILABLE'
        WHERE sp.trang_thai = true
        GROUP BY spct.id, sp.ten_san_pham
        HAVING COUNT(sn.id) < :threshold
        ORDER BY COUNT(sn.id) ASC
        """, nativeQuery = true)
    List<Object[]> findLowStockVariants(@Param("threshold") Integer threshold);

    // Validation and Constraints

    /**
     * Find duplicate serial numbers (should not exist due to unique constraint)
     */
    @Query("SELECT sn.serialNumberValue, COUNT(sn) FROM SerialNumber sn GROUP BY sn.serialNumberValue HAVING COUNT(sn) > 1")
    List<Object[]> findDuplicateSerialNumbers();

    /**
     * Validate serial number format
     */
    @Query(value = "SELECT * FROM serial_number sn WHERE sn.serial_number_value !~ :pattern", nativeQuery = true)
    List<SerialNumber> findInvalidSerialNumberFormat(@Param("pattern") String pattern);

    // Bulk Operations Support

    /**
     * Find serial numbers for bulk status update
     */
    @Query("SELECT sn FROM SerialNumber sn WHERE sn.id IN :ids")
    List<SerialNumber> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * Bulk update status
     */
    @Modifying
    @Query("UPDATE SerialNumber sn SET sn.trangThai = :newStatus WHERE sn.id IN :ids AND sn.trangThai = :currentStatus")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("currentStatus") TrangThaiSerialNumber currentStatus, @Param("newStatus") TrangThaiSerialNumber newStatus);

    /**
     * Find next available serial number for a pattern
     */
    @Query(value = """
        SELECT sn.serial_number_value FROM serial_number sn 
        WHERE sn.serial_number_value LIKE :pattern 
        ORDER BY sn.serial_number_value DESC 
        LIMIT 1
        """, nativeQuery = true)
    Optional<String> findLastSerialNumberByPattern(@Param("pattern") String pattern);
}
