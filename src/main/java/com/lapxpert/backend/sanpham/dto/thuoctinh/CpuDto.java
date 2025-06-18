package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Cpu;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link Cpu}
 */
@Data
public class CpuDto implements Serializable {
    Long id;
    String maCpu;
    String moTaCpu;
}
