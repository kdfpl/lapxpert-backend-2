package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.OCungDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.OCung;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface OCungMapper {
    OCungDto toDto(OCung oCung);

    List<OCungDto> toDtos(List<OCung> entities);

    Set<OCungDto> toDtoSet(Set<OCung> entities);

    OCung toEntity(OCungDto dto);
}
