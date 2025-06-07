package com.lapxpert.backend.phieugiamgia.application.dto;

import com.lapxpert.backend.common.enums.TrangThaiCampaign;
import com.lapxpert.backend.common.enums.LoaiGiamGia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for {@link com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia}
 * Uses TrangThaiCampaign for voucher campaign lifecycle tracking
 * Enhanced with builder pattern for better construction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhieuGiamGiaDto implements Serializable {

    private Long id;
    private String maPhieuGiamGia;
    private LoaiGiamGia loaiGiamGia;
    private TrangThaiCampaign trangThai;
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriDonHangToiThieu;
    private Instant ngayBatDau;
    private Instant ngayKetThuc;
    private String moTa;
    private Integer soLuongBanDau;
    private Integer soLuongDaDung;
    private Instant ngayTao;
    private Instant ngayCapNhat;
    private List<Long> danhSachNguoiDung;

    // Vietnam timezone formatted strings for display
    private String ngayBatDauVietnam;
    private String ngayKetThucVietnam;
    private String ngayTaoVietnam;
    private String ngayCapNhatVietnam;

    // Timezone metadata for frontend
    private String businessTimezone;

    // Audit fields from AdminAuditableEntity
    private String nguoiTao;
    private String nguoiCapNhat;
    private String lyDoThayDoi;
    private String giaTriCu;
    private String giaTriMoi;
    private String ipAddress;
    private String userAgent;
}