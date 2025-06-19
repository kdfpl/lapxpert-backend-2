package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.BoNhoDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.BoNho;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BoNhoMapper {
    BoNhoDto toDto(BoNho boNho);

    List<BoNhoDto> toDtos(List<BoNho> entities);

    Set<BoNhoDto> toDtoSet(Set<BoNho> entities);

    BoNho toEntity(BoNhoDto dto);
}
