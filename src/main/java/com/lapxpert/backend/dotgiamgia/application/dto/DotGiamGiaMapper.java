package com.lapxpert.backend.dotgiamgia.application.dto;

import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGia;
import com.lapxpert.backend.dotgiamgia.domain.entity.DotGiamGiaAuditHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DotGiamGiaMapper {

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
