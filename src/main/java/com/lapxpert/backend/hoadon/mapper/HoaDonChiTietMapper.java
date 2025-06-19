package com.lapxpert.backend.hoadon.mapper;

import com.lapxpert.backend.hoadon.dto.HoaDonChiTietDto;
import com.lapxpert.backend.hoadon.entity.HoaDonChiTiet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HoaDonChiTietMapper {

    HoaDonChiTietMapper INSTANCE = Mappers.getMapper(HoaDonChiTietMapper.class);

    @Mapping(source = "hoaDon.id", target = "hoaDonId")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    HoaDonChiTietDto toDto(HoaDonChiTiet hoaDonChiTiet);

    @Mapping(source = "hoaDonId", target = "hoaDon.id")
    @Mapping(source = "sanPhamChiTietId", target = "sanPhamChiTiet.id")
    @Mapping(target = "nguoiCapNhat", ignore = true)
    @Mapping(target = "nguoiTao", ignore = true)
    @Mapping(target = "ngayTao", ignore = true)
    @Mapping(target = "ngayCapNhat", ignore = true)
    HoaDonChiTiet toEntity(HoaDonChiTietDto hoaDonChiTietDto);

    List<HoaDonChiTietDto> toDtoList(List<HoaDonChiTiet> hoaDonChiTietList);

    List<HoaDonChiTiet> toEntityList(List<HoaDonChiTietDto> hoaDonChiTietDtoList);
}
