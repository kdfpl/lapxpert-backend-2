package com.lapxpert.backend.dotgiamgia.application.dto;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DotGiamGiaMapper {
    DotGiamGiaDTO toDto(DotGiamGia entity);

    List<DotGiamGiaDTO> toDtos(List<DotGiamGia> entities);

    DotGiamGia toEntity(DotGiamGiaDTO dto);
}
