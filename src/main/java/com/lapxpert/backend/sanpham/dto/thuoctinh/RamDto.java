package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.Ram;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link Ram}
 */
@Data
public class RamDto implements Serializable {
    Long id;
    String maRam;
    String moTaRam;
}
