package com.lapxpert.backend.sanpham.application.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu}
 */
@Data
public class ThuongHieuDto implements Serializable {
    Long id;
    String moTaThuongHieu;
}