package com.lapxpert.backend.nguoidung.application.mapper;

import com.lapxpert.backend.nguoidung.application.dto.DiaChiDto;
import com.lapxpert.backend.nguoidung.domain.entity.DiaChi;
import com.lapxpert.backend.nguoidung.domain.entity.NguoiDung;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiaChiMapper {

    @Mapping(source = "nguoiDung.id", target = "nguoiDungId")
    DiaChiDto toDto(DiaChi diaChi);

    @Mapping(source = "nguoiDungId", target = "nguoiDung", qualifiedByName = "nguoiDungFromId")
    @Mapping(target = "ngayTao", ignore = true)
    @Mapping(target = "ngayCapNhat", ignore = true)
    DiaChi toEntity(DiaChiDto diaChiDto);

    List<DiaChiDto> toDtoList(List<DiaChi> diaChis);
    List<DiaChi> toEntityList(List<DiaChiDto> diaChiDtos);

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
