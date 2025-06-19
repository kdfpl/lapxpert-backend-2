package com.lapxpert.backend.danhsachyeuthich.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for moving single wishlist item to cart
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChuyenVaoGioHangRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long sanPhamId;
    
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    private Long sanPhamChiTietId;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer soLuong;
    
    /**
     * Optional: Remove from wishlist after adding to cart
     * Default: true (remove from wishlist)
     */
    @Builder.Default
    private Boolean removeFromWishlist = true;
    
    /**
     * Optional: Force add even if price has changed significantly
     * Default: false (will prompt user about price changes)
     */
    @Builder.Default
    private Boolean forceAdd = false;
}
