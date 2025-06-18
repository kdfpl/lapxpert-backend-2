package com.lapxpert.backend.giohang.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for updating cart item quantity
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapNhatSoLuongRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    private Long sanPhamChiTietId;
    
    @NotNull(message = "Số lượng mới không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer soLuongMoi;
    
    /**
     * Optional: Update price to current price when updating quantity
     * Default: false (keep original price)
     */
    @Builder.Default
    private Boolean updatePrice = false;
}
