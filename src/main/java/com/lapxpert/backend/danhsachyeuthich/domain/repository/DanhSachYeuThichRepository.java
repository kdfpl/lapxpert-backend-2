package com.lapxpert.backend.danhsachyeuthich.domain.repository;

import com.lapxpert.backend.danhsachyeuthich.domain.entity.DanhSachYeuThich;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
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
 * Repository for DanhSachYeuThich entity
 * Provides data access methods for wishlist operations
 */
@Repository
public interface DanhSachYeuThichRepository extends JpaRepository<DanhSachYeuThich, Long> {

    /**
     * Find all wishlist items for a specific user
     * @param nguoiDung the user
     * @return list of wishlist items
     */
    List<DanhSachYeuThich> findByNguoiDung(NguoiDung nguoiDung);

    /**
     * Find all wishlist items for a specific user with pagination
     * @param nguoiDung the user
     * @param pageable pagination information
     * @return page of wishlist items
     */
    Page<DanhSachYeuThich> findByNguoiDung(NguoiDung nguoiDung, Pageable pageable);

    /**
     * Find all wishlist items for a specific user by user ID
     * @param nguoiDungId the user ID
     * @return list of wishlist items
     */
    List<DanhSachYeuThich> findByNguoiDungId(Long nguoiDungId);

    /**
     * Find all wishlist items for a specific user by user ID with pagination
     * @param nguoiDungId the user ID
     * @param pageable pagination information
     * @return page of wishlist items
     */
    Page<DanhSachYeuThich> findByNguoiDungId(Long nguoiDungId, Pageable pageable);

    /**
     * Check if a product is in user's wishlist
     * @param nguoiDungId the user ID
     * @param sanPhamId the product ID
     * @return true if product is in wishlist
     */
    boolean existsByNguoiDungIdAndSanPhamId(Long nguoiDungId, Long sanPhamId);

    /**
     * Find specific wishlist item by user and product
     * @param nguoiDungId the user ID
     * @param sanPhamId the product ID
     * @return optional wishlist item
     */
    Optional<DanhSachYeuThich> findByNguoiDungIdAndSanPhamId(Long nguoiDungId, Long sanPhamId);

    /**
     * Find wishlist items for available products only
     * @param nguoiDungId the user ID
     * @return list of wishlist items with available products
     */
    @Query("SELECT d FROM DanhSachYeuThich d WHERE d.nguoiDung.id = :nguoiDungId AND d.sanPham.trangThai = true")
    List<DanhSachYeuThich> findByNguoiDungIdAndAvailableProducts(@Param("nguoiDungId") Long nguoiDungId);

    /**
     * Find wishlist items with price drops
     * @param nguoiDungId the user ID
     * @return list of wishlist items where current price is lower than when added
     */
    @Query("SELECT d FROM DanhSachYeuThich d JOIN d.sanPham sp " +
           "WHERE d.nguoiDung.id = :nguoiDungId " +
           "AND EXISTS (SELECT 1 FROM SanPhamChiTiet spc WHERE spc.sanPham = sp " +
           "AND spc.giaBan < (SELECT MIN(spc2.giaBan) FROM SanPhamChiTiet spc2 WHERE spc2.sanPham = sp))")
    List<DanhSachYeuThich> findItemsWithPriceDrops(@Param("nguoiDungId") Long nguoiDungId);

    /**
     * Count wishlist items for a user
     * @param nguoiDungId the user ID
     * @return count of wishlist items
     */
    long countByNguoiDungId(Long nguoiDungId);

    /**
     * Find wishlist items added within a date range
     * @param nguoiDungId the user ID
     * @param startDate start date
     * @param endDate end date
     * @return list of wishlist items
     */
    @Query("SELECT d FROM DanhSachYeuThich d WHERE d.nguoiDung.id = :nguoiDungId " +
           "AND d.ngayTao BETWEEN :startDate AND :endDate")
    List<DanhSachYeuThich> findByNguoiDungIdAndDateRange(@Param("nguoiDungId") Long nguoiDungId,
                                                         @Param("startDate") Instant startDate,
                                                         @Param("endDate") Instant endDate);

    /**
     * Find most recently added wishlist items for a user
     * @param nguoiDungId the user ID
     * @param pageable pagination information
     * @return page of recent wishlist items
     */
    @Query("SELECT d FROM DanhSachYeuThich d WHERE d.nguoiDung.id = :nguoiDungId ORDER BY d.ngayTao DESC")
    Page<DanhSachYeuThich> findRecentByNguoiDungId(@Param("nguoiDungId") Long nguoiDungId, Pageable pageable);

    /**
     * Find wishlist items by product category
     * @param nguoiDungId the user ID
     * @param categoryId the category ID
     * @return list of wishlist items in the category
     */
    @Query("SELECT d FROM DanhSachYeuThich d JOIN d.sanPham sp JOIN sp.danhMucs dm " +
           "WHERE d.nguoiDung.id = :nguoiDungId AND dm.id = :categoryId")
    List<DanhSachYeuThich> findByNguoiDungIdAndCategory(@Param("nguoiDungId") Long nguoiDungId,
                                                        @Param("categoryId") Long categoryId);

    /**
     * Delete wishlist item by user and product
     * @param nguoiDungId the user ID
     * @param sanPhamId the product ID
     */
    void deleteByNguoiDungIdAndSanPhamId(Long nguoiDungId, Long sanPhamId);

    /**
     * Delete all wishlist items for a user
     * @param nguoiDungId the user ID
     */
    void deleteByNguoiDungId(Long nguoiDungId);

    /**
     * Find popular products in wishlists (for analytics)
     * @param pageable pagination information to limit results
     * @return list of product IDs ordered by wishlist frequency
     */
    @Query("SELECT d.sanPham.id, COUNT(d) as wishlistCount FROM DanhSachYeuThich d " +
           "GROUP BY d.sanPham.id ORDER BY wishlistCount DESC")
    Page<Object[]> findMostWishlistedProducts(Pageable pageable);
}
