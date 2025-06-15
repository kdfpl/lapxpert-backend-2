package com.lapxpert.backend.hoadon.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class HoaDonChiTietDto {
    private Long id;
    private Long hoaDonId; // Corresponds to HoaDon entity
    private Long sanPhamChiTietId; // Corresponds to SanPhamChiTiet entity
    private Integer soLuong;
    private BigDecimal giaGoc;
    private BigDecimal giaBan;
    private BigDecimal thanhTien;
    private String tenSanPhamSnapshot;
    private String skuSnapshot;
    private String hinhAnhSnapshot;
    private Instant ngayTao;
    private Instant ngayCapNhat;

    // Serial number information for specific inventory tracking
    private Long serialNumberId; // ID of the specific serial number to use
    private String serialNumber; // Serial number value for display/validation
}
