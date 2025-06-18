package com.lapxpert.backend.sanpham.dto.thuoctinh;

import com.lapxpert.backend.sanpham.entity.thuoctinh.MauSac;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link MauSac}
 */
@Data
public class MauSacDto implements Serializable {
    Long id;
    String maMauSac;
    String moTaMauSac;
}
