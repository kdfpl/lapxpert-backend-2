package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.DanhMucDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface DanhMucMapper {

    @Mapping(source = "moTaDanhMuc", target = "tenDanhMuc")
    DanhMucDto toDto(DanhMuc danhMuc);

    @Mapping(source = "tenDanhMuc", target = "moTaDanhMuc")
    DanhMuc toEntity(DanhMucDto dto);

    List<DanhMucDto> toDtos(List<DanhMuc> entities);

    Set<DanhMucDto> toDtoSet(Set<DanhMuc> entities);
}
