package com.lapxpert.backend.nguoidung.domain.entity;

/**
 * Enum representing user status states
 * Replaces the Boolean trangThai field for better type safety and business logic
 */
public enum TrangThaiNguoiDung {
    HOAT_DONG("Hoạt động"),
    KHONG_HOAT_DONG("Không hoạt động");

    private final String description;

    TrangThaiNguoiDung(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the status represents an active user
     * @return true if user is active
     */
    public boolean isActive() {
        return this == HOAT_DONG;
    }

    /**
     * Check if the status represents an inactive user
     * @return true if user is inactive
     */
    public boolean isInactive() {
        return this == KHONG_HOAT_DONG;
    }

    /**
     * Convert from Boolean to enum for backward compatibility during migration
     * @param active Boolean status (true = active, false = inactive)
     * @return corresponding enum value
     */
    public static TrangThaiNguoiDung fromBoolean(Boolean active) {
        if (active == null) {
            return KHONG_HOAT_DONG; // Default to inactive for null values
        }
        return active ? HOAT_DONG : KHONG_HOAT_DONG;
    }

    /**
     * Convert to Boolean for backward compatibility
     * @return true if active, false if inactive
     */
    public boolean toBoolean() {
        return this == HOAT_DONG;
    }
}
