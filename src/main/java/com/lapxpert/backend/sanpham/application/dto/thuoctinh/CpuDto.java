package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.Cpu}
 */
@Data
public class CpuDto implements Serializable {
    Long id;
    String maCpu;
    String moTaCpu;
}
