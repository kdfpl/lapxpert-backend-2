package com.lapxpert.backend.thongke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for monthly revenue statistics
 * Contains revenue data broken down by month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoanhThuTheoThangDto {
    
    /**
     * Month labels for chart display (e.g., "Tháng 1", "Tháng 2", ...)
     */
    private List<String> labels;
    
    /**
     * Revenue data points corresponding to labels
     */
    private List<BigDecimal> data;
    
    /**
     * Year being analyzed
     */
    private Integer nam;
    
    /**
     * Total revenue for the year
     */
    private BigDecimal tongDoanhThuNam;
    
    /**
     * Average monthly revenue
     */
    private BigDecimal doanhThuTrungBinhThang;
    
    /**
     * Best performing month (1-12)
     */
    private Integer thangTotNhat;
    
    /**
     * Revenue of the best month
     */
    private BigDecimal doanhThuThangTotNhat;
    
    /**
     * Worst performing month (1-12)
     */
    private Integer thangXauNhat;
    
    /**
     * Revenue of the worst month
     */
    private BigDecimal doanhThuThangXauNhat;
    
    /**
     * Growth percentage compared to previous year
     */
    private Double tyLeTangTruongNam;
    
    /**
     * Revenue comparison with previous year
     */
    private BigDecimal doanhThuNamTruoc;
    
    /**
     * Quarter-wise breakdown
     */
    private List<QuarterRevenueDto> doanhThuTheoQuy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterRevenueDto {
        private Integer quy;
        private BigDecimal doanhThu;
        private Double tyLeTangTruong;
    }
}
