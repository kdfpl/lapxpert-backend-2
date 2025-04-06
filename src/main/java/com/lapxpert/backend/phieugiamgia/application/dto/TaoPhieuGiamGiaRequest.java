package com.lapxpert.backend.phieugiamgia.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class TaoPhieuGiamGiaRequest {
    private String maPhieuGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriDonHangToiThieu;
    private OffsetDateTime ngayBatDau;
    private OffsetDateTime ngayKetThuc;
    private String moTa;
    private Boolean phieuRiengTu;
    private Integer soLuongBanDau;
    private List<Long> danhSachNguoiDungDuocNhan; // chỉ dùng khi là phiếu riêng tư
}
