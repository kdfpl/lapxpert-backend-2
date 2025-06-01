package com.lapxpert.backend.giohang.domain.repository;

import com.lapxpert.backend.giohang.domain.entity.GioHangChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GioHangChiTiet entity
 * Provides data access methods for cart item operations
 * Follows established repository patterns with Vietnamese naming conventions
 */
@Repository
public interface GioHangChiTietRepository extends JpaRepository<GioHangChiTiet, Long> {

    /**
     * Find cart item by cart ID and product variant ID
     * @param gioHangId cart ID
     * @param sanPhamChiTietId product variant ID
     * @return Optional cart item
     */
    Optional<GioHangChiTiet> findByGioHang_IdAndSanPhamChiTiet_Id(Long gioHangId, Long sanPhamChiTietId);

    /**
     * Find all cart items for a specific cart
     * @param gioHangId cart ID
     * @return list of cart items
     */
    List<GioHangChiTiet> findByGioHang_Id(Long gioHangId);

    /**
     * Find all cart items for a specific user
     * @param nguoiDungId user ID
     * @return list of cart items for the user
     */
    @Query("SELECT c FROM GioHangChiTiet c WHERE c.gioHang.nguoiDung.id = :nguoiDungId")
    List<GioHangChiTiet> findByUserId(@Param("nguoiDungId") Long nguoiDungId);

    /**
     * Find cart items containing a specific product variant
     * @param sanPhamChiTietId product variant ID
     * @return list of cart items containing the product
     */
    List<GioHangChiTiet> findBySanPhamChiTiet_Id(Long sanPhamChiTietId);

    /**
     * Find cart items with price changes (current price differs from cart price)
     * @return list of cart items with price changes
     */
    @Query("SELECT c FROM GioHangChiTiet c WHERE c.giaTaiThoiDiemThem != c.sanPhamChiTiet.giaBan")
    List<GioHangChiTiet> findItemsWithPriceChanges();

    /**
     * Find cart items for unavailable products
     * @return list of cart items for unavailable products
     */
    @Query("SELECT c FROM GioHangChiTiet c WHERE c.sanPhamChiTiet.trangThai != com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.AVAILABLE")
    List<GioHangChiTiet> findItemsWithUnavailableProducts();

    /**
     * Find cart items older than specified date
     * @param cutoffDate date before which items are considered stale
     * @return list of stale cart items
     */
    @Query("SELECT c FROM GioHangChiTiet c WHERE c.ngayTao < :cutoffDate")
    List<GioHangChiTiet> findStaleItemsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Calculate total value for a specific cart
     * @param gioHangId cart ID
     * @return total value of all items in the cart
     */
    @Query("SELECT COALESCE(SUM(c.soLuong * c.giaTaiThoiDiemThem), 0) FROM GioHangChiTiet c WHERE c.gioHang.id = :gioHangId")
    BigDecimal calculateCartTotal(@Param("gioHangId") Long gioHangId);

    /**
     * Calculate total quantity for a specific cart
     * @param gioHangId cart ID
     * @return total quantity of all items in the cart
     */
    @Query("SELECT COALESCE(SUM(c.soLuong), 0) FROM GioHangChiTiet c WHERE c.gioHang.id = :gioHangId")
    Integer calculateCartTotalQuantity(@Param("gioHangId") Long gioHangId);

    /**
     * Count unique products in a cart
     * @param gioHangId cart ID
     * @return number of unique products in the cart
     */
    @Query("SELECT COUNT(c) FROM GioHangChiTiet c WHERE c.gioHang.id = :gioHangId")
    Integer countUniqueProductsInCart(@Param("gioHangId") Long gioHangId);

    /**
     * Find cart items by product category
     * @param danhMucId category ID
     * @return list of cart items from the specified category
     */
    @Query("SELECT c FROM GioHangChiTiet c JOIN c.sanPhamChiTiet.sanPham sp JOIN sp.danhMucs dm WHERE dm.id = :danhMucId")
    List<GioHangChiTiet> findItemsByCategory(@Param("danhMucId") Long danhMucId);

    /**
     * Find cart items by brand
     * @param thuongHieuId brand ID
     * @return list of cart items from the specified brand
     */
    @Query("SELECT c FROM GioHangChiTiet c WHERE c.sanPhamChiTiet.sanPham.thuongHieu.id = :thuongHieuId")
    List<GioHangChiTiet> findItemsByBrand(@Param("thuongHieuId") Long thuongHieuId);

    /**
     * Find most popular products in carts (by quantity)
     * @param pageable pagination information to limit results
     * @return page of product IDs ordered by total quantity in carts
     */
    @Query("SELECT c.sanPhamChiTiet.id, SUM(c.soLuong) as totalQty " +
           "FROM GioHangChiTiet c " +
           "GROUP BY c.sanPhamChiTiet.id " +
           "ORDER BY totalQty DESC")
    Page<Object[]> findMostPopularProductsInCarts(Pageable pageable);

    /**
     * Delete cart items for unavailable products
     * @return number of deleted items
     */
    @Query("DELETE FROM GioHangChiTiet c WHERE c.sanPhamChiTiet.trangThai != com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham.AVAILABLE")
    int deleteItemsWithUnavailableProducts();

    /**
     * Delete cart items older than specified date
     * @param cutoffDate date before which items should be deleted
     * @return number of deleted items
     */
    @Query("DELETE FROM GioHangChiTiet c WHERE c.ngayTao < :cutoffDate")
    int deleteItemsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Check if cart contains a specific product variant
     * @param gioHangId cart ID
     * @param sanPhamChiTietId product variant ID
     * @return true if cart contains the product
     */
    boolean existsByGioHang_IdAndSanPhamChiTiet_Id(Long gioHangId, Long sanPhamChiTietId);

    /**
     * Get cart item statistics
     * @return array with [total_items, total_quantity, average_quantity_per_item]
     */
    @Query("SELECT COUNT(c), SUM(c.soLuong), AVG(c.soLuong) FROM GioHangChiTiet c")
    Object[] getCartItemStatistics();
}
