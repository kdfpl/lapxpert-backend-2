package com.lapxpert.backend.phieugiamgia.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhieuGiamGiaNguoiDungId implements Serializable {
    private Long phieuGiamGiaId;
    private Long nguoiDungId;
}
