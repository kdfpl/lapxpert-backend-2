package com.lapxpert.backend.thongke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for product performance by category
 * Contains sales and performance metrics broken down by product categories
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamTheoDanhMucDto {
    
    /**
     * Category labels for chart display
     */
    private List<String> labels;
    
    /**
     * Sales data corresponding to labels
     */
    private List<BigDecimal> doanhThuData;
    
    /**
     * Quantity sold data corresponding to labels
     */
    private List<Long> soLuongData;
    
    /**
     * Total revenue across all categories
     */
    private BigDecimal tongDoanhThu;
    
    /**
     * Total quantity sold across all categories
     */
    private Long tongSoLuong;
    
    /**
     * Detailed breakdown by category
     */
    private List<DanhMucChiTietDto> chiTietDanhMuc;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DanhMucChiTietDto {
        /**
         * Category ID
         */
        private Long danhMucId;
        
        /**
         * Category name
         */
        private String tenDanhMuc;
        
        /**
         * Category description
         */
        private String moTa;
        
        /**
         * Number of products in this category
         */
        private Long soLuongSanPham;
        
        /**
         * Total quantity sold in this category
         */
        private Long soLuongBan;
        
        /**
         * Total revenue from this category
         */
        private BigDecimal doanhThu;
        
        /**
         * Average price in this category
         */
        private BigDecimal giaTrungBinh;
        
        /**
         * Percentage of total revenue
         */
        private Double tyLeDoanhThu;
        
        /**
         * Percentage of total quantity sold
         */
        private Double tyLeSoLuong;
        
        /**
         * Growth compared to previous period
         */
        private Double tyLeTangTruong;
        
        /**
         * Best selling product in this category
         */
        private String sanPhamBanChayNhat;
        
        /**
         * Current stock level for this category
         */
        private Long tonKho;
        
        /**
         * Stock value for this category
         */
        private BigDecimal giaTriTonKho;
        
        /**
         * Performance ranking among categories
         */
        private Integer thuHang;
    }
}
