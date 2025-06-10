package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.BoNhoDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.BoNho;
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
