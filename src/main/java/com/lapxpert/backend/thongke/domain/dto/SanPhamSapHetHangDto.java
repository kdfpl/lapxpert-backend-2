package com.lapxpert.backend.thongke.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for low stock products
 * Contains information about products that are running low on inventory
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamSapHetHangDto {
    
    /**
     * Stock threshold used for the query
     */
    private Integer nguongTonKho;
    
    /**
     * Total number of low stock products
     */
    private Long tongSoSanPham;
    
    /**
     * List of low stock products
     */
    private List<SanPhamSapHetHangChiTietDto> danhSachSanPham;
    
    /**
     * Total value of low stock inventory
     */
    private BigDecimal tongGiaTriTonKho;
    
    /**
     * Products that are completely out of stock
     */
    private Long sanPhamHetHang;
    
    /**
     * Products with critical stock levels (below 5)
     */
    private Long sanPhamTonKhoNguyHiem;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamSapHetHangChiTietDto {
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
         * Current stock quantity
         */
        private Long tonKho;
        
        /**
         * Product price
         */
        private BigDecimal gia;
        
        /**
         * Total inventory value for this product
         */
        private BigDecimal giaTriTonKho;
        
        /**
         * Average sales per day (last 30 days)
         */
        private Double banTrungBinhNgay;
        
        /**
         * Estimated days until out of stock
         */
        private Integer soNgayConLai;
        
        /**
         * Stock status level
         */
        private String mucDoTonKho; // "HET_HANG", "NGUY_HIEM", "THAP", "BINH_THUONG"
        
        /**
         * Recommended reorder quantity
         */
        private Long soLuongDeXuat;
        
        /**
         * Last restock date
         */
        private String ngayNhapCuoi;
    }
}
