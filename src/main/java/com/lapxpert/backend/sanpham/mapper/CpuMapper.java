package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.CpuDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.Cpu;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface CpuMapper {
    CpuDto toDto(Cpu cpu);

    List<CpuDto> toDtos(List<Cpu> entities);

    Set<CpuDto> toDtoSet(Set<Cpu> entities);

    Cpu toEntity(CpuDto dto);
}
