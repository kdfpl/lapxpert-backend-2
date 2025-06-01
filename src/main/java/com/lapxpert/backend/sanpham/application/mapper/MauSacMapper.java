package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.thuoctinh.MauSacDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.MauSac;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface MauSacMapper {
    MauSacDto toDto(MauSac mauSac);

    List<MauSacDto> toDtos(List<MauSac> entities);

    Set<MauSacDto> toDtoSet(Set<MauSac> entities);

    MauSac toEntity(MauSacDto dto);
}
