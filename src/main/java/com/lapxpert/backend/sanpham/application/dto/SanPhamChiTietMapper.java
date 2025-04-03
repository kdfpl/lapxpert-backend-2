package com.lapxpert.backend.sanpham.application.dto;

import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface SanPhamChiTietMapper {
    SanPhamChiTietDTO toDto(SanPhamChiTiet entity);

    List<SanPhamChiTietDTO> toDtos(List<SanPhamChiTiet> entities);

    Set<SanPhamChiTietDTO> toDtoSet(Set<SanPhamChiTiet> entities);
}
