package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Gpu;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link Gpu}
 */
@Data
public class GpuDto implements Serializable {
    Long id;
    String maGpu;
    String moTaGpu;
}
