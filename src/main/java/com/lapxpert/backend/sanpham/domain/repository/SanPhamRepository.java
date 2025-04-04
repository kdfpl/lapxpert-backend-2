package com.lapxpert.backend.sanpham.domain.repository;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
}