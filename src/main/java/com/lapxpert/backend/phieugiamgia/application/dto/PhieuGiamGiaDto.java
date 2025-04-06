package com.lapxpert.backend.phieugiamgia.application.dto;

import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for {@link com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia}
 */
@Value
public class PhieuGiamGiaDto implements Serializable {
    Long id;
    Boolean loaiPhieuGiamGia;
    PhieuGiamGia.TrangThaiPhieuGiamGia trangThai;
    String maPhieuGiamGia;
    BigDecimal giaTriGiam;
    BigDecimal giaTriDonHangToiThieu;
    OffsetDateTime ngayBatDau;
    OffsetDateTime ngayKetThuc;
    String moTa;
    Boolean phieuRiengTu;
    Integer soLuongBanDau;
    Integer soLuongDaDung;
    OffsetDateTime ngayTao;
    OffsetDateTime ngayCapNhat;

    private List<PhieuGiamGiaNguoiDung> phieuGiamGiaNguoiDungs;
}