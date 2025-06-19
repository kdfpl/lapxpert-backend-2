package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.DanhMuc;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link DanhMuc}
 */
@Data
public class DanhMucDto implements Serializable {
    Long id;
    String maDanhMuc;
    String moTaDanhMuc;
}