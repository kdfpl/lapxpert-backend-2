package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.ThuongHieuDto;
import com.lapxpert.backend.sanpham.domain.entity.thuoctinh.ThuongHieu;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ThuongHieuMapper {
    ThuongHieuDto toDto(ThuongHieu thuongHieu);

    List<ThuongHieuDto> toDtos(List<ThuongHieu> entities);

    Set<ThuongHieuDto> toDtoSet(Set<ThuongHieu> entities);
}
