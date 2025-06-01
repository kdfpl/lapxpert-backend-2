package com.lapxpert.backend.thongke.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for order overview statistics
 * Contains key order metrics and summaries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonHangTongQuanDto {
    
    /**
     * Total number of orders today
     */
    private Long donHangHomNay;
    
    /**
     * Total number of orders this week
     */
    private Long donHangTuanNay;
    
    /**
     * Total number of orders this month
     */
    private Long donHangThangNay;
    
    /**
     * Total number of orders this year
     */
    private Long donHangNamNay;
    
    /**
     * Total number of orders all time
     */
    private Long tongSoDonHang;
    
    /**
     * Number of pending orders (waiting for confirmation)
     */
    private Long donHangChoXacNhan;
    
    /**
     * Number of orders being processed
     */
    private Long donHangDangXuLy;
    
    /**
     * Number of orders being shipped
     */
    private Long donHangDangGiao;
    
    /**
     * Number of completed orders
     */
    private Long donHangHoanThanh;
    
    /**
     * Number of cancelled orders
     */
    private Long donHangDaHuy;
    
    /**
     * Number of returned orders
     */
    private Long donHangTraHang;
    
    /**
     * Average order value
     */
    private BigDecimal giaTriDonHangTrungBinh;
    
    /**
     * Highest order value
     */
    private BigDecimal giaTriDonHangCaoNhat;
    
    /**
     * Order completion rate (percentage)
     */
    private Double tyLeHoanThanh;
    
    /**
     * Order cancellation rate (percentage)
     */
    private Double tyLeHuy;
    
    /**
     * Order return rate (percentage)
     */
    private Double tyLeTraHang;
    
    /**
     * Daily growth in orders (today vs yesterday)
     */
    private Double tyLeTangTruongNgay;
    
    /**
     * Weekly growth in orders (this week vs last week)
     */
    private Double tyLeTangTruongTuan;
    
    /**
     * Monthly growth in orders (this month vs last month)
     */
    private Double tyLeTangTruongThang;
    
    /**
     * Order breakdown by type
     */
    private DonHangTheoLoaiDto donHangTheoLoai;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonHangTheoLoaiDto {
        private Long taiQuay;        // POS orders
        private Long online;         // Online orders
        private Double tyLeTaiQuay;  // POS percentage
        private Double tyLeOnline;   // Online percentage
    }
}
