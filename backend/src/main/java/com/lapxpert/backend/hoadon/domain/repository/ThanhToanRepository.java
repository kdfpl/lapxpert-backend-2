package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {
    Optional<ThanhToan> findByMaGiaoDich(String maGiaoDich);
    List<ThanhToan> findByNguoiDung_Id(Long nguoiDungId);
    // Potentially add methods to find by trangThaiGiaoDich or phuongThucThanhToan
}
