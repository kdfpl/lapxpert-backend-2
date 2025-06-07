package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.DanhMucDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.DanhMuc;
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
