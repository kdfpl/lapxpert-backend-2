package com.lapxpert.backend.dotgiamgia.dto;

import com.lapxpert.backend.dotgiamgia.entity.DotGiamGia;
import com.lapxpert.backend.dotgiamgia.entity.DotGiamGiaAuditHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DotGiamGiaMapper {

    @Mapping(target = "businessTimezone", ignore = true)
    @Mapping(target = "lyDoThayDoi", ignore = true)
    @Mapping(target = "ngayBatDauVietnam", ignore = true)
    @Mapping(target = "ngayCapNhatVietnam", ignore = true)
    @Mapping(target = "ngayKetThucVietnam", ignore = true)
    @Mapping(target = "ngayTaoVietnam", ignore = true)
    DotGiamGiaDto toDto(DotGiamGia entity);

    List<DotGiamGiaDto> toDtos(List<DotGiamGia> entities);

    @Mapping(target = "sanPhamChiTiets", ignore = true)
    @Mapping(target = "statusManuallySet", ignore = true)
    DotGiamGia toEntity(DotGiamGiaDto dto);

    // Audit History Mapping
    @Mapping(target = "hanhDongDisplay", ignore = true)
    @Mapping(target = "thoiGianThayDoiVietnam", ignore = true)
    @Mapping(target = "chiTietThayDoi", ignore = true)
    DotGiamGiaAuditHistoryDto toAuditHistoryDto(DotGiamGiaAuditHistory entity);

    List<DotGiamGiaAuditHistoryDto> toAuditHistoryDtos(List<DotGiamGiaAuditHistory> entities);
}
