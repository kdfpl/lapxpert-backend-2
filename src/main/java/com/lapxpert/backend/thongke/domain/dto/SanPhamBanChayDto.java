package com.lapxpert.backend.thongke.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for top selling products statistics
 * Contains information about best performing products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamBanChayDto {
    
    /**
     * Analysis period start date
     */
    private LocalDate tuNgay;
    
    /**
     * Analysis period end date
     */
    private LocalDate denNgay;
    
    /**
     * Number of top products requested
     */
    private Integer soLuong;
    
    /**
     * List of top selling products
     */
    private List<SanPhamBanChayChiTietDto> danhSachSanPham;
    
    /**
     * Total revenue from top products
     */
    private BigDecimal tongDoanhThu;
    
    /**
     * Total quantity sold from top products
     */
    private Long tongSoLuongBan;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamBanChayChiTietDto {
        /**
         * Product ID
         */
        private Long sanPhamId;
        
        /**
         * Product name
         */
        private String tenSanPham;
        
        /**
         * Product image URL
         */
        private String hinhAnh;
        
        /**
         * Brand name
         */
        private String thuongHieu;
        
        /**
         * Category name
         */
        private String danhMuc;
        
        /**
         * Quantity sold in the period
         */
        private Long soLuongBan;
        
        /**
         * Revenue generated from this product
         */
        private BigDecimal doanhThu;
        
        /**
         * Average selling price
         */
        private BigDecimal giaTrungBinh;
        
        /**
         * Current stock quantity
         */
        private Long tonKho;
        
        /**
         * Ranking position
         */
        private Integer thuHang;
        
        /**
         * Percentage of total sales
         */
        private Double tyLeBanHang;
        
        /**
         * Growth compared to previous period
         */
        private Double tyLeTangTruong;
    }
}
