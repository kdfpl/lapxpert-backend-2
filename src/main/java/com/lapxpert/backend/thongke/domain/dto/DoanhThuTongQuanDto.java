package com.lapxpert.backend.thongke.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for revenue overview/summary
 * Contains key revenue metrics and summaries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoanhThuTongQuanDto {
    
    /**
     * Today's revenue
     */
    private BigDecimal doanhThuHomNay;
    
    /**
     * Yesterday's revenue for comparison
     */
    private BigDecimal doanhThuHomQua;
    
    /**
     * This week's revenue
     */
    private BigDecimal doanhThuTuanNay;
    
    /**
     * Last week's revenue for comparison
     */
    private BigDecimal doanhThuTuanTruoc;
    
    /**
     * This month's revenue
     */
    private BigDecimal doanhThuThangNay;
    
    /**
     * Last month's revenue for comparison
     */
    private BigDecimal doanhThuThangTruoc;
    
    /**
     * This year's revenue
     */
    private BigDecimal doanhThuNamNay;
    
    /**
     * Last year's revenue for comparison
     */
    private BigDecimal doanhThuNamTruoc;
    
    /**
     * Daily growth percentage (today vs yesterday)
     */
    private Double tyLeTangTruongNgay;
    
    /**
     * Weekly growth percentage (this week vs last week)
     */
    private Double tyLeTangTruongTuan;
    
    /**
     * Monthly growth percentage (this month vs last month)
     */
    private Double tyLeTangTruongThang;
    
    /**
     * Yearly growth percentage (this year vs last year)
     */
    private Double tyLeTangTruongNam;
    
    /**
     * Average daily revenue this month
     */
    private BigDecimal doanhThuTrungBinhNgay;
    
    /**
     * Best revenue day this month
     */
    private LocalDate ngayDoanhThuTotNhat;
    
    /**
     * Best revenue amount this month
     */
    private BigDecimal doanhThuTotNhat;
    
    /**
     * Revenue breakdown by order type
     */
    private DoanhThuTheoLoaiDto doanhThuTheoLoai;
    
    /**
     * Revenue breakdown by payment method
     */
    private DoanhThuTheoThanhToanDto doanhThuTheoThanhToan;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoanhThuTheoLoaiDto {
        private BigDecimal taiQuay;      // POS orders
        private BigDecimal online;       // Online orders
        private Double tyLeTaiQuay;      // POS percentage
        private Double tyLeOnline;       // Online percentage
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoanhThuTheoThanhToanDto {
        private BigDecimal tienMat;      // Cash payments
        private BigDecimal chuyenKhoan;  // Bank transfer
        private BigDecimal vnpay;        // VNPay
        private BigDecimal cod;          // Cash on delivery
    }
}
