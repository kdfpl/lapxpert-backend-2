package com.lapxpert.backend.hoadon.mapper;

import com.lapxpert.backend.hoadon.dto.HoaDonThanhToanDto;
import com.lapxpert.backend.hoadon.entity.HoaDon;
import com.lapxpert.backend.hoadon.entity.HoaDonThanhToan;
import com.lapxpert.backend.hoadon.entity.ThanhToan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ThanhToanMapper.class}) // uses ThanhToanMapper if nesting ThanhToanDto
public interface HoaDonThanhToanMapper {

    @Mapping(source = "hoaDon.id", target = "hoaDonId")
    @Mapping(source = "thanhToan.id", target = "thanhToanId")
    // If you add ThanhToanDto to HoaDonThanhToanDto:
    // @Mapping(source = "thanhToan", target = "thanhToanDetails") 
    HoaDonThanhToanDto toDto(HoaDonThanhToan hoaDonThanhToan);

    @Mapping(source = "hoaDonId", target = "hoaDon", qualifiedByName = "hoaDonFromId")
    @Mapping(source = "thanhToanId", target = "thanhToan", qualifiedByName = "thanhToanFromId")
    @Mapping(target = "id", ignore = true) // Ignore ID mapping from DTO to Entity
    // If you add ThanhToanDto to HoaDonThanhToanDto:
    // @Mapping(source = "thanhToanDetails", target = "thanhToan")
    HoaDonThanhToan toEntity(HoaDonThanhToanDto hoaDonThanhToanDto);

    List<HoaDonThanhToanDto> toDtoList(List<HoaDonThanhToan> hoaDonThanhToans);
    List<HoaDonThanhToan> toEntityList(List<HoaDonThanhToanDto> hoaDonThanhToanDtos);

    @Named("hoaDonFromId")
    default HoaDon hoaDonFromId(Long id) {
        if (id == null) {
            return null;
        }
        HoaDon hoaDon = new HoaDon();
        hoaDon.setId(id);
        return hoaDon;
    }

    @Named("thanhToanFromId")
    default ThanhToan thanhToanFromId(Long id) {
        if (id == null) {
            return null;
        }
        ThanhToan thanhToan = new ThanhToan();
        thanhToan.setId(id);
        return thanhToan;
    }
}
