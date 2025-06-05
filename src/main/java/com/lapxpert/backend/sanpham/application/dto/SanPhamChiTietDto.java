package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet}
 * Updated to align with entity structure using Boolean for variant status
 */
@Data
public class SanPhamChiTietDto implements Serializable {
    private Long id;

    /**
     * Unique SKU for variant identification
     */
    private String sku;

    private MauSac mauSac;

    private BigDecimal giaBan;

    private BigDecimal giaKhuyenMai;

    private List<String> hinhAnh;

    /**
     * Variant status (active/inactive for sales)
     * Uses Boolean: true = active, false = inactive
     */
    private Boolean trangThai;

    private Instant ngayTao;
    private Instant ngayCapNhat;

    // === 6 CORE ATTRIBUTES (as per requirements) ===
    private Cpu cpu;
    private Ram ram;
    private Gpu gpu;
    private OCung oCung;
    private ManHinh manHinh;

    // Computed fields for business logic
    /**
     * Check if this variant is active for sales
     */
    public boolean isActive() {
        return trangThai != null && trangThai;
    }
}
