package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.ManHinh;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link ManHinh}
 */
@Data
public class ManHinhDto implements Serializable {
    Long id;
    String maManHinh;
    String moTaManHinh;
}
