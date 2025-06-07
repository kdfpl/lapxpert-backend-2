package com.lapxpert.backend.sanpham.application.dto.thuoctinh;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac}
 */
@Data
public class MauSacDto implements Serializable {
    Long id;
    String moTaMauSac;
}
