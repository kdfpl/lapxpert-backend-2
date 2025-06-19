package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {SanPhamMapper.class, MauSacMapper.class, CpuMapper.class, RamMapper.class, GpuMapper.class, BoNhoMapper.class, ManHinhMapper.class})
public interface SanPhamChiTietMapper {
    SanPhamChiTietDto toDto(SanPhamChiTiet sanPhamChiTiet);

    @Mapping(target = "sanPham", ignore = true)
    @Mapping(target = "dotGiamGias", ignore = true)
    @Mapping(target = "id", ignore = true)
    SanPhamChiTiet toEntity(SanPhamChiTietDto dto);

    List<SanPhamChiTietDto> toDtos(List<SanPhamChiTiet> entities);

    Set<SanPhamChiTietDto> toDtoSet(Set<SanPhamChiTiet> entities);
}
