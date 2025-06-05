package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {SanPhamMapper.class, MauSacMapper.class})
public interface SanPhamChiTietMapper {
    SanPhamChiTietDto toDto(SanPhamChiTiet sanPhamChiTiet);

    @Mapping(target = "sanPham", ignore = true)
    @Mapping(target = "dotGiamGias", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpu", ignore = true)
    @Mapping(target = "ram", ignore = true)
    @Mapping(target = "gpu", ignore = true)
    @Mapping(target = "oCung", ignore = true)
    @Mapping(target = "manHinh", ignore = true)
    @Mapping(target = "mauSac", ignore = true)
    SanPhamChiTiet toEntity(SanPhamChiTietDto dto);

    List<SanPhamChiTietDto> toDtos(List<SanPhamChiTiet> entities);

    Set<SanPhamChiTietDto> toDtoSet(Set<SanPhamChiTiet> entities);
}
