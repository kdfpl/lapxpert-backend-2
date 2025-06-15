package com.lapxpert.backend.thongke.domain.service;

import com.lapxpert.backend.thongke.domain.dto.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * ThongKe (Statistics) Service Interface
 * Defines business logic for statistics calculation and aggregation
 * Following Vietnamese naming conventions and clean architecture principles
 */
public interface ThongKeService {

    // ==================== DOANH THU (REVENUE) STATISTICS ====================

    /**
     * Get daily revenue statistics
     * @param tuNgay Start date (optional, defaults to 30 days ago)
     * @param denNgay End date (optional, defaults to today)
     * @return Daily revenue breakdown
     */
    DoanhThuTheoNgayDto layDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay);

    /**
     * Get monthly revenue statistics
     * @param nam Year (optional, defaults to current year)
     * @return Monthly revenue breakdown for the year
     */
    DoanhThuTheoThangDto layDoanhThuTheoThang(Integer nam);

    /**
     * Get revenue overview/summary
     * @return Revenue summary with key metrics
     */
    DoanhThuTongQuanDto layDoanhThuTongQuan();

    // ==================== DON HANG (ORDER) STATISTICS ====================

    /**
     * Get order overview statistics
     * @return Order summary with key metrics
     */
    DonHangTongQuanDto layDonHangTongQuan();

    /**
     * Get order statistics by status
     * @return Order count breakdown by status
     */
    DonHangTheoTrangThaiDto layDonHangTheoTrangThai();

    /**
     * Get average order value statistics
     * @return Average order value metrics
     */
    Map<String, Object> layGiaTriDonHangTrungBinh();

    // ==================== SAN PHAM (PRODUCT) STATISTICS ====================

    /**
     * Get top selling products
     * @param soLuong Number of top products to return
     * @param tuNgay Start date for analysis period
     * @param denNgay End date for analysis period
     * @return List of top selling products
     */
    SanPhamBanChayDto laySanPhamBanChayNhat(Integer soLuong, LocalDate tuNgay, LocalDate denNgay);

    /**
     * Get low stock products
     * @param nguongTonKho Stock threshold
     * @return List of products with low stock
     */
    SanPhamSapHetHangDto laySanPhamSapHetHang(Integer nguongTonKho);

    /**
     * Get product performance by category
     * @return Product performance breakdown by category
     */
    SanPhamTheoDanhMucDto laySanPhamTheoDanhMuc();

    // ==================== KHACH HANG (CUSTOMER) STATISTICS ====================

    /**
     * Get new customer statistics
     * @param tuNgay Start date for analysis period
     * @param denNgay End date for analysis period
     * @return New customer acquisition metrics
     */
    KhachHangMoiDto layKhachHangMoi(LocalDate tuNgay, LocalDate denNgay);

    /**
     * Get customer retention rate
     * @return Customer retention metrics
     */
    Map<String, Object> layTyLeGiuChanKhachHang();

    /**
     * Get average customer value
     * @return Customer lifetime value metrics
     */
    Map<String, Object> layGiaTriKhachHangTrungBinh();

    // ==================== DASHBOARD SUMMARY ====================

    /**
     * Get dashboard summary with key metrics
     * @return Dashboard summary data
     */
    DashboardSummaryDto layDashboardSummary();
}
