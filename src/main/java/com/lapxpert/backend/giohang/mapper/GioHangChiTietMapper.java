package com.lapxpert.backend.giohang.mapper;

import com.lapxpert.backend.giohang.dto.GioHangChiTietDto;
import com.lapxpert.backend.giohang.entity.GioHangChiTiet;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for GioHangChiTiet entity and DTO conversion
 * Follows established mapper patterns with Vietnamese naming conventions
 * Handles product information and price calculations
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GioHangChiTietMapper {

    /**
     * Convert GioHangChiTiet entity to DTO
     * @param gioHangChiTiet entity to convert
     * @return converted DTO
     */
    @Mapping(target = "sanPhamChiTietId", source = "sanPhamChiTiet.id")
    @Mapping(target = "tenSanPham", source = "sanPhamChiTiet.sanPham.tenSanPham")
    @Mapping(target = "serialNumber", ignore = true)
    @Mapping(target = "hinhAnh", expression = "java(getFirstImage(gioHangChiTiet))")
    @Mapping(target = "mauSac", expression = "java(getMauSacDescription(gioHangChiTiet))")
    @Mapping(target = "giaHienTai", source = "sanPhamChiTiet.giaBan")
    @Mapping(target = "thanhTien", expression = "java(calculateThanhTien(gioHangChiTiet))")
    @Mapping(target = "isAvailable", expression = "java(checkSanPhamConHang(gioHangChiTiet))")
    @Mapping(target = "hasPriceChanged", expression = "java(checkCoThayDoiGia(gioHangChiTiet))")
    @Mapping(target = "priceChangeDifference", expression = "java(calculateChenhLechGia(gioHangChiTiet))")
    @Mapping(target = "availabilityStatus", expression = "java(getAvailabilityStatus(gioHangChiTiet))")
    GioHangChiTietDto toDto(GioHangChiTiet gioHangChiTiet);

    /**
     * Convert DTO to GioHangChiTiet entity
     * @param gioHangChiTietDto DTO to convert
     * @return converted entity
     */
    @Mapping(target = "gioHang", ignore = true) // Set separately in service
    @Mapping(target = "sanPhamChiTiet", ignore = true) // Set separately in service
    GioHangChiTiet toEntity(GioHangChiTietDto gioHangChiTietDto);

    /**
     * Convert list of entities to DTOs
     * @param gioHangChiTiets list of entities
     * @return list of DTOs
     */
    List<GioHangChiTietDto> toDtoList(List<GioHangChiTiet> gioHangChiTiets);

    /**
     * Convert list of DTOs to entities
     * @param gioHangChiTietDtos list of DTOs
     * @return list of entities
     */
    List<GioHangChiTiet> toEntityList(List<GioHangChiTietDto> gioHangChiTietDtos);

    /**
     * Update existing entity with DTO data
     * @param gioHangChiTietDto source DTO
     * @param gioHangChiTiet target entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gioHang", ignore = true)
    @Mapping(target = "sanPhamChiTiet", ignore = true)
    void updateEntityFromDto(GioHangChiTietDto gioHangChiTietDto, @MappingTarget GioHangChiTiet gioHangChiTiet);

    // Business logic calculation methods

    /**
     * Calculate total amount for this cart item
     * @param gioHangChiTiet cart item entity
     * @return total amount (quantity * price at time of addition)
     */
    default BigDecimal calculateThanhTien(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getGiaTaiThoiDiemThem() == null ||
            gioHangChiTiet.getSoLuong() == null) {
            return BigDecimal.ZERO;
        }

        return gioHangChiTiet.getGiaTaiThoiDiemThem()
            .multiply(BigDecimal.valueOf(gioHangChiTiet.getSoLuong()));
    }

    /**
     * Check if the product price has changed since adding to cart
     * @param gioHangChiTiet cart item entity
     * @return true if price has changed
     */
    default boolean checkCoThayDoiGia(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null ||
            gioHangChiTiet.getGiaTaiThoiDiemThem() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getGiaBan() == null) {
            return false;
        }

        return !gioHangChiTiet.getGiaTaiThoiDiemThem()
            .equals(gioHangChiTiet.getSanPhamChiTiet().getGiaBan());
    }

    /**
     * Check if the product is still available
     * @param gioHangChiTiet cart item entity
     * @return true if product is available
     */
    default boolean checkSanPhamConHang(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null) {
            return false;
        }

        return Boolean.TRUE.equals(gioHangChiTiet.getSanPhamChiTiet().getTrangThai());
    }

    /**
     * Calculate price difference between cart price and current price
     * @param gioHangChiTiet cart item entity
     * @return price difference (positive if current price is higher, negative if lower)
     */
    default BigDecimal calculateChenhLechGia(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null ||
            gioHangChiTiet.getGiaTaiThoiDiemThem() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getGiaBan() == null) {
            return BigDecimal.ZERO;
        }

        return gioHangChiTiet.getSanPhamChiTiet().getGiaBan()
            .subtract(gioHangChiTiet.getGiaTaiThoiDiemThem());
    }

    /**
     * Get first image from product image list
     * @param gioHangChiTiet cart item entity
     * @return first image URL or null if no images
     */
    default String getFirstImage(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getSanPham() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getSanPham().getHinhAnh() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getSanPham().getHinhAnh().isEmpty()) {
            return null;
        }

        return gioHangChiTiet.getSanPhamChiTiet().getSanPham().getHinhAnh().get(0);
    }

    /**
     * Get availability status as human-readable string
     * @param gioHangChiTiet cart item entity
     * @return availability status string
     */
    default String getAvailabilityStatus(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null) {
            return "Không xác định";
        }

        if (Boolean.TRUE.equals(gioHangChiTiet.getSanPhamChiTiet().getTrangThai())) {
            return "Còn hàng";
        } else {
            return "Hết hàng";
        }
    }

    /**
     * Get color description from MauSac entity
     * @param gioHangChiTiet cart item entity
     * @return color description or null if no color
     */
    default String getMauSacDescription(GioHangChiTiet gioHangChiTiet) {
        if (gioHangChiTiet == null || gioHangChiTiet.getSanPhamChiTiet() == null ||
            gioHangChiTiet.getSanPhamChiTiet().getMauSac() == null) {
            return null;
        }

        return gioHangChiTiet.getSanPhamChiTiet().getMauSac().getMoTaMauSac();
    }
}
