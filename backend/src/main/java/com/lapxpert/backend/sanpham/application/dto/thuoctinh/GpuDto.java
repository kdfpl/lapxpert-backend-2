package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Gpu}
 */
@Data
public class GpuDto implements Serializable {
    Long id;
    String moTaGpu;
}
