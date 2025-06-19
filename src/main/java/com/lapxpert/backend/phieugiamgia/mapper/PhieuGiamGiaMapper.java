package com.lapxpert.backend.phieugiamgia.mapper;

import com.lapxpert.backend.phieugiamgia.dto.PhieuGiamGiaDto;
import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGia;
import com.lapxpert.backend.phieugiamgia.entity.PhieuGiamGiaNguoiDung;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for PhieuGiamGia entity and DTO conversion.
 * Handles the mapping between voucher campaign entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface PhieuGiamGiaMapper {

    /**
     * Convert PhieuGiamGia entity to DTO
     * Maps campaign status and extracts user IDs from assignments
     * Uses clean enum-based structure without deprecated fields
     */
    default PhieuGiamGiaDto toDto(PhieuGiamGia phieuGiamGia) {
        if (phieuGiamGia == null) {
            return null;
        }

        return PhieuGiamGiaDto.builder()
            .id(phieuGiamGia.getId())
            .maPhieuGiamGia(phieuGiamGia.getMaPhieuGiamGia())
            .loaiGiamGia(phieuGiamGia.getLoaiGiamGia())
            .trangThai(phieuGiamGia.getTrangThai())
            .giaTriGiam(phieuGiamGia.getGiaTriGiam())
            .giaTriDonHangToiThieu(phieuGiamGia.getGiaTriDonHangToiThieu())
            .ngayBatDau(phieuGiamGia.getNgayBatDau())
            .ngayKetThuc(phieuGiamGia.getNgayKetThuc())
            .moTa(phieuGiamGia.getMoTa())
            .soLuongBanDau(phieuGiamGia.getSoLuongBanDau())
            .soLuongDaDung(phieuGiamGia.getSoLuongDaDung())
            .ngayTao(phieuGiamGia.getNgayTao())
            .ngayCapNhat(phieuGiamGia.getNgayCapNhat())
            .danhSachNguoiDung(extractUserIds(phieuGiamGia.getDanhSachNguoiDung()))
            // Include audit fields from AdminAuditableEntity
            .nguoiTao(phieuGiamGia.getNguoiTao())
            .nguoiCapNhat(phieuGiamGia.getNguoiCapNhat())
            .build();
    }

    /**
     * Convert PhieuGiamGiaDto to entity
     * Ignores audit fields and relationships that should be managed separately
     * Uses clean enum-based structure
     */
    default PhieuGiamGia toEntity(PhieuGiamGiaDto dto) {
        if (dto == null) {
            return null;
        }

        return PhieuGiamGia.builder()
            .id(dto.getId())
            .maPhieuGiamGia(dto.getMaPhieuGiamGia())
            .loaiGiamGia(dto.getLoaiGiamGia())
            .trangThai(dto.getTrangThai())
            .giaTriGiam(dto.getGiaTriGiam())
            .giaTriDonHangToiThieu(dto.getGiaTriDonHangToiThieu())
            .ngayBatDau(dto.getNgayBatDau())
            .ngayKetThuc(dto.getNgayKetThuc())
            .moTa(dto.getMoTa())
            .soLuongBanDau(dto.getSoLuongBanDau())
            .soLuongDaDung(dto.getSoLuongDaDung())
            // Ignore danhSachNguoiDung and hoaDonPhieuGiamGias - managed separately
            .build();
    }

    /**
     * Convert list of entities to DTOs
     */
    List<PhieuGiamGiaDto> toDtos(List<PhieuGiamGia> entities);

    /**
     * Convert list of DTOs to entities
     */
    List<PhieuGiamGia> toEntities(List<PhieuGiamGiaDto> dtos);

    /**
     * Extract user IDs from PhieuGiamGiaNguoiDung relationships
     * Used for mapping entity relationships to simple ID list in DTO
     */
    @Named("extractUserIds")
    default List<Long> extractUserIds(List<PhieuGiamGiaNguoiDung> assignments) {
        if (assignments == null) {
            return null;
        }
        return assignments.stream()
                .map(assignment -> assignment.getNguoiDung().getId())
                .collect(Collectors.toList());
    }
}
