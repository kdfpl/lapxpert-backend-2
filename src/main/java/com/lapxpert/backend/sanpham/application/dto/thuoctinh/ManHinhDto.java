package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ManHinh}
 */
@Data
public class ManHinhDto implements Serializable {
    Long id;
    String moTaManHinh;
}
