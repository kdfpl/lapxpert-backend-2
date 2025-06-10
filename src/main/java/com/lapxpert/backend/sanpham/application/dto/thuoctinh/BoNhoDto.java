package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BoNho}
 */
@Data
public class BoNhoDto implements Serializable {
    Long id;
    String maBoNho;
    String moTaBoNho;
}
