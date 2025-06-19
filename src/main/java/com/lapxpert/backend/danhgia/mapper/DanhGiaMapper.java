package com.lapxpert.backend.danhgia.mapper;

import com.lapxpert.backend.danhgia.dto.CreateReviewDto;
import com.lapxpert.backend.danhgia.dto.DanhGiaDto;
import com.lapxpert.backend.danhgia.dto.UpdateReviewDto;
import com.lapxpert.backend.danhgia.entity.DanhGia;
import com.lapxpert.backend.nguoidung.entity.NguoiDung;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPham;
import com.lapxpert.backend.hoadon.entity.HoaDonChiTiet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for DanhGia entity and DTOs
 * Provides comprehensive mapping between entity and different DTO types
 * Follows established mapper patterns with Vietnamese naming conventions
 */
@Mapper(componentModel = "spring")
public interface DanhGiaMapper {

    /**
     * Convert DanhGia entity to DanhGiaDto with all business logic fields
     * @param entity DanhGia entity to convert
     * @return converted DTO with calculated fields
     */
    @Mapping(source = "nguoiDung.id", target = "nguoiDungId")
    @Mapping(source = "nguoiDung.hoTen", target = "tenNguoiDung")
    @Mapping(source = "sanPham.id", target = "sanPhamId")
    @Mapping(source = "sanPham.tenSanPham", target = "tenSanPham")
    @Mapping(source = "hoaDonChiTiet.id", target = "hoaDonChiTietId")
    @Mapping(source = "hoaDonChiTiet.hoaDon.ngayTao", target = "purchaseDate")
    @Mapping(target = "isVerifiedPurchase", expression = "java(entity.isVerifiedPurchase())")
    @Mapping(target = "hasImages", expression = "java(!entity.getHinhAnh().isEmpty())")
    @Mapping(target = "imageCount", expression = "java(entity.getHinhAnh().size())")
    @Mapping(target = "isVisible", expression = "java(entity.isVisible())")
    @Mapping(target = "isPending", expression = "java(entity.getTrangThai().isPending())")
    @Mapping(target = "helpfulVotes", constant = "0") // Will be implemented in future enhancement
    @Mapping(target = "totalVotes", constant = "0")   // Will be implemented in future enhancement
    @Mapping(target = "moderatorNote", ignore = true) // Set separately for admin operations
    DanhGiaDto toDto(DanhGia entity);

    /**
     * Convert CreateReviewDto to DanhGia entity
     * Entity references will be set separately in service layer
     * @param dto CreateReviewDto to convert
     * @return converted entity ready for persistence
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nguoiDung", ignore = true)     // Set in service using nguoiDungId
    @Mapping(target = "sanPham", ignore = true)       // Set in service using sanPhamId
    @Mapping(target = "hoaDonChiTiet", ignore = true) // Set in service using hoaDonChiTietId
    @Mapping(target = "trangThai", ignore = true)     // Set by auto-moderation logic
    DanhGia toEntity(CreateReviewDto dto);

    /**
     * Update existing DanhGia entity from UpdateReviewDto
     * Only updates modifiable fields, preserves audit and relationship data
     * @param dto UpdateReviewDto with updated values
     * @param entity existing DanhGia entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nguoiDung", ignore = true)     // Preserve existing relationship
    @Mapping(target = "sanPham", ignore = true)       // Preserve existing relationship
    @Mapping(target = "hoaDonChiTiet", ignore = true) // Preserve existing relationship
    @Mapping(target = "trangThai", ignore = true)     // Updated by auto-moderation
    @Mapping(target = "nguoiTao", ignore = true)      // Handled by audit
    @Mapping(target = "nguoiCapNhat", ignore = true)  // Handled by audit
    @Mapping(target = "ngayTao", ignore = true)       // Handled by audit
    @Mapping(target = "ngayCapNhat", ignore = true)   // Handled by audit
    void updateEntityFromDto(UpdateReviewDto dto, @org.mapstruct.MappingTarget DanhGia entity);

    /**
     * Convert list of DanhGia entities to DTOs
     * @param entities list of entities to convert
     * @return list of converted DTOs
     */
    List<DanhGiaDto> toDtoList(List<DanhGia> entities);

    /**
     * Helper method to create NguoiDung reference from ID
     * @param id NguoiDung ID
     * @return NguoiDung entity with only ID set
     */
    @Named("nguoiDungFromId")
    default NguoiDung nguoiDungFromId(Long id) {
        if (id == null) {
            return null;
        }
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setId(id);
        return nguoiDung;
    }

    /**
     * Helper method to create SanPham reference from ID
     * @param id SanPham ID
     * @return SanPham entity with only ID set
     */
    @Named("sanPhamFromId")
    default SanPham sanPhamFromId(Long id) {
        if (id == null) {
            return null;
        }
        SanPham sanPham = new SanPham();
        sanPham.setId(id);
        return sanPham;
    }

    /**
     * Helper method to create HoaDonChiTiet reference from ID
     * @param id HoaDonChiTiet ID
     * @return HoaDonChiTiet entity with only ID set
     */
    @Named("hoaDonChiTietFromId")
    default HoaDonChiTiet hoaDonChiTietFromId(Long id) {
        if (id == null) {
            return null;
        }
        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setId(id);
        return hoaDonChiTiet;
    }
}
