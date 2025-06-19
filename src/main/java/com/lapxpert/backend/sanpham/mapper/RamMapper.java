package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.RamDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.Ram;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RamMapper {
    RamDto toDto(Ram ram);

    List<RamDto> toDtos(List<Ram> entities);

    Set<RamDto> toDtoSet(Set<Ram> entities);

    Ram toEntity(RamDto dto);
}
