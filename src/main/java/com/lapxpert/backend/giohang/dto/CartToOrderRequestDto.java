package com.lapxpert.backend.giohang.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for converting cart to order
 * Contains all information needed to create an order from cart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartToOrderRequestDto {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Long diaChiGiaoHangId;
    
    /**
     * Payment information
     */
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String phuongThucThanhToan;
    
    /**
     * Delivery information
     */
    @NotBlank(message = "Phương thức vận chuyển không được để trống")
    private String phuongThucVanChuyen;
    
    /**
     * Applied vouchers and discounts
     */
    private List<Long> phieuGiamGiaIds;
    private List<Long> dotGiamGiaIds;
    
    /**
     * Order notes and preferences
     */
    @Size(max = 500, message = "Ghi chú đơn hàng không được vượt quá 500 ký tự")
    private String ghiChu;
    
    /**
     * Delivery preferences
     */
    private String thoiGianGiaoHangMongMuon;
    private Boolean giaohangTanNoi;
    
    /**
     * Price confirmation (to prevent price manipulation)
     */
    @NotNull(message = "Tổng tiền xác nhận không được để trống")
    @DecimalMin(value = "0.0", message = "Tổng tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal tongTienXacNhan;
    
    /**
     * Force conversion flags
     */
    @Builder.Default
    private Boolean forceConvertWithPriceChanges = false;
    
    @Builder.Default
    private Boolean forceConvertWithUnavailableItems = false;
    
    /**
     * Contact information for delivery
     */
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String soDienThoaiGiaoHang;
    
    @Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
    private String tenNguoiNhan;
    
    /**
     * Validation method to ensure required delivery info is provided
     */
    @AssertTrue(message = "Thông tin người nhận và số điện thoại là bắt buộc")
    public boolean isDeliveryInfoValid() {
        return (tenNguoiNhan != null && !tenNguoiNhan.trim().isEmpty()) &&
               (soDienThoaiGiaoHang != null && !soDienThoaiGiaoHang.trim().isEmpty());
    }
}
