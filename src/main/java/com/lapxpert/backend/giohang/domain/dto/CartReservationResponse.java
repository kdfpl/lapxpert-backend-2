package com.lapxpert.backend.giohang.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for cart reservation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartReservationResponse {
    
    private Long sanPhamChiTietId;
    private Integer soLuongDatTruoc;
    private List<String> serialNumbers;
    private String cartSessionId;
    private Instant thoiGianDatTruoc;
    private Instant thoiGianHetHan;
    private Boolean thanhCong;
    private String thongBao;
}
