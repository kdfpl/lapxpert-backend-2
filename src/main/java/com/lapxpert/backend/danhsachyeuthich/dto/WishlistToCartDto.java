package com.lapxpert.backend.danhsachyeuthich.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for individual wishlist item conversion to cart
 * Used in batch wishlist-to-cart operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistToCartDto {
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long sanPhamId;
    
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    private Long sanPhamChiTietId;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer soLuong;
    
    /**
     * Optional: Priority for this item in batch operation
     * Higher priority items are processed first
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * Optional: Skip this item if unavailable
     * Default: true (skip if unavailable)
     */
    @Builder.Default
    private Boolean skipIfUnavailable = true;
}
