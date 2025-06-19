package com.lapxpert.backend.danhsachyeuthich.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/**
 * Request DTO for moving multiple wishlist items to cart
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChuyenNhieuVaoGioHangRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "Danh sách sản phẩm không được để trống")
    @Size(min = 1, message = "Phải có ít nhất 1 sản phẩm")
    @Size(max = 50, message = "Không được vượt quá 50 sản phẩm cùng lúc")
    @Valid
    private List<WishlistToCartDto> danhSachSanPham;
    
    /**
     * Optional: Remove all items from wishlist after adding to cart
     * Default: true (remove from wishlist)
     */
    @Builder.Default
    private Boolean removeFromWishlist = true;
    
    /**
     * Optional: Force add all items even if some have price changes
     * Default: false (will prompt user about price changes)
     */
    @Builder.Default
    private Boolean forceAddAll = false;
    
    /**
     * Optional: Skip items that are unavailable instead of failing entire operation
     * Default: true (skip unavailable items)
     */
    @Builder.Default
    private Boolean skipUnavailableItems = true;
}
