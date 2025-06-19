package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.MauSacDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.MauSac;
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
