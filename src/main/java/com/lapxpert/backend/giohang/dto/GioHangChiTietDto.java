package com.lapxpert.backend.giohang.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for GioHangChiTiet entity representing individual cart items
 * Follows established DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GioHangChiTietDto {
    
    private Long id;
    
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    private Long sanPhamChiTietId;
    
    /**
     * Product information for display
     */
    private String tenSanPham;
    private String serialNumber;
    private String hinhAnh;
    private String mauSac;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer soLuong;
    
    @NotNull(message = "Giá tại thời điểm thêm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal giaTaiThoiDiemThem;
    
    /**
     * Current price of the product (may differ from cart price)
     */
    private BigDecimal giaHienTai;
    
    /**
     * Total amount for this cart item (quantity * price at time of addition)
     */
    private BigDecimal thanhTien;
    
    /**
     * Standard audit fields for online modules
     */
    private Instant ngayThem;
    private Instant ngayCapNhat;
    
    // Business logic fields
    /**
     * Indicates if the product is still available for purchase
     */
    private boolean isAvailable;
    
    /**
     * Indicates if the price has changed since adding to cart
     */
    private boolean hasPriceChanged;
    
    /**
     * Price difference if price has changed (positive = price increase, negative = price decrease)
     */
    private BigDecimal priceChangeDifference;
    
    /**
     * Human-readable availability status
     */
    private String availabilityStatus;
    
    /**
     * Calculate total amount for this item
     * @return quantity * price at time of addition
     */
    public BigDecimal calculateThanhTien() {
        if (giaTaiThoiDiemThem == null || soLuong == null) {
            return BigDecimal.ZERO;
        }
        return giaTaiThoiDiemThem.multiply(BigDecimal.valueOf(soLuong));
    }
    
    /**
     * Check if current price differs from cart price
     * @return true if price has changed
     */
    public boolean isPriceChanged() {
        if (giaHienTai == null || giaTaiThoiDiemThem == null) {
            return false;
        }
        return !giaTaiThoiDiemThem.equals(giaHienTai);
    }
    
    /**
     * Calculate price change percentage
     * @return percentage change (positive = increase, negative = decrease)
     */
    public BigDecimal getPriceChangePercentage() {
        if (!isPriceChanged() || giaTaiThoiDiemThem.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal difference = giaHienTai.subtract(giaTaiThoiDiemThem);
        return difference.divide(giaTaiThoiDiemThem, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
