package com.lapxpert.backend.phieugiamgia.application.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for {@link com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDung}
 */
@Value
public class PhieuGiamGiaNguoiDungDto implements Serializable {
    OffsetDateTime ngayNhan;
    Boolean daSuDung;
}