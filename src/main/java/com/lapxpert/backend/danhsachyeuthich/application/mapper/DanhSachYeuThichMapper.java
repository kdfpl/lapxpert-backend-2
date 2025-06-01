package com.lapxpert.backend.danhsachyeuthich.application.mapper;

import com.lapxpert.backend.danhsachyeuthich.application.dto.DanhSachYeuThichDto;
import com.lapxpert.backend.danhsachyeuthich.application.dto.SanPhamChiTietSummaryDto;
import com.lapxpert.backend.danhsachyeuthich.domain.entity.DanhSachYeuThich;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPham;
import com.lapxpert.backend.sanpham.domain.entity.sanpham.SanPhamChiTiet;
import com.lapxpert.backend.sanpham.domain.enums.TrangThaiSanPham;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for DanhSachYeuThich entity and DTO conversion
 * Provides comprehensive mapping with business logic calculations
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DanhSachYeuThichMapper {

    /**
     * Convert entity to DTO with business logic calculations
     * @param entity the wishlist entity
     * @return the wishlist DTO
     */
    @Mapping(target = "nguoiDungId", source = "nguoiDung.id")
    @Mapping(target = "sanPhamId", source = "sanPham.id")
    @Mapping(target = "tenSanPham", source = "sanPham.tenSanPham")
    @Mapping(target = "maSanPham", source = "sanPham.maSanPham")
    @Mapping(target = "moTa", source = "sanPham.moTa")
    @Mapping(target = "hinhAnh", source = "sanPham.hinhAnh", qualifiedByName = "getFirstImage")
    @Mapping(target = "giaThapNhat", source = "sanPham", qualifiedByName = "calculateMinPrice")
    @Mapping(target = "giaCaoNhat", source = "sanPham", qualifiedByName = "calculateMaxPrice")
    @Mapping(target = "giaKhiThem", source = ".", qualifiedByName = "getOriginalPrice")
    @Mapping(target = "giaHienTai", source = "sanPham", qualifiedByName = "calculateCurrentMinPrice")
    @Mapping(target = "isAvailable", source = ".", qualifiedByName = "checkAvailability")
    @Mapping(target = "hasPriceDropped", source = ".", qualifiedByName = "checkPriceDrop")
    @Mapping(target = "priceDropPercentage", source = ".", qualifiedByName = "calculatePriceDropPercentage")
    @Mapping(target = "availableVariantCount", source = "sanPham", qualifiedByName = "countAvailableVariants")
    @Mapping(target = "hasActiveDiscount", source = "sanPham", qualifiedByName = "checkActiveDiscount")
    @Mapping(target = "availabilityStatus", source = ".", qualifiedByName = "getAvailabilityStatus")
    @Mapping(target = "availableVariants", source = "sanPham", qualifiedByName = "getAvailableVariants")
    DanhSachYeuThichDto toDto(DanhSachYeuThich entity);

    /**
     * Convert DTO to entity (basic mapping)
     * @param dto the wishlist DTO
     * @return the wishlist entity
     */
    @Mapping(target = "nguoiDung", ignore = true)
    @Mapping(target = "sanPham", ignore = true)
    DanhSachYeuThich toEntity(DanhSachYeuThichDto dto);

    /**
     * Convert list of entities to DTOs
     * @param entities list of wishlist entities
     * @return list of wishlist DTOs
     */
    List<DanhSachYeuThichDto> toDtoList(List<DanhSachYeuThich> entities);

    /**
     * Get first image from product image list
     * @param hinhAnh list of image URLs
     * @return first image URL or null
     */
    @Named("getFirstImage")
    default String getFirstImage(List<String> hinhAnh) {
        return (hinhAnh != null && !hinhAnh.isEmpty()) ? hinhAnh.get(0) : null;
    }

    /**
     * Calculate minimum price from product variants
     * @param sanPham the product
     * @return minimum price
     */
    @Named("calculateMinPrice")
    default BigDecimal calculateMinPrice(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null || sanPham.getSanPhamChiTiets().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return sanPham.getSanPhamChiTiets().stream()
                .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
                .map(SanPhamChiTiet::getGiaBan)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate maximum price from product variants
     * @param sanPham the product
     * @return maximum price
     */
    @Named("calculateMaxPrice")
    default BigDecimal calculateMaxPrice(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null || sanPham.getSanPhamChiTiets().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return sanPham.getSanPhamChiTiets().stream()
                .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
                .map(SanPhamChiTiet::getGiaBan)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate current minimum price (considering promotional prices)
     * @param sanPham the product
     * @return current minimum price
     */
    @Named("calculateCurrentMinPrice")
    default BigDecimal calculateCurrentMinPrice(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null || sanPham.getSanPhamChiTiets().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return sanPham.getSanPhamChiTiets().stream()
                .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
                .map(variant -> {
                    BigDecimal promotionalPrice = variant.getGiaKhuyenMai();
                    BigDecimal regularPrice = variant.getGiaBan();
                    return (promotionalPrice != null && promotionalPrice.compareTo(BigDecimal.ZERO) > 0)
                           ? promotionalPrice : regularPrice;
                })
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get original price when item was added to wishlist
     * Uses stored price if available, otherwise calculates current min price
     * @param wishlistItem the wishlist item
     * @return original price when added
     */
    @Named("getOriginalPrice")
    default BigDecimal getOriginalPrice(DanhSachYeuThich wishlistItem) {
        if (wishlistItem.getSanPham() == null) {
            return BigDecimal.ZERO;
        }

        // Use stored price if available
        if (wishlistItem.getGiaKhiThem() != null &&
            wishlistItem.getGiaKhiThem().compareTo(BigDecimal.ZERO) > 0) {
            return wishlistItem.getGiaKhiThem();
        }

        // Fallback to current min price for existing records
        return calculateMinPrice(wishlistItem.getSanPham());
    }

    /**
     * Check if product is available
     * @param wishlistItem the wishlist item
     * @return true if product is available
     */
    @Named("checkAvailability")
    default boolean checkAvailability(DanhSachYeuThich wishlistItem) {
        return wishlistItem.isProductAvailable();
    }

    /**
     * Check if price has dropped since adding to wishlist
     * @param wishlistItem the wishlist item
     * @return true if price has dropped
     */
    @Named("checkPriceDrop")
    default boolean checkPriceDrop(DanhSachYeuThich wishlistItem) {
        if (wishlistItem.getSanPham() == null) {
            return false;
        }

        BigDecimal currentPrice = calculateCurrentMinPrice(wishlistItem.getSanPham());
        BigDecimal originalPrice = getOriginalPrice(wishlistItem);

        // Only consider it a price drop if current price is significantly lower
        // and both prices are valid
        return currentPrice.compareTo(BigDecimal.ZERO) > 0 &&
               originalPrice.compareTo(BigDecimal.ZERO) > 0 &&
               currentPrice.compareTo(originalPrice) < 0;
    }

    /**
     * Calculate price drop percentage
     * @param wishlistItem the wishlist item
     * @return price drop percentage
     */
    @Named("calculatePriceDropPercentage")
    default BigDecimal calculatePriceDropPercentage(DanhSachYeuThich wishlistItem) {
        if (wishlistItem.getSanPham() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentPrice = calculateCurrentMinPrice(wishlistItem.getSanPham());
        BigDecimal originalPrice = getOriginalPrice(wishlistItem);

        // Validate prices and ensure there's actually a drop
        if (originalPrice.compareTo(BigDecimal.ZERO) == 0 ||
            currentPrice.compareTo(BigDecimal.ZERO) == 0 ||
            currentPrice.compareTo(originalPrice) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = originalPrice.subtract(currentPrice);
        return difference.divide(originalPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Count available variants for a product
     * @param sanPham the product
     * @return count of available variants
     */
    @Named("countAvailableVariants")
    default long countAvailableVariants(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return 0;
        }

        return sanPham.getSanPhamChiTiets().stream()
                .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
                .count();
    }

    /**
     * Check if product has active discount campaigns
     * @param sanPham the product
     * @return true if has active discounts
     */
    @Named("checkActiveDiscount")
    default boolean checkActiveDiscount(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return false;
        }

        return sanPham.getSanPhamChiTiets().stream()
                .anyMatch(variant -> variant.getGiaKhuyenMai() != null &&
                                   variant.getGiaKhuyenMai().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Get availability status as human-readable string
     * @param wishlistItem the wishlist item
     * @return availability status
     */
    @Named("getAvailabilityStatus")
    default String getAvailabilityStatus(DanhSachYeuThich wishlistItem) {
        if (!wishlistItem.isProductAvailable()) {
            return "Không có sẵn";
        }

        long availableCount = countAvailableVariants(wishlistItem.getSanPham());
        if (availableCount == 0) {
            return "Hết hàng";
        } else if (availableCount == 1) {
            return "Còn 1 phiên bản";
        } else {
            return "Còn " + availableCount + " phiên bản";
        }
    }

    /**
     * Get available variants for the product
     * @param sanPham the product
     * @return list of available variant summaries
     */
    @Named("getAvailableVariants")
    default List<SanPhamChiTietSummaryDto> getAvailableVariants(SanPham sanPham) {
        if (sanPham == null || sanPham.getSanPhamChiTiets() == null) {
            return List.of();
        }

        return sanPham.getSanPhamChiTiets().stream()
                .filter(variant -> variant.getTrangThai() == TrangThaiSanPham.AVAILABLE)
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Map SanPhamChiTiet to SanPhamChiTietSummaryDto
     * @param variant the product variant
     * @return summary DTO with computed business logic fields
     */
    default SanPhamChiTietSummaryDto mapToSummaryDto(SanPhamChiTiet variant) {
        SanPhamChiTietSummaryDto dto = SanPhamChiTietSummaryDto.builder()
                .id(variant.getId())
                .serialNumber(variant.getSerialNumber())
                .mauSac(variant.getMauSac())
                .giaBan(variant.getGiaBan())
                .giaKhuyenMai(variant.getGiaKhuyenMai())
                .hinhAnh(variant.getHinhAnh() != null && !variant.getHinhAnh().isEmpty()
                        ? variant.getHinhAnh().get(0) : null)
                .trangThai(variant.getTrangThai())
                .build();

        // Business logic is computed by the DTO methods
        return dto;
    }
}
