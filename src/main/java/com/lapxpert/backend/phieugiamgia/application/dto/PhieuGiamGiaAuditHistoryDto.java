package com.lapxpert.backend.phieugiamgia.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for PhieuGiamGiaAuditHistory entity.
 * Represents a single audit trail entry for voucher changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhieuGiamGiaAuditHistoryDto implements Serializable {

    private Long id;
    private Long phieuGiamGiaId;
    private String hanhDong;
    private Instant thoiGianThayDoi;
    private String nguoiThucHien;
    private String lyDoThayDoi;
    private String giaTriCu;
    private String giaTriMoi;

    // Computed fields for frontend display
    private String hanhDongDisplay;
    private String thoiGianThayDoiVietnam;
    private List<ChangeDetail> chiTietThayDoi;

    /**
     * Represents a specific field change within an audit entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeDetail implements Serializable {
        private String fieldName;
        private String fieldDisplayName;
        private String oldValue;
        private String newValue;
        private String oldValueDisplay;
        private String newValueDisplay;
    }

    /**
     * Timeline entry for frontend display
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry implements Serializable {
        private String action;
        private String actionDisplay;
        private Instant timestamp;
        private String timestampDisplay;
        private String user;
        private String reason;
        private List<ChangeDetail> changes;
        private String icon;
        private String severity; // success, info, warn, error
    }
}
