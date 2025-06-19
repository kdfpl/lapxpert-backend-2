package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.SanPhamChiTietAuditHistoryDto;
import com.lapxpert.backend.sanpham.entity.SanPhamChiTietAuditHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for SanPhamChiTietAuditHistory entity to DTO conversion.
 * Handles mapping between product variant audit history entities and DTOs for API responses.
 */
@Mapper(componentModel = "spring")
public interface SanPhamChiTietAuditHistoryMapper {

    /**
     * Convert SanPhamChiTietAuditHistory entity to DTO
     * @param entity SanPhamChiTietAuditHistory entity
     * @return SanPhamChiTietAuditHistoryDto
     */
    @Mapping(target = "hanhDongDisplay", ignore = true)
    @Mapping(target = "thoiGianThayDoiVietnam", ignore = true)
    @Mapping(target = "chiTietThayDoi", ignore = true)
    SanPhamChiTietAuditHistoryDto toDto(SanPhamChiTietAuditHistory entity);

    /**
     * Convert list of SanPhamChiTietAuditHistory entities to DTOs
     * @param entities List of SanPhamChiTietAuditHistory entities
     * @return List of SanPhamChiTietAuditHistoryDto
     */
    List<SanPhamChiTietAuditHistoryDto> toDtos(List<SanPhamChiTietAuditHistory> entities);

    /**
     * Convert SanPhamChiTietAuditHistoryDto to entity (for creation)
     * @param dto SanPhamChiTietAuditHistoryDto
     * @return SanPhamChiTietAuditHistory entity
     */
    @Mapping(target = "id", ignore = true)
    SanPhamChiTietAuditHistory toEntity(SanPhamChiTietAuditHistoryDto dto);
}
