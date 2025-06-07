package com.lapxpert.backend.phieugiamgia.application.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * DTO for {@link com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung}
 * Represents individual user voucher assignments with usage tracking
 */
@Value
public class PhieuGiamGiaNguoiDungDto implements Serializable {
    Long phieuGiamGiaId;
    Long nguoiDungId;
    OffsetDateTime ngayNhan;
    Boolean daSuDung;
    Instant ngaySuDung;
    Instant ngayTao;
    Instant ngayCapNhat;
}