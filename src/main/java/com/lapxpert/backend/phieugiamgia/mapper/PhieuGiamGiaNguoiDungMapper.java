package com.lapxpert.backend.phieugiamgia.mapper;

import com.lapxpert.backend.phieugiamgia.dto.PhieuGiamGiaNguoiDungDto;
import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGiaNguoiDung;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for PhieuGiamGiaNguoiDung entity and DTO conversion.
 * Handles the mapping between voucher-user assignment entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface PhieuGiamGiaNguoiDungMapper {

    /**
     * Convert PhieuGiamGiaNguoiDung entity to DTO
     */
    @Mapping(target = "phieuGiamGiaId", source = "id.phieuGiamGiaId")
    @Mapping(target = "nguoiDungId", source = "id.nguoiDungId")
    PhieuGiamGiaNguoiDungDto toDto(PhieuGiamGiaNguoiDung phieuGiamGiaNguoiDung);

    /**
     * Convert PhieuGiamGiaNguoiDungDto to entity
     * Ignores relationships that should be set separately
     */
    @Mapping(target = "id", ignore = true) // Composite key managed separately
    @Mapping(target = "phieuGiamGia", ignore = true) // Set by service
    @Mapping(target = "nguoiDung", ignore = true) // Set by service
    PhieuGiamGiaNguoiDung toEntity(PhieuGiamGiaNguoiDungDto dto);

    /**
     * Convert list of entities to DTOs
     */
    List<PhieuGiamGiaNguoiDungDto> toDtos(List<PhieuGiamGiaNguoiDung> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<PhieuGiamGiaNguoiDung> toEntities(List<PhieuGiamGiaNguoiDungDto> dtos);
}
