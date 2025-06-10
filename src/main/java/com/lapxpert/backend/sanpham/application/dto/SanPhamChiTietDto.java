package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.*;
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

    private MauSacDto mauSac;

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
    private CpuDto cpu;
    private RamDto ram;
    private GpuDto gpu;
    private BoNhoDto boNho;
    private ManHinhDto manHinh;

    // Computed fields for business logic
    /**
     * Check if this variant is active for sales
     */
    public boolean isActive() {
        return trangThai != null && trangThai;
    }
}
