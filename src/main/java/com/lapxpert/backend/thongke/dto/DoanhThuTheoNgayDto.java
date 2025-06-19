package com.lapxpert.backend.thongke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for daily revenue statistics
 * Contains revenue data broken down by day
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoanhThuTheoNgayDto {
    
    /**
     * Date labels for chart display
     */
    private List<String> labels;
    
    /**
     * Revenue data points corresponding to labels
     */
    private List<BigDecimal> data;
    
    /**
     * Total revenue for the period
     */
    private BigDecimal tongDoanhThu;
    
    /**
     * Average daily revenue
     */
    private BigDecimal doanhThuTrungBinhNgay;
    
    /**
     * Start date of the analysis period
     */
    private LocalDate tuNgay;
    
    /**
     * End date of the analysis period
     */
    private LocalDate denNgay;
    
    /**
     * Number of days in the analysis period
     */
    private Integer soNgay;
    
    /**
     * Growth percentage compared to previous period
     */
    private Double tyLeTangTruong;
    
    /**
     * Highest revenue day in the period
     */
    private LocalDate ngayDoanhThuCaoNhat;
    
    /**
     * Highest revenue amount
     */
    private BigDecimal doanhThuCaoNhat;
    
    /**
     * Lowest revenue day in the period
     */
    private LocalDate ngayDoanhThuThapNhat;
    
    /**
     * Lowest revenue amount
     */
    private BigDecimal doanhThuThapNhat;
}
