package com.lapxpert.backend.hoadon.repository;

import com.lapxpert.backend.hoadon.entity.HoaDonThanhToan;
import com.lapxpert.backend.hoadon.entity.HoaDonThanhToanId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HoaDonThanhToanRepository extends JpaRepository<HoaDonThanhToan, HoaDonThanhToanId> {

    /**
     * Find all payment records for a specific order
     */
    List<HoaDonThanhToan> findByHoaDonId(Long hoaDonId);

    /**
     * Calculate total paid amount for an order
     */
    @Query("SELECT COALESCE(SUM(hdt.soTienApDung), 0) FROM HoaDonThanhToan hdt WHERE hdt.hoaDon.id = :hoaDonId")
    BigDecimal calculateTotalPaidAmount(@Param("hoaDonId") Long hoaDonId);

    /**
     * Find payment records for an order with payment details
     */
    @Query("SELECT hdt FROM HoaDonThanhToan hdt " +
           "JOIN FETCH hdt.thanhToan t " +
           "WHERE hdt.hoaDon.id = :hoaDonId " +
           "ORDER BY hdt.ngayTao DESC")
    List<HoaDonThanhToan> findByHoaDonIdWithPaymentDetails(@Param("hoaDonId") Long hoaDonId);
}
