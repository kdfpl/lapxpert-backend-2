package com.lapxpert.backend.dotgiamgia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * DTO for DotGiamGiaAuditHistory entity.
 * Represents a single audit trail entry for discount campaign changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotGiamGiaAuditHistoryDto implements Serializable {

    private Long id;

    @NotNull(message = "ID đợt giảm giá không được để trống")
    private Long dotGiamGiaId;

    @NotBlank(message = "Hành động không được để trống")
    @Size(max = 50, message = "Hành động không được vượt quá 50 ký tự")
    private String hanhDong;

    @NotNull(message = "Thời gian thay đổi không được để trống")
    private Instant thoiGianThayDoi;

    @Size(max = 100, message = "Người thực hiện không được vượt quá 100 ký tự")
    private String nguoiThucHien;

    @Size(max = 500, message = "Lý do thay đổi không được vượt quá 500 ký tự")
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
