package com.lapxpert.backend.thongke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for new customer statistics
 * Contains metrics about customer acquisition and growth
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangMoiDto {
    
    /**
     * Analysis period start date
     */
    private LocalDate tuNgay;
    
    /**
     * Analysis period end date
     */
    private LocalDate denNgay;
    
    /**
     * Date labels for chart display
     */
    private List<String> labels;
    
    /**
     * New customer count data corresponding to labels
     */
    private List<Long> data;
    
    /**
     * Total new customers in the period
     */
    private Long tongKhachHangMoi;
    
    /**
     * Average new customers per day
     */
    private Double khachHangMoiTrungBinhNgay;
    
    /**
     * Best day for new customer acquisition
     */
    private LocalDate ngayTotNhat;
    
    /**
     * Number of new customers on best day
     */
    private Long khachHangMoiNgayTotNhat;
    
    /**
     * Growth compared to previous period
     */
    private Double tyLeTangTruong;
    
    /**
     * New customers from previous period for comparison
     */
    private Long khachHangMoiKyTruoc;
    
    /**
     * Customer acquisition breakdown
     */
    private KhachHangMoiChiTietDto chiTiet;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KhachHangMoiChiTietDto {
        /**
         * New customers from online channel
         */
        private Long khachHangOnline;
        
        /**
         * New customers from POS/store
         */
        private Long khachHangTaiQuay;
        
        /**
         * New customers from referrals
         */
        private Long khachHangGioiThieu;
        
        /**
         * New customers from marketing campaigns
         */
        private Long khachHangMarketing;
        
        /**
         * Percentage from online
         */
        private Double tyLeOnline;
        
        /**
         * Percentage from POS
         */
        private Double tyLeTaiQuay;
        
        /**
         * Average first order value
         */
        private BigDecimal giaTriDonHangDauTrungBinh;
        
        /**
         * Customers who made repeat purchases
         */
        private Long khachHangQuayLai;
        
        /**
         * Customer retention rate for new customers
         */
        private Double tyLeGiuChan;
    }
}
