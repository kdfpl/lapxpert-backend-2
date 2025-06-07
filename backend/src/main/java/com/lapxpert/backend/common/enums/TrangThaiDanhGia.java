package com.lapxpert.backend.common.enums;

/**
 * Enum for review status in the review system
 */
public enum TrangThaiDanhGia {
    CHO_DUYET("Chờ duyệt"),
    DA_DUYET("Đã duyệt"),
    BI_TU_CHOI("Bị từ chối"),
    DA_AN("Đã ẩn");

    private final String description;

    TrangThaiDanhGia(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if review is visible to public
     * @return true if review can be displayed
     */
    public boolean isVisible() {
        return this == DA_DUYET;
    }

    /**
     * Check if review is pending approval
     * @return true if review needs moderation
     */
    public boolean isPending() {
        return this == CHO_DUYET;
    }

    /**
     * Check if review was rejected or hidden
     * @return true if review is not visible
     */
    public boolean isHidden() {
        return this == BI_TU_CHOI || this == DA_AN;
    }
}
