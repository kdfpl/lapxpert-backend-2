package com.lapxpert.backend.sanpham.mapper;

import com.lapxpert.backend.sanpham.dto.SerialNumberDto;
import com.lapxpert.backend.sanpham.entity.SerialNumber;
import com.lapxpert.backend.sanpham.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.enums.TrangThaiSerialNumber;
import org.mapstruct.*;

import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for SerialNumber entity and DTO conversion.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SerialNumberMapper {

    @Named("toDto")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    @Mapping(source = "sanPhamChiTiet.sanPham.tenSanPham", target = "tenSanPham")
    @Mapping(source = "sanPhamChiTiet.sanPham.maSanPham", target = "maSanPham")
    @Mapping(source = "sanPhamChiTiet", target = "tenBienThe", qualifiedByName = "mapVariantName")
    @Mapping(source = "trangThai", target = "trangThaiDisplay", qualifiedByName = "mapStatusDisplay")
    @Mapping(source = "trangThai", target = "trangThaiSeverity", qualifiedByName = "mapStatusSeverity")
    @Mapping(target = "isExpiredReservation", ignore = true)
    @Mapping(target = "isWarrantyExpiringSoon", ignore = true)
    @Mapping(target = "daysUntilWarrantyExpiry", ignore = true)
    SerialNumberDto toDto(SerialNumber serialNumber);

    @Mapping(source = "sanPhamChiTietId", target = "sanPhamChiTiet.id")
    @Mapping(target = "sanPhamChiTiet.sanPham", ignore = true)
    @Mapping(target = "id", ignore = true)
    SerialNumber toEntity(SerialNumberDto serialNumberDto);

    @IterableMapping(qualifiedByName = "toDto")
    List<SerialNumberDto> toDtoList(List<SerialNumber> serialNumbers);

    List<SerialNumber> toEntityList(List<SerialNumberDto> serialNumberDtos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serialNumberValue", ignore = true) // Don't allow changing serial number value
    @Mapping(target = "sanPhamChiTiet", ignore = true) // Don't allow changing variant
    @Mapping(target = "ngayTao", ignore = true)
    @Mapping(target = "nguoiTao", ignore = true)
    void partialUpdate(SerialNumberDto serialNumberDto, @MappingTarget SerialNumber serialNumber);

    // Custom mapping methods

    @Named("mapVariantName")
    default String mapVariantName(SanPhamChiTiet sanPhamChiTiet) {
        if (sanPhamChiTiet == null || sanPhamChiTiet.getSanPham() == null) {
            return "";
        }

        StringBuilder variantName = new StringBuilder();
        variantName.append(sanPhamChiTiet.getSanPham().getTenSanPham());

        // Add variant specifications
        if (sanPhamChiTiet.getRam() != null) {
            variantName.append(" - ").append(sanPhamChiTiet.getRam().getMoTaRam());
        }
        if (sanPhamChiTiet.getBoNho() != null) {
            variantName.append("/").append(sanPhamChiTiet.getBoNho().getMoTaBoNho());
        }
        if (sanPhamChiTiet.getMauSac() != null) {
            variantName.append(" - ").append(sanPhamChiTiet.getMauSac().getMoTaMauSac());
        }

        return variantName.toString();
    }

    @Named("mapStatusDisplay")
    default String mapStatusDisplay(TrangThaiSerialNumber trangThai) {
        return trangThai != null ? trangThai.getDescription() : "";
    }

    @Named("mapStatusSeverity")
    default String mapStatusSeverity(TrangThaiSerialNumber trangThai) {
        return trangThai != null ? trangThai.getSeverity() : "secondary";
    }

    @Named("mapExpiredReservation")
    default boolean mapExpiredReservation(SerialNumber serialNumber) {
        return serialNumber.isReservationExpired();
    }

    @Named("mapWarrantyExpiring")
    default boolean mapWarrantyExpiring(SerialNumber serialNumber) {
        if (serialNumber.getNgayHetBaoHanh() == null) return false;
        Instant thirtyDaysFromNow = Instant.now().plusSeconds(30 * 24 * 60 * 60); // 30 days
        return serialNumber.getNgayHetBaoHanh().isBefore(thirtyDaysFromNow);
    }

    @Named("mapWarrantyDays")
    default long mapWarrantyDays(SerialNumber serialNumber) {
        if (serialNumber.getNgayHetBaoHanh() == null) return -1;
        return java.time.Duration.between(Instant.now(), serialNumber.getNgayHetBaoHanh()).toDays();
    }

    // Specialized mapping methods for different use cases

    /**
     * Map for inventory display (minimal information)
     */
    @Named("toInventoryDto")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    @Mapping(source = "sanPhamChiTiet.sanPham.tenSanPham", target = "tenSanPham")
    @Mapping(source = "trangThai", target = "trangThaiDisplay", qualifiedByName = "mapStatusDisplay")
    @Mapping(source = "trangThai", target = "trangThaiSeverity", qualifiedByName = "mapStatusSeverity")
    @Mapping(target = "tenBienThe", ignore = true)
    @Mapping(target = "isExpiredReservation", ignore = true)
    @Mapping(target = "isWarrantyExpiringSoon", ignore = true)
    @Mapping(target = "daysUntilWarrantyExpiry", ignore = true)
    SerialNumberDto toInventoryDto(SerialNumber serialNumber);

    /**
     * Map for order display (includes reservation info)
     */
    @Named("toOrderDto")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    @Mapping(source = "sanPhamChiTiet.sanPham.tenSanPham", target = "tenSanPham")
    @Mapping(source = "sanPhamChiTiet", target = "tenBienThe", qualifiedByName = "mapVariantName")
    @Mapping(source = "trangThai", target = "trangThaiDisplay", qualifiedByName = "mapStatusDisplay")
    @Mapping(source = "trangThai", target = "trangThaiSeverity", qualifiedByName = "mapStatusSeverity")
    @Mapping(target = "isExpiredReservation", ignore = true)
    @Mapping(target = "isWarrantyExpiringSoon", ignore = true)
    @Mapping(target = "daysUntilWarrantyExpiry", ignore = true)
    SerialNumberDto toOrderDto(SerialNumber serialNumber);

    /**
     * Map for warranty tracking (includes warranty info)
     */
    @Named("toWarrantyDto")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    @Mapping(source = "sanPhamChiTiet.sanPham.tenSanPham", target = "tenSanPham")
    @Mapping(source = "sanPhamChiTiet", target = "tenBienThe", qualifiedByName = "mapVariantName")
    @Mapping(source = "trangThai", target = "trangThaiDisplay", qualifiedByName = "mapStatusDisplay")
    @Mapping(source = "trangThai", target = "trangThaiSeverity", qualifiedByName = "mapStatusSeverity")
    @Mapping(target = "isWarrantyExpiringSoon", ignore = true)
    @Mapping(target = "daysUntilWarrantyExpiry", ignore = true)
    @Mapping(target = "isExpiredReservation", ignore = true)
    SerialNumberDto toWarrantyDto(SerialNumber serialNumber);

    /**
     * Map for bulk operations (minimal information)
     */
    @Named("toBulkDto")
    @Mapping(source = "sanPhamChiTiet.id", target = "sanPhamChiTietId")
    @Mapping(source = "trangThai", target = "trangThaiDisplay", qualifiedByName = "mapStatusDisplay")
    @Mapping(target = "tenSanPham", ignore = true)
    @Mapping(target = "tenBienThe", ignore = true)
    @Mapping(target = "trangThaiSeverity", ignore = true)
    @Mapping(target = "isExpiredReservation", ignore = true)
    @Mapping(target = "isWarrantyExpiringSoon", ignore = true)
    @Mapping(target = "daysUntilWarrantyExpiry", ignore = true)
    SerialNumberDto toBulkDto(SerialNumber serialNumber);

    // List mapping methods for specialized use cases

    @IterableMapping(qualifiedByName = "toInventoryDto")
    List<SerialNumberDto> toInventoryDtoList(List<SerialNumber> serialNumbers);

    @IterableMapping(qualifiedByName = "toOrderDto")
    List<SerialNumberDto> toOrderDtoList(List<SerialNumber> serialNumbers);

    @IterableMapping(qualifiedByName = "toWarrantyDto")
    List<SerialNumberDto> toWarrantyDtoList(List<SerialNumber> serialNumbers);

    @IterableMapping(qualifiedByName = "toBulkDto")
    List<SerialNumberDto> toBulkDtoList(List<SerialNumber> serialNumbers);
}
