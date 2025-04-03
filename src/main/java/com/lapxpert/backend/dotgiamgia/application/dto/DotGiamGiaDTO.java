package com.lapxpert.backend.dotgiamgia.application.dto;

import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia}
 */
@Value
public class DotGiamGiaDTO implements Serializable {
    Long id;
    String maDotGiamGia;
    String tenDotGiamGia;
    BigDecimal phanTramGiam;
    Instant ngayBatDau;
    Instant ngayKetThuc;
    Boolean trangThai;
    Instant ngayTao;
    Instant ngayCapNhat;
}