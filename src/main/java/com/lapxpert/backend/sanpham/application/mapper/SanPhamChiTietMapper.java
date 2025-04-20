package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.SanPhamChiTietDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {SanPhamMapper.class})
public interface SanPhamChiTietMapper {
    SanPhamChiTietDto toDto(SanPhamChiTiet sanPhamChiTiet);
    SanPhamChiTiet toEntity(SanPhamChiTietDto dto);

    List<SanPhamChiTietDto> toDtos(List<SanPhamChiTiet> entities);

    Set<SanPhamChiTietDto> toDtoSet(Set<SanPhamChiTiet> entities);
}
