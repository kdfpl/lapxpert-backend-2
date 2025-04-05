package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
    List<SanPham> findAllByTrangThai(Boolean trangThai);


    @Query(value = "SELECT s.ma_san_pham FROM san_pham s WHERE s.ma_san_pham LIKE 'SP%' ORDER BY s.id DESC LIMIT 1", nativeQuery = true)
    String findLastMaSanPham();

}