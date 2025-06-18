package com.lapxpert.backend.thongke.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for order statistics by status
 * Contains order count breakdown by different statuses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonHangTheoTrangThaiDto {
    
    /**
     * Status labels for chart display
     */
    private List<String> labels;
    
    /**
     * Order count data corresponding to labels
     */
    private List<Long> data;
    
    /**
     * Total number of orders
     */
    private Long tongSoDonHang;
    
    /**
     * Detailed breakdown by status
     */
    private List<TrangThaiChiTietDto> chiTietTrangThai;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrangThaiChiTietDto {
        /**
         * Status name in Vietnamese
         */
        private String tenTrangThai;
        
        /**
         * Status code/enum value
         */
        private String maTrangThai;
        
        /**
         * Number of orders in this status
         */
        private Long soLuong;
        
        /**
         * Percentage of total orders
         */
        private Double tyLe;
        
        /**
         * Color for chart display
         */
        private String mauSac;
        
        /**
         * Description of the status
         */
        private String moTa;
    }
}
