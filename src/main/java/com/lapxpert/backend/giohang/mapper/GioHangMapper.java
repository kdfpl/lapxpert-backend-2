package com.lapxpert.backend.giohang.mapper;

import com.lapxpert.backend.giohang.dto.GioHangDto;
import com.lapxpert.backend.giohang.entity.GioHang;
import com.lapxpert.backend.giohang.entity.GioHangChiTiet;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for GioHang entity and DTO conversion
 * Follows established mapper patterns with Vietnamese naming conventions
 * Handles complex business logic calculations and relationships
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GioHangMapper {

    /**
     * Convert GioHang entity to DTO
     * @param gioHang entity to convert
     * @return converted DTO
     */
    @Mapping(target = "nguoiDungId", source = "nguoiDung.id")
    @Mapping(target = "tenNguoiDung", source = "nguoiDung.hoTen")
    @Mapping(target = "tongTien", expression = "java(calculateTongTien(gioHang))")
    @Mapping(target = "tongSoLuong", expression = "java(calculateTongSoLuong(gioHang))")
    @Mapping(target = "soLuongSanPhamKhacNhau", expression = "java(calculateSoLuongSanPhamKhacNhau(gioHang))")
    @Mapping(target = "hasExpiredItems", expression = "java(checkHasExpiredItems(gioHang))")
    @Mapping(target = "hasPriceChanges", expression = "java(checkHasPriceChanges(gioHang))")
    @Mapping(target = "hasUnavailableItems", expression = "java(checkHasUnavailableItems(gioHang))")
    GioHangDto toDto(GioHang gioHang);

    /**
     * Convert DTO to GioHang entity
     * @param gioHangDto DTO to convert
     * @return converted entity
     */
    @Mapping(target = "nguoiDung", ignore = true) // Set separately in service
    GioHang toEntity(GioHangDto gioHangDto);

    /**
     * Convert list of entities to DTOs
     * @param gioHangs list of entities
     * @return list of DTOs
     */
    List<GioHangDto> toDtoList(List<GioHang> gioHangs);

    /**
     * Convert list of DTOs to entities
     * @param gioHangDtos list of DTOs
     * @return list of entities
     */
    List<GioHang> toEntityList(List<GioHangDto> gioHangDtos);

    /**
     * Update existing entity with DTO data
     * @param gioHangDto source DTO
     * @param gioHang target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nguoiDung", ignore = true)
    @Mapping(target = "ngayTao", ignore = true)
    @Mapping(target = "ngayCapNhat", ignore = true)
    void updateEntityFromDto(GioHangDto gioHangDto, @MappingTarget GioHang gioHang);

    // Business logic calculation methods

    /**
     * Calculate total amount for all items in cart
     * @param gioHang cart entity
     * @return total amount
     */
    default BigDecimal calculateTongTien(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return BigDecimal.ZERO;
        }

        return gioHang.getChiTiets().stream()
            .map(GioHangChiTiet::getThanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total quantity of all items in cart
     * @param gioHang cart entity
     * @return total quantity
     */
    default Integer calculateTongSoLuong(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return 0;
        }

        return gioHang.getChiTiets().stream()
            .mapToInt(item -> item.getSoLuong() != null ? item.getSoLuong() : 0)
            .sum();
    }

    /**
     * Calculate number of unique products in cart
     * @param gioHang cart entity
     * @return number of unique products
     */
    default Integer calculateSoLuongSanPhamKhacNhau(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return 0;
        }

        return gioHang.getChiTiets().size();
    }

    /**
     * Check if cart has expired items (items added more than 30 days ago)
     * @param gioHang cart entity
     * @return true if cart has expired items
     */
    default boolean checkHasExpiredItems(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return false;
        }

        java.time.Instant thirtyDaysAgo = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        return gioHang.getChiTiets().stream()
            .anyMatch(item -> item.getNgayTao() != null && item.getNgayTao().isBefore(thirtyDaysAgo));
    }

    /**
     * Check if cart has items with price changes
     * @param gioHang cart entity
     * @return true if cart has items with price changes
     */
    default boolean checkHasPriceChanges(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return false;
        }

        return gioHang.getChiTiets().stream()
            .anyMatch(GioHangChiTiet::hasPriceChanged);
    }

    /**
     * Check if cart has unavailable items
     * @param gioHang cart entity
     * @return true if cart has unavailable items
     */
    default boolean checkHasUnavailableItems(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTiets() == null) {
            return false;
        }

        return gioHang.getChiTiets().stream()
            .anyMatch(item -> item.getSanPhamChiTiet() != null &&
                     !Boolean.TRUE.equals(item.getSanPhamChiTiet().getTrangThai()));
    }
}
