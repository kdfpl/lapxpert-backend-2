package com.lapxpert.backend.hoadon.repository;

import com.lapxpert.backend.hoadon.enity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {
    // Các phương thức truy vấn tùy chỉnh nếu cần (Ví dụ: tìm lịch sử theo hoa_don_id)
    List<LichSuHoaDon> findByHoaDon_IdOrderByThoiGianDesc(Long hoaDonId);

}

