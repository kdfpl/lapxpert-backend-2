package com.lapxpert.backend.giohang.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for real-time inventory availability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAvailabilityResponse {
    
    private Long sanPhamChiTietId;
    private Integer tongSoLuong;
    private Integer soLuongCoSan;
    private Integer soLuongDatTruoc;
    private Integer soLuongDaBan;
    private Integer soLuongCartHienTai; // Reserved in current user's carts
}
