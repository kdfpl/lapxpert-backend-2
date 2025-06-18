package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.SanPhamAuditHistoryDto;
import com.lapxpert.backend.sanpham.entity.SanPhamAuditHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for SanPhamAuditHistory entity to DTO conversion.
 * Handles mapping between audit history entities and DTOs for API responses.
 */
@Mapper(componentModel = "spring")
public interface SanPhamAuditHistoryMapper {

    /**
     * Convert SanPhamAuditHistory entity to DTO
     * @param entity SanPhamAuditHistory entity
     * @return SanPhamAuditHistoryDto
     */
    @Mapping(target = "hanhDongDisplay", ignore = true)
    @Mapping(target = "thoiGianThayDoiVietnam", ignore = true)
    @Mapping(target = "chiTietThayDoi", ignore = true)
    SanPhamAuditHistoryDto toDto(SanPhamAuditHistory entity);

    /**
     * Convert list of SanPhamAuditHistory entities to DTOs
     * @param entities List of SanPhamAuditHistory entities
     * @return List of SanPhamAuditHistoryDto
     */
    List<SanPhamAuditHistoryDto> toDtos(List<SanPhamAuditHistory> entities);

    /**
     * Convert SanPhamAuditHistoryDto to entity (for creation)
     * @param dto SanPhamAuditHistoryDto
     * @return SanPhamAuditHistory entity
     */
    @Mapping(target = "id", ignore = true)
    SanPhamAuditHistory toEntity(SanPhamAuditHistoryDto dto);
}
