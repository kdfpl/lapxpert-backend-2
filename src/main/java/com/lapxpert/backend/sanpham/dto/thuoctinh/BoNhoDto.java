package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.BoNho;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link BoNho}
 */
@Data
public class BoNhoDto implements Serializable {
    Long id;
    String maBoNho;
    String moTaBoNho;
}
