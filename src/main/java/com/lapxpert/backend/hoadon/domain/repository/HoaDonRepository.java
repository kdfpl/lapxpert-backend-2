package com.lapxpert.backend.hoadon.domain.repository;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.domain.enums.LoaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // ==================== CUSTOMER VALUE STATISTICS ====================

    /**
     * Get customer value statistics
     * @return Array of [customer_count, total_value, avg_value, max_value, min_value]
     */
    @Query("SELECT COUNT(DISTINCT h.khachHang.id), " +
           "SUM(h.tongThanhToan), " +
           "AVG(h.tongThanhToan), " +
           "MAX(h.tongThanhToan), " +
           "MIN(h.tongThanhToan) " +
           "FROM HoaDon h " +
           "WHERE h.trangThaiDonHang = :trangThai")
    Object[] getCustomerValueStatistics(@Param("trangThai") TrangThaiDonHang trangThai);

    /**
     * Get customer lifetime value
     * @param customerId customer ID
     * @return total value of completed orders
     */
    @Query("SELECT COALESCE(SUM(h.tongThanhToan), 0) FROM HoaDon h " +
           "WHERE h.khachHang.id = :customerId " +
           "AND h.trangThaiDonHang = :trangThai")
    BigDecimal getCustomerLifetimeValue(@Param("customerId") Long customerId,
                                       @Param("trangThai") TrangThaiDonHang trangThai);

    /**
     * Count customers who made orders in a period
     */
    @Query("SELECT COUNT(DISTINCT h.khachHang.id) FROM HoaDon h " +
           "WHERE h.ngayTao BETWEEN :tuNgay AND :denNgay " +
           "AND h.trangThaiDonHang = :trangThai")
    Long countActiveCustomers(@Param("tuNgay") Instant tuNgay,
                             @Param("denNgay") Instant denNgay,
                             @Param("trangThai") TrangThaiDonHang trangThai);

    /**
     * Count customers who made repeat orders
     */
    @Query("SELECT COUNT(DISTINCT h.khachHang.id) FROM HoaDon h " +
           "WHERE h.khachHang.id IN (" +
           "  SELECT h2.khachHang.id FROM HoaDon h2 " +
           "  WHERE h2.ngayTao BETWEEN :tuNgay AND :denNgay " +
           "  AND h2.trangThaiDonHang = :trangThai " +
           "  GROUP BY h2.khachHang.id " +
           "  HAVING COUNT(h2.id) > 1" +
           ")")
    Long countRepeatCustomers(@Param("tuNgay") Instant tuNgay,
                             @Param("denNgay") Instant denNgay,
                             @Param("trangThai") TrangThaiDonHang trangThai);
}