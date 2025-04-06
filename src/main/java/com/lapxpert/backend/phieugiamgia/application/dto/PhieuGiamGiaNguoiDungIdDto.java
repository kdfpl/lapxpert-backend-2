package com.lapxpert.backend.phieugiamgia.application.dto;

import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGiaNguoiDungId}
 */
@Value
public class PhieuGiamGiaNguoiDungIdDto implements Serializable {
    Long phieuGiamGiaId;
    Long nguoiDungId;
}