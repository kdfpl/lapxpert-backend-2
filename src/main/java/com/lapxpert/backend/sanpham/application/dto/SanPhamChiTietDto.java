package com.lapxpert.backend.sanpham.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SanPhamChiTietDto {
    private Long id;
    private String sku;
    private BigDecimal giaBan;
    private List<String> hinhAnh;
}
