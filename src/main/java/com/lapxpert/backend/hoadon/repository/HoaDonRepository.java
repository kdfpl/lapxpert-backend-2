package com.lapxpert.backend.hoadon.repository;

import com.lapxpert.backend.hoadon.enity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    List<HoaDon> findByTrangThaiGiaoHang(HoaDon.TrangThaiGiaoHang trangThaiGiaoHang);
}