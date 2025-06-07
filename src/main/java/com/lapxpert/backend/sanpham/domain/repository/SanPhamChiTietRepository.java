package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {

    /**
     * Find all product variants with a specific status using Boolean.
     * This method is used for variant management and reporting.
     *
     * @param trangThai The status to filter by (true = active, false = inactive)
     * @return List of product variants with the specified status
     */
    List<SanPhamChiTiet> findAllByTrangThai(Boolean trangThai);

    /**
     * Find product variants by status (alias for findAllByTrangThai)
     */
    List<SanPhamChiTiet> findByTrangThai(Boolean trangThai);

    /**
     * Count product variants by status
     */
    long countByTrangThai(Boolean trangThai);

    /**
     * Find all active product variants.
     * Convenience method for finding variants that are available for sale.
     *
     * @return List of active product variants
     */
    default List<SanPhamChiTiet> findAllActive() {
        return findAllByTrangThai(true);
    }

    /**
     * Find active variants for a specific product.
     * Used for displaying available product configurations.
     *
     * @param sanPhamId The product ID
     * @return List of active variants for the product
     */
    @Query("SELECT spct FROM SanPhamChiTiet spct WHERE spct.sanPham.id = :sanPhamId AND spct.trangThai = true")
    List<SanPhamChiTiet> findActiveVariantsBySanPhamId(@Param("sanPhamId") Long sanPhamId);

    /**
     * Find a product variant by its SKU.
     * This method is used for variant identification and lookup.
     *
     * @param sku The unique SKU of the product variant
     * @return Optional containing the product variant if found
     */
    Optional<SanPhamChiTiet> findBySku(String sku);

    /**
     * Check if a SKU already exists (for uniqueness validation).
     *
     * @param sku The SKU to check
     * @return true if SKU exists, false otherwise
     */
    boolean existsBySku(String sku);

    /**
     * Count active variants for a specific product.
     * Used for product availability checks.
     *
     * @param sanPhamId The product ID
     * @return Count of active variants
     */
    @Query("SELECT COUNT(spct) FROM SanPhamChiTiet spct WHERE spct.sanPham.id = :sanPhamId AND spct.trangThai = :trangThai")
    long countBySanPhamIdAndTrangThai(@Param("sanPhamId") Long sanPhamId, @Param("trangThai") Boolean trangThai);

    /**
     * Count active variants for a specific product.
     * Convenience method for counting active variants.
     *
     * @param sanPhamId The product ID
     * @return Count of active variants
     */
    default long countActiveBySanPhamId(Long sanPhamId) {
        return countBySanPhamIdAndTrangThai(sanPhamId, true);
    }
}