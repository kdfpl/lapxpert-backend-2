package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.thuoctinh.ThuongHieuDto;
import com.lapxpert.backend.sanpham.entity.thuoctinh.ThuongHieu;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ThuongHieuMapper {
    ThuongHieuDto toDto(ThuongHieu thuongHieu);

    List<ThuongHieuDto> toDtos(List<ThuongHieu> entities);

    Set<ThuongHieuDto> toDtoSet(Set<ThuongHieu> entities);
}
