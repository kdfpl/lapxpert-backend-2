package com.lapxpert.backend.hoadon.domain.mapper;

import com.lapxpert.backend.hoadon.domain.dto.HoaDonDto;
import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.nguoidung.application.mapper.DiaChiMapper;
import com.lapxpert.backend.nguoidung.application.mapper.NguoiDungMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {HoaDonChiTietMapper.class, DiaChiMapper.class, NguoiDungMapper.class})
public interface HoaDonMapper {

    HoaDonMapper INSTANCE = Mappers.getMapper(HoaDonMapper.class);

    @Mapping(source = "khachHang.id", target = "khachHangId")
    @Mapping(source = "nhanVien.id", target = "nhanVienId")
    @Mapping(source = "diaChiGiaoHang.id", target = "diaChiGiaoHangId")
    @Mapping(source = "diaChiGiaoHang", target = "diaChiGiaoHang")
    @Mapping(source = "hoaDonChiTiets", target = "chiTiet")
    @Mapping(source = "khachHang", target = "khachHang")
    @Mapping(source = "nhanVien", target = "nhanVien")
    @Mapping(target = "voucherCodes", ignore = true)
    HoaDonDto toDto(HoaDon hoaDon);

    @Mapping(source = "khachHangId", target = "khachHang.id")
    @Mapping(source = "nhanVienId", target = "nhanVien.id")
    @Mapping(source = "diaChiGiaoHangId", target = "diaChiGiaoHang.id")
    @Mapping(source = "chiTiet", target = "hoaDonChiTiets")
    @Mapping(target = "nguoiCapNhat", ignore = true)
    @Mapping(target = "nguoiTao", ignore = true)
    @Mapping(target = "hoaDonPhieuGiamGias", ignore = true)
    @Mapping(target = "maVanDon", ignore = true)
    @Mapping(target = "ngayDuKienGiaoHang", ignore = true)
    @Mapping(target = "khachHang", ignore = true) // Full object ignored for entity mapping
    @Mapping(target = "nhanVien", ignore = true) // Full object ignored for entity mapping
    HoaDon toEntity(HoaDonDto hoaDonDto);

    List<HoaDonDto> toDtoList(List<HoaDon> hoaDonList);

    List<HoaDon> toEntityList(List<HoaDonDto> hoaDonDtoList);
}
