package com.lapxpert.backend.hoadon.mapper;

import com.lapxpert.backend.hoadon.dto.ThanhToanDto;
import com.lapxpert.backend.hoadon.entity.ThanhToan;
import com.lapxpert.backend.nguoidung.entity.NguoiDung; // For mapping NguoiDung to nguoiDungId
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ThanhToanMapper {

    @Mapping(source = "nguoiDung.id", target = "nguoiDungId")
    ThanhToanDto toDto(ThanhToan thanhToan);

    @Mapping(source = "nguoiDungId", target = "nguoiDung", qualifiedByName = "nguoiDungFromId")
    ThanhToan toEntity(ThanhToanDto thanhToanDto);

    List<ThanhToanDto> toDtoList(List<ThanhToan> thanhToans);
    List<ThanhToan> toEntityList(List<ThanhToanDto> thanhToanDtos);

    @Named("nguoiDungFromId")
    default NguoiDung nguoiDungFromId(Long id) {
        if (id == null) {
            return null;
        }
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setId(id);
        return nguoiDung;
    }
}
