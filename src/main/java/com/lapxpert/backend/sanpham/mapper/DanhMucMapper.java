package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.DanhMucDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.DanhMuc;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface DanhMucMapper {

    DanhMucDto toDto(DanhMuc danhMuc);

    DanhMuc toEntity(DanhMucDto dto);

    List<DanhMucDto> toDtos(List<DanhMuc> entities);

    Set<DanhMucDto> toDtoSet(Set<DanhMuc> entities);
}
