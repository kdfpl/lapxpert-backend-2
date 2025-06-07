package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.HoaDonThanhToan;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonThanhToanId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonThanhToanRepository extends JpaRepository<HoaDonThanhToan, HoaDonThanhToanId> {
    // Add custom query methods if needed in the future
}
