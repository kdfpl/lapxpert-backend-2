package com.lapxpert.backend.phieugiamgia.application.mapper;

import com.lapxpert.backend.common.service.VietnamTimeZoneService;
import com.lapxpert.backend.phieugiamgia.application.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.domain.entity.PhieuGiamGia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service-based mapper for PhieuGiamGia with timezone support
 * Provides enhanced mapping with Vietnam timezone formatting
 * Complements the MapStruct mapper with timezone-aware functionality
 */
@Component
@RequiredArgsConstructor
public class PhieuGiamGiaDtoMapper {
    
    private final VietnamTimeZoneService vietnamTimeZoneService;
    private final PhieuGiamGiaMapper phieuGiamGiaMapper;
    
    /**
     * Convert PhieuGiamGia entity to DTO with Vietnam timezone formatting
     * Provides both UTC timestamps and Vietnam-formatted strings
     */
    public PhieuGiamGiaDto toDtoWithTimezone(PhieuGiamGia entity) {
        if (entity == null) {
            return null;
        }
        
        // Use MapStruct for basic mapping
        PhieuGiamGiaDto dto = phieuGiamGiaMapper.toDto(entity);
        
        // Add Vietnam timezone formatted strings
        dto.setNgayBatDauVietnam(vietnamTimeZoneService.formatAsVietnamDateTime(entity.getNgayBatDau()));
        dto.setNgayKetThucVietnam(vietnamTimeZoneService.formatAsVietnamDateTime(entity.getNgayKetThuc()));
        dto.setNgayTaoVietnam(vietnamTimeZoneService.formatAsVietnamDateTime(entity.getNgayTao()));
        dto.setNgayCapNhatVietnam(vietnamTimeZoneService.formatAsVietnamDateTime(entity.getNgayCapNhat()));
        dto.setBusinessTimezone("Asia/Ho_Chi_Minh");
        
        return dto;
    }
    
    /**
     * Convert list of entities to DTOs with timezone formatting
     */
    public List<PhieuGiamGiaDto> toDtosWithTimezone(List<PhieuGiamGia> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(this::toDtoWithTimezone)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert DTO to entity (timezone formatting not needed for entity)
     */
    public PhieuGiamGia toEntity(PhieuGiamGiaDto dto) {
        return phieuGiamGiaMapper.toEntity(dto);
    }
    
    /**
     * Convert list of DTOs to entities
     */
    public List<PhieuGiamGia> toEntities(List<PhieuGiamGiaDto> dtos) {
        return phieuGiamGiaMapper.toEntities(dtos);
    }
    
    /**
     * Create DTO for email notifications with Vietnam time formatting
     * Optimized for email template usage
     */
    public PhieuGiamGiaDto toDtoForEmail(PhieuGiamGia entity) {
        PhieuGiamGiaDto dto = toDtoWithTimezone(entity);
        
        // Add additional formatting for email templates if needed
        // For example, could add formatted discount amount, etc.
        
        return dto;
    }
}
