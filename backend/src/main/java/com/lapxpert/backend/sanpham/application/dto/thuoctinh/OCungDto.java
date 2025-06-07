package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.OCung}
 */
@Data
public class OCungDto implements Serializable {
    Long id;
    String moTaOCung;
}
