package com.lapxpert.backend.thongke.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for dashboard summary
 * Contains all key metrics for the main dashboard view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    
    /**
     * Timestamp when data was last updated
     */
    private LocalDateTime capNhatLanCuoi;
    
    /**
     * Revenue summary
     */
    private DoanhThuSummary doanhThu;
    
    /**
     * Order summary
     */
    private DonHangSummary donHang;
    
    /**
     * Product summary
     */
    private SanPhamSummary sanPham;
    
    /**
     * Customer summary
     */
    private KhachHangSummary khachHang;
    
    /**
     * System alerts and notifications
     */
    private ThongBaoSummary thongBao;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoanhThuSummary {
        private BigDecimal homNay;
        private BigDecimal tuanNay;
        private BigDecimal thangNay;
        private BigDecimal namNay;
        private Double tangTruongNgay;
        private Double tangTruongTuan;
        private Double tangTruongThang;
        private Double tangTruongNam;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonHangSummary {
        private Long tongSo;
        private Long choXacNhan;
        private Long dangXuLy;
        private Long hoanThanh;
        private Long daHuy;
        private Double tyLeHoanThanh;
        private BigDecimal giaTriTrungBinh;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamSummary {
        private Long tongSo;
        private Long sapHetHang;
        private Long hetHang;
        private List<SanPhamBanChayChiTietDto> banChayNhat;
        private List<DanhMucTotDto> danhMucTot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanPhamBanChayChiTietDto {
        private Long id;
        private String tenSanPham;
        private String hinhAnh;
        private String thuongHieu;
        private Long soLuongBan;
        private BigDecimal doanhThu;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DanhMucTotDto {
        private Long id;
        private String tenDanhMuc;
        private Long soLuong;
        private BigDecimal doanhThu;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KhachHangSummary {
        private Long tongSo;
        private Long moi;
        private Long hoatDong;
        private Double tyLeGiuChan;
        private BigDecimal giaTriTrungBinh;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThongBaoSummary {
        private Long donHangMoi;
        private Long sanPhamSapHetHang;
        private Long khachHangMoi;
        private Long danhGiaMoi;
        private Long tongThongBao;
    }
}
