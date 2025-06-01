package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.SanPhamDto;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {SanPhamChiTietMapper.class, ThuongHieuMapper.class, DanhMucMapper.class})
public interface SanPhamMapper {
    SanPhamDto toDto(SanPham sanPham);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sanPhamChiTiets", ignore = true)
    @Mapping(target = "nguoiTao", ignore = true)
    @Mapping(target = "nguoiCapNhat", ignore = true)
    @Mapping(target = "ngayTao", ignore = true)
    @Mapping(target = "ngayCapNhat", ignore = true)
    SanPham toEntity(SanPhamDto dto);

    List<SanPhamDto> toDtos(List<SanPham> entities);

    Set<SanPhamDto> toDtoSet(Set<SanPham> entities);
}
