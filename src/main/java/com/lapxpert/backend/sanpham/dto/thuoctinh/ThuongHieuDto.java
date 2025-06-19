package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.ThuongHieu;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link ThuongHieu}
 */
@Data
public class ThuongHieuDto implements Serializable {
    Long id;
    String maThuongHieu;
    String moTaThuongHieu;
}