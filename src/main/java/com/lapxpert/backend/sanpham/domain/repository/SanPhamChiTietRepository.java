package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {

    /**
     * Find all product items with a specific status using enum.
     * This method is used for inventory management and reporting.
     *
     * @param trangThai The status to filter by using TrangThaiSanPham enum
     * @return List of product items with the specified status
     */
    List<SanPhamChiTiet> findAllByTrangThai(TrangThaiSanPham trangThai);

    /**
     * Find product items by status (alias for findAllByTrangThai)
     */
    List<SanPhamChiTiet> findByTrangThai(TrangThaiSanPham trangThai);

    /**
     * Count product items by status
     */
    long countByTrangThai(TrangThaiSanPham trangThai);

    /**
     * Count product items by status and channel
     */
    long countByTrangThaiAndKenhDatTruoc(TrangThaiSanPham trangThai, String kenhDatTruoc);

    /**
     * Find all available product items.
     * Convenience method for finding items that can be sold.
     *
     * @return List of available product items
     */
    default List<SanPhamChiTiet> findAllAvailable() {
        return findAllByTrangThai(TrangThaiSanPham.AVAILABLE);
    }

    /**
     * Find available individual items for a specific product variant (configuration).
     * In the current system, each SanPhamChiTiet represents an individual trackable item.
     * This query finds available items that match the same product configuration.
     *
     * @param productVariantId The ID of a SanPhamChiTiet that represents the desired configuration
     * @param limit Maximum number of items to return
     * @return List of available individual items with the same configuration
     */
    @Query(value = """
        SELECT spct.* FROM san_pham_chi_tiet spct
        WHERE spct.san_pham_id = (
            SELECT ref.san_pham_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        )
        AND spct.mau_sac_id = (
            SELECT ref.mau_sac_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        )
        AND COALESCE(spct.cpu_id, 0) = COALESCE((
            SELECT ref.cpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.ram_id, 0) = COALESCE((
            SELECT ref.ram_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.o_cung_id, 0) = COALESCE((
            SELECT ref.o_cung_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.gpu_id, 0) = COALESCE((
            SELECT ref.gpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND spct.trang_thai = 'AVAILABLE'
        ORDER BY spct.ngay_tao ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<SanPhamChiTiet> findAvailableItemsByProductVariant(
        @Param("productVariantId") Long productVariantId,
        @Param("limit") Integer limit
    );

    /**
     * Count available items for a specific product variant.
     *
     * @param productVariantId The ID of a SanPhamChiTiet that represents the desired configuration
     * @return Number of available items with the same configuration
     */
    @Query(value = """
        SELECT COUNT(*) FROM san_pham_chi_tiet spct
        WHERE spct.san_pham_id = (
            SELECT ref.san_pham_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        )
        AND spct.mau_sac_id = (
            SELECT ref.mau_sac_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        )
        AND COALESCE(spct.cpu_id, 0) = COALESCE((
            SELECT ref.cpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.ram_id, 0) = COALESCE((
            SELECT ref.ram_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.o_cung_id, 0) = COALESCE((
            SELECT ref.o_cung_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND COALESCE(spct.gpu_id, 0) = COALESCE((
            SELECT ref.gpu_id FROM san_pham_chi_tiet ref WHERE ref.id = :productVariantId
        ), 0)
        AND spct.trang_thai = 'AVAILABLE'
        """, nativeQuery = true)
    int countAvailableItemsByProductVariant(@Param("productVariantId") Long productVariantId);

    /**
     * Find a product item by its serial number.
     * This method is used for inventory tracking and order processing.
     *
     * @param serialNumber The unique serial number of the product item
     * @return Optional containing the product item if found
     */
    Optional<SanPhamChiTiet> findBySerialNumber(String serialNumber);

    /**
     * Check if a serial number already exists (for uniqueness validation).
     *
     * @param serialNumber The serial number to check
     * @return true if serial number exists, false otherwise
     */
    boolean existsBySerialNumber(String serialNumber);

    /**
     * Count available items for a specific product.
     * Used for inventory availability checks.
     *
     * @param sanPhamId The product ID
     * @return Count of available items
     */
    @Query("SELECT COUNT(spct) FROM SanPhamChiTiet spct WHERE spct.sanPham.id = :sanPhamId AND spct.trangThai = :trangThai")
    long countBySanPhamIdAndTrangThai(@Param("sanPhamId") Long sanPhamId, @Param("trangThai") TrangThaiSanPham trangThai);

    /**
     * Count available items for a specific product.
     * Convenience method for counting available items.
     *
     * @param sanPhamId The product ID
     * @return Count of available items
     */
    default long countAvailableBySanPhamId(Long sanPhamId) {
        return countBySanPhamIdAndTrangThai(sanPhamId, TrangThaiSanPham.AVAILABLE);
    }
}