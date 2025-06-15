package com.lapxpert.backend.thongke.application.controller;

import com.lapxpert.backend.thongke.domain.service.ThongKeService;
import com.lapxpert.backend.thongke.domain.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * ThongKe (Statistics) Controller
 * Provides comprehensive business intelligence and reporting endpoints
 * Following Vietnamese naming conventions and clean architecture principles
 */
@RestController
@RequestMapping("/api/v1/thong-ke")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ThongKeController {

    private final ThongKeService thongKeService;

    // ==================== DOANH THU (REVENUE) STATISTICS ====================

    /**
     * Get daily revenue statistics
     * @param tuNgay Start date (optional, defaults to 30 days ago)
     * @param denNgay End date (optional, defaults to today)
     * @return Daily revenue breakdown
     */
    @GetMapping("/doanh-thu/theo-ngay")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DoanhThuTheoNgayDto> layDoanhThuTheoNgay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        
        log.debug("Getting daily revenue statistics from {} to {}", tuNgay, denNgay);
        
        try {
            DoanhThuTheoNgayDto result = thongKeService.layDoanhThuTheoNgay(tuNgay, denNgay);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting daily revenue statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get monthly revenue statistics
     * @param nam Year (optional, defaults to current year)
     * @return Monthly revenue breakdown for the year
     */
    @GetMapping("/doanh-thu/theo-thang")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DoanhThuTheoThangDto> layDoanhThuTheoThang(
            @RequestParam(required = false) Integer nam) {
        
        log.debug("Getting monthly revenue statistics for year {}", nam);
        
        try {
            DoanhThuTheoThangDto result = thongKeService.layDoanhThuTheoThang(nam);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting monthly revenue statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get revenue overview/summary
     * @return Revenue summary with key metrics
     */
    @GetMapping("/doanh-thu/tong-quan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DoanhThuTongQuanDto> layDoanhThuTongQuan() {
        log.debug("Getting revenue overview");
        
        try {
            DoanhThuTongQuanDto result = thongKeService.layDoanhThuTongQuan();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting revenue overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== DON HANG (ORDER) STATISTICS ====================

    /**
     * Get order overview statistics
     * @return Order summary with key metrics
     */
    @GetMapping("/don-hang/tong-quan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DonHangTongQuanDto> layDonHangTongQuan() {
        log.debug("Getting order overview");
        
        try {
            DonHangTongQuanDto result = thongKeService.layDonHangTongQuan();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting order overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get order statistics by status
     * @return Order count breakdown by status
     */
    @GetMapping("/don-hang/theo-trang-thai")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DonHangTheoTrangThaiDto> layDonHangTheoTrangThai() {
        log.debug("Getting order statistics by status");
        
        try {
            DonHangTheoTrangThaiDto result = thongKeService.layDonHangTheoTrangThai();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting order statistics by status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get average order value statistics
     * @return Average order value metrics
     */
    @GetMapping("/don-hang/gia-tri-trung-binh")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> layGiaTriDonHangTrungBinh() {
        log.debug("Getting average order value statistics");
        
        try {
            Map<String, Object> result = thongKeService.layGiaTriDonHangTrungBinh();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting average order value statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== SAN PHAM (PRODUCT) STATISTICS ====================

    /**
     * Get top selling products
     * @param soLuong Number of top products to return (default: 10)
     * @param tuNgay Start date for analysis period
     * @param denNgay End date for analysis period
     * @return List of top selling products
     */
    @GetMapping("/san-pham/ban-chay-nhat")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SanPhamBanChayDto> laySanPhamBanChayNhat(
            @RequestParam(defaultValue = "10") Integer soLuong,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        
        log.debug("Getting top {} selling products from {} to {}", soLuong, tuNgay, denNgay);
        
        try {
            SanPhamBanChayDto result = thongKeService.laySanPhamBanChayNhat(soLuong, tuNgay, denNgay);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting top selling products", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get low stock products
     * @param nguongTonKho Stock threshold (default: 10)
     * @return List of products with low stock
     */
    @GetMapping("/san-pham/sap-het-hang")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SanPhamSapHetHangDto> laySanPhamSapHetHang(
            @RequestParam(defaultValue = "10") Integer nguongTonKho) {
        
        log.debug("Getting low stock products with threshold {}", nguongTonKho);
        
        try {
            SanPhamSapHetHangDto result = thongKeService.laySanPhamSapHetHang(nguongTonKho);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting low stock products", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get product performance by category
     * @return Product performance breakdown by category
     */
    @GetMapping("/san-pham/theo-danh-muc")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<SanPhamTheoDanhMucDto> laySanPhamTheoDanhMuc() {
        log.debug("Getting product performance by category");
        
        try {
            SanPhamTheoDanhMucDto result = thongKeService.laySanPhamTheoDanhMuc();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting product performance by category", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== KHACH HANG (CUSTOMER) STATISTICS ====================

    /**
     * Get new customer statistics
     * @param tuNgay Start date for analysis period
     * @param denNgay End date for analysis period
     * @return New customer acquisition metrics
     */
    @GetMapping("/khach-hang/moi")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<KhachHangMoiDto> layKhachHangMoi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        
        log.debug("Getting new customer statistics from {} to {}", tuNgay, denNgay);
        
        try {
            KhachHangMoiDto result = thongKeService.layKhachHangMoi(tuNgay, denNgay);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting new customer statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get customer retention rate
     * @return Customer retention metrics
     */
    @GetMapping("/khach-hang/ty-le-giu-chan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> layTyLeGiuChanKhachHang() {
        log.debug("Getting customer retention rate");
        
        try {
            Map<String, Object> result = thongKeService.layTyLeGiuChanKhachHang();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting customer retention rate", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get average customer value
     * @return Customer lifetime value metrics
     */
    @GetMapping("/khach-hang/gia-tri-trung-binh")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> layGiaTriKhachHangTrungBinh() {
        log.debug("Getting average customer value");
        
        try {
            Map<String, Object> result = thongKeService.layGiaTriKhachHangTrungBinh();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting average customer value", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== DASHBOARD SUMMARY ====================

    /**
     * Get dashboard summary with key metrics
     * @return Dashboard summary data
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DashboardSummaryDto> layDashboardSummary() {
        log.debug("Getting dashboard summary");
        
        try {
            DashboardSummaryDto result = thongKeService.layDashboardSummary();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting dashboard summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
