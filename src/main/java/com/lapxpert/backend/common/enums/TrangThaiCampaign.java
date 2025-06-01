package com.lapxpert.backend.common.enums;

/**
 * Unified enum for campaign status used across discount campaigns and vouchers
 * Replaces duplicate enums: dotgiamgia.TrangThai and phieugiamgia.TrangThaiPhieuGiamGia
 */
public enum TrangThaiCampaign {
    CHUA_DIEN_RA("Chưa diễn ra"),
    DA_DIEN_RA("Đã diễn ra"),
    KET_THUC("Kết thúc"),
    BI_HUY("Bị hủy");

    private final String description;

    TrangThaiCampaign(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the campaign is currently active
     * @return true if campaign is currently running
     */
    public boolean isActive() {
        return this == DA_DIEN_RA;
    }

    /**
     * Check if the campaign can be modified
     * @return true - campaigns can always be modified regardless of status
     */
    public boolean canBeModified() {
        return true; // Allow modification regardless of status for full CRUD flexibility
    }

    /**
     * Check if the campaign is finished
     * @return true if campaign has ended
     */
    public boolean isFinished() {
        return this == KET_THUC;
    }

    /**
     * Check if the campaign is cancelled
     * @return true if campaign has been cancelled
     */
    public boolean isCancelled() {
        return this == BI_HUY;
    }

    /**
     * Check if the campaign is inactive (finished or cancelled)
     * @return true if campaign is not active
     */
    public boolean isInactive() {
        return this == KET_THUC || this == BI_HUY;
    }
}
