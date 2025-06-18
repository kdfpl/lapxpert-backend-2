package com.lapxpert.backend.hoadon.dto;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.enums.TrangThaiGiaoDich;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ThanhToanDto {
    private Long id;
    private Long nguoiDungId; // ID of the user who made the payment
    private String maGiaoDich;
    private BigDecimal giaTri;
    private String ghiChu;
    private Instant thoiGianThanhToan;
    private TrangThaiGiaoDich trangThaiGiaoDich;
    private PhuongThucThanhToan phuongThucThanhToan;
    private Instant ngayTao;
    private Instant ngayCapNhat;
}
