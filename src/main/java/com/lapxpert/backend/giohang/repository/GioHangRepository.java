package com.lapxpert.backend.giohang.repository;

import com.lapxpert.backend.giohang.entity.GioHang;
import com.lapxpert.backend.nguoidung.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GioHang entity
 * Provides data access methods for shopping cart operations
 * Follows established repository patterns with Vietnamese naming conventions
 */
@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Long> {

    /**
     * Find cart by user ID
     * @param nguoiDungId user ID
     * @return Optional cart for the user
     */
    Optional<GioHang> findByNguoiDung_Id(Long nguoiDungId);

    /**
     * Find cart by user email
     * @param email user email
     * @return Optional cart for the user
     */
    Optional<GioHang> findByNguoiDung_Email(String email);

    /**
     * Check if user has a cart
     * @param nguoiDungId user ID
     * @return true if user has a cart
     */
    boolean existsByNguoiDung_Id(Long nguoiDungId);

    /**
     * Find carts that haven't been updated for a specified period (for cleanup)
     * @param cutoffDate date before which carts are considered stale
     * @return list of stale carts
     */
    @Query("SELECT g FROM GioHang g WHERE g.ngayCapNhat < :cutoffDate")
    List<GioHang> findStaleCartsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find carts with items count
     * @return list of carts that have items
     */
    @Query("SELECT g FROM GioHang g WHERE SIZE(g.chiTiets) > 0")
    List<GioHang> findCartsWithItems();

    /**
     * Find empty carts (for cleanup)
     * @return list of empty carts
     */
    @Query("SELECT g FROM GioHang g WHERE SIZE(g.chiTiets) = 0")
    List<GioHang> findEmptyCarts();

    /**
     * Count total items across all carts
     * @return total number of items in all carts
     */
    @Query("SELECT COALESCE(SUM(c.soLuong), 0) FROM GioHangChiTiet c")
    Long countTotalItemsInAllCarts();

    /**
     * Find carts by user role (for analytics)
     * @param vaiTro user role
     * @return list of carts for users with specified role
     */
    @Query("SELECT g FROM GioHang g WHERE g.nguoiDung.vaiTro = :vaiTro")
    List<GioHang> findCartsByUserRole(@Param("vaiTro") VaiTro vaiTro);

    /**
     * Find carts updated within a time range
     * @param startDate start date
     * @param endDate end date
     * @return list of carts updated within the range
     */
    @Query("SELECT g FROM GioHang g WHERE g.ngayCapNhat BETWEEN :startDate AND :endDate")
    List<GioHang> findCartsUpdatedBetween(@Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    /**
     * Get cart statistics
     * @return array with [total_carts, carts_with_items, empty_carts]
     */
    @Query("SELECT COUNT(g), " +
           "SUM(CASE WHEN SIZE(g.chiTiets) > 0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN SIZE(g.chiTiets) = 0 THEN 1 ELSE 0 END) " +
           "FROM GioHang g")
    Object[] getCartStatistics();

    /**
     * Find carts containing a specific product
     * @param sanPhamChiTietId product variant ID
     * @return list of carts containing the product
     */
    @Query("SELECT DISTINCT g FROM GioHang g JOIN g.chiTiets c WHERE c.sanPhamChiTiet.id = :sanPhamChiTietId")
    List<GioHang> findCartsContainingProduct(@Param("sanPhamChiTietId") Long sanPhamChiTietId);

    /**
     * Find carts with total value greater than specified amount
     * @param minAmount minimum cart value
     * @return list of high-value carts
     */
    @Query("SELECT g FROM GioHang g WHERE " +
           "(SELECT COALESCE(SUM(c.soLuong * c.giaTaiThoiDiemThem), 0) FROM GioHangChiTiet c WHERE c.gioHang = g) >= :minAmount")
    List<GioHang> findCartsWithValueGreaterThan(@Param("minAmount") java.math.BigDecimal minAmount);

    /**
     * Delete empty carts older than specified date
     * @param cutoffDate date before which empty carts should be deleted
     * @return number of deleted carts
     */
    @Query("DELETE FROM GioHang g WHERE SIZE(g.chiTiets) = 0 AND g.ngayCapNhat < :cutoffDate")
    int deleteEmptyCartsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}
