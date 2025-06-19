package com.lapxpert.backend.giohang.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for adding products to cart
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemSanPhamVaoGioRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    private Long sanPhamChiTietId;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer soLuong;
    
    /**
     * Optional: Force add even if price has changed
     * Default: false (will prompt user about price changes)
     */
    @Builder.Default
    private Boolean forceAdd = false;
    
    /**
     * Optional: Replace existing quantity instead of adding to it
     * Default: false (will add to existing quantity)
     */
    @Builder.Default
    private Boolean replaceQuantity = false;
}
