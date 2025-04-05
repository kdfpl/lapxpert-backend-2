package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {SanPhamChiTietMapper.class, ThuongHieuMapper.class})
public interface SanPhamMapper {
    SanPhamDto toDto(SanPham sanPham);
    SanPham toEntity(SanPhamDto dto);

    List<SanPhamDto> toDtos(List<SanPham> entities);

    Set<SanPhamDto> toDtoSet(Set<SanPham> entities);
}
