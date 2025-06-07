package com.lapxpert.backend.common.enums;

/**
 * Enum representing discount types for voucher campaigns.
 * Replaces the Boolean loaiPhieuGiamGia field with clear, type-safe values.
 *
 * Used in PhieuGiamGia entity to specify whether the discount is:
 * - PHAN_TRAM: Percentage-based discount (e.g., 10% off)
 * - SO_TIEN_CO_DINH: Fixed amount discount (e.g., 50,000 VND off)
 */
public enum LoaiGiamGia {
    /**
     * Percentage-based discount
     * Value should be between 0.01 and 100.00
     */
    PHAN_TRAM("Giảm theo phần trăm"),

    /**
     * Fixed amount discount
     * Value should be greater than 0
     */
    SO_TIEN_CO_DINH("Giảm số tiền cố định");

    private final String description;

    LoaiGiamGia(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a percentage discount
     * @return true if percentage discount
     */
    public boolean isPhanTram() {
        return this == PHAN_TRAM;
    }

    /**
     * Check if this is a fixed amount discount
     * @return true if fixed amount discount
     */
    public boolean isSoTienCoDinh() {
        return this == SO_TIEN_CO_DINH;
    }


}
