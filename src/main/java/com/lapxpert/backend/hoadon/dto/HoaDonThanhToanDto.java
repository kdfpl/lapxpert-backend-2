package com.lapxpert.backend.hoadon.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class HoaDonThanhToanDto {
    private Long hoaDonId;
    private Long thanhToanId;
    // Optionally, include ThanhToanDto here if you want to nest payment details
    // private ThanhToanDto thanhToanDetails; 
    private BigDecimal soTienApDung;
    private Instant ngayTao;
    private Instant ngayCapNhat;
}
