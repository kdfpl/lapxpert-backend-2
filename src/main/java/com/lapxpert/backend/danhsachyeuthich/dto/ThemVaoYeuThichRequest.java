package com.lapxpert.backend.danhsachyeuthich.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for adding products to wishlist
 * Follows established request DTO patterns with Vietnamese naming conventions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemVaoYeuThichRequest {
    
    @NotNull(message = "ID người dùng không được để trống")
    private Long nguoiDungId;
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long sanPhamId;
    
    /**
     * Optional: Current price when adding to wishlist for price tracking
     */
    private java.math.BigDecimal giaKhiThem;
    
    /**
     * Optional: Source of the wishlist addition (e.g., "product_page", "search_results")
     */
    private String source;
}
