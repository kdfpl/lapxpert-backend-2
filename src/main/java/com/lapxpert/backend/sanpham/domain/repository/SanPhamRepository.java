package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    List<SanPham> findAllByTrangThai(Boolean trangThai);

    @Query(value = "SELECT s.ma_san_pham FROM san_pham s WHERE s.ma_san_pham LIKE 'SP%' ORDER BY s.id DESC LIMIT 1", nativeQuery = true)
    String findLastMaSanPham();

    // Search products by name, code, or description
    @Query("SELECT s FROM SanPham s WHERE " +
           "(:tenSanPham IS NULL OR LOWER(s.tenSanPham) LIKE LOWER(CONCAT('%', :tenSanPham, '%'))) AND " +
           "(:maSanPham IS NULL OR LOWER(s.maSanPham) LIKE LOWER(CONCAT('%', :maSanPham, '%'))) AND " +
           "(:moTa IS NULL OR LOWER(s.moTa) LIKE LOWER(CONCAT('%', :moTa, '%'))) AND " +
           "s.trangThai = true")
    List<SanPham> searchProducts(@Param("tenSanPham") String tenSanPham,
                                @Param("maSanPham") String maSanPham,
                                @Param("moTa") String moTa);

}