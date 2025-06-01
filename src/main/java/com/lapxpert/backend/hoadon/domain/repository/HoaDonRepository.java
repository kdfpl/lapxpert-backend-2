package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    // Find invoices by order status
    List<HoaDon> findByTrangThaiDonHang(TrangThaiDonHang trangThaiDonHang);

    // Find invoices by customer's email
    List<HoaDon> findByKhachHang_Email(String email);

    /**
     * Get customer ID for an order without fetching full entities
     */
    @Query("SELECT h.khachHang.id FROM HoaDon h WHERE h.id = :orderId")
    Long findCustomerIdByOrderId(@Param("orderId") Long orderId);

    // ==================== STATISTICS METHODS ====================

    /**
     * Count orders by status
     */
    Long countByTrangThaiDonHang(TrangThaiDonHang trangThaiDonHang);

    /**
     * Count orders by type (online/POS)
     */
    Long countByLoaiHoaDon(LoaiHoaDon loaiHoaDon);

    /**
     * Find orders by date range and status
     */
    List<HoaDon> findByNgayTaoBetweenAndTrangThaiDonHang(Instant startDate, Instant endDate, TrangThaiDonHang trangThaiDonHang);

    /**
     * Count orders by date range
     */
    Long countByNgayTaoBetween(Instant startDate, Instant endDate);
}