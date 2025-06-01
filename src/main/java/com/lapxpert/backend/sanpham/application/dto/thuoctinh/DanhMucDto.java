package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc}
 */
@Data
public class DanhMucDto implements Serializable {
    Long id;
    String tenDanhMuc;
}