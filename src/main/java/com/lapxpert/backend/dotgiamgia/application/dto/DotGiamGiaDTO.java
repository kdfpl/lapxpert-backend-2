package com.lapxpert.backend.dotgiamgia.application.dto;

import com.lapxpert.backend.dotgiamgia.domain.entity.TrangThai;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia}
 */
@Data
public class DotGiamGiaDTO implements Serializable {
    Long id;
    String maDotGiamGia;
    String tenDotGiamGia;
    BigDecimal phanTramGiam;
    Instant ngayBatDau;
    Instant ngayKetThuc;
    TrangThai trangThai;
    Instant ngayTao;
    Instant ngayCapNhat;
}