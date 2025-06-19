package com.lapxpert.backend.phieugiamgia.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhieuGiamGiaNguoiDungId implements Serializable {
    private Long phieuGiamGiaId;
    private Long nguoiDungId;
}
