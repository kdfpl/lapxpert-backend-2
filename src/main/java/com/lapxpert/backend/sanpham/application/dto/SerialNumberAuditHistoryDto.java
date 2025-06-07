package com.lapxpert.backend.sanpham.application.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * DTO for {@link com.lapxpert.backend.sanpham.domain.entity.SerialNumberAuditHistory}
 */
@Data
public class SerialNumberAuditHistoryDto implements Serializable {
    private Long id;
    private Long serialNumberId;
    private String serialNumberValue; // For display purposes
    private String hanhDong;
    private Instant thoiGianThayDoi;
    private String nguoiThucHien;
    private String lyDoThayDoi;
    private String giaTriCu;
    private String giaTriMoi;
    private String ipAddress;
    private String userAgent;
    private String batchOperationId;
    private String orderId;
    private String channel;
    private String metadata;

    // Computed fields for UI display
    private String hanhDongDisplay;
    private String thoiGianThayDoiVietnam;
    private String chiTietThayDoi;
    private String actionIcon;
    private String actionColor;

    // Helper methods for UI

    public String getHanhDongDisplay() {
        if (hanhDong == null) return "";
        
        switch (hanhDong) {
            case "CREATE":
                return "Tạo mới";
            case "UPDATE":
                return "Cập nhật";
            case "DELETE":
                return "Xóa";
            case "STATUS_CHANGE":
                return "Thay đổi trạng thái";
            case "RESERVE":
                return "Đặt trước";
            case "RELEASE":
                return "Hủy đặt trước";
            case "SELL":
                return "Bán hàng";
            case "RETURN":
                return "Trả hàng";
            case "IMPORT":
                return "Nhập kho";
            case "GENERATE":
                return "Tạo hàng loạt";
            default:
                return hanhDong;
        }
    }

    public String getThoiGianThayDoiVietnam() {
        if (thoiGianThayDoi == null) return "";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        return formatter.format(thoiGianThayDoi);
    }

    public String getChiTietThayDoi() {
        if (giaTriCu == null && giaTriMoi == null) {
            return "";
        }
        
        StringBuilder details = new StringBuilder();
        
        if (giaTriCu != null && !giaTriCu.trim().isEmpty()) {
            details.append("Trước: ").append(formatJsonForDisplay(giaTriCu));
        }
        
        if (giaTriMoi != null && !giaTriMoi.trim().isEmpty()) {
            if (details.length() > 0) {
                details.append(" | ");
            }
            details.append("Sau: ").append(formatJsonForDisplay(giaTriMoi));
        }
        
        return details.toString();
    }

    public String getActionIcon() {
        if (hanhDong == null) return "pi pi-circle";
        
        switch (hanhDong) {
            case "CREATE":
            case "GENERATE":
            case "IMPORT":
                return "pi pi-plus-circle";
            case "UPDATE":
                return "pi pi-pencil";
            case "DELETE":
                return "pi pi-trash";
            case "STATUS_CHANGE":
                return "pi pi-refresh";
            case "RESERVE":
                return "pi pi-lock";
            case "RELEASE":
                return "pi pi-unlock";
            case "SELL":
                return "pi pi-shopping-cart";
            case "RETURN":
                return "pi pi-undo";
            default:
                return "pi pi-circle";
        }
    }

    public String getActionColor() {
        if (hanhDong == null) return "text-gray-500";
        
        switch (hanhDong) {
            case "CREATE":
            case "GENERATE":
            case "IMPORT":
                return "text-green-600";
            case "UPDATE":
                return "text-blue-600";
            case "DELETE":
                return "text-red-600";
            case "STATUS_CHANGE":
                return "text-orange-600";
            case "RESERVE":
                return "text-purple-600";
            case "RELEASE":
                return "text-gray-600";
            case "SELL":
                return "text-green-700";
            case "RETURN":
                return "text-yellow-600";
            default:
                return "text-gray-500";
        }
    }

    private String formatJsonForDisplay(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }
        
        // Simple JSON formatting for display
        // In a real implementation, you might want to use a JSON library
        return json.replace("{", "").replace("}", "").replace("\"", "").replace(",", ", ");
    }

    /**
     * Timeline entry for UI display
     */
    @Data
    public static class TimelineEntry implements Serializable {
        private String title;
        private String subtitle;
        private String timestamp;
        private String icon;
        private String color;
        private String details;
        private String user;
        private String reason;
        private String channel;
        private String orderId;
        private String batchId;

        public static TimelineEntry fromAuditHistory(SerialNumberAuditHistoryDto audit) {
            TimelineEntry entry = new TimelineEntry();
            entry.setTitle(audit.getHanhDongDisplay());
            entry.setSubtitle(audit.getSerialNumberValue());
            entry.setTimestamp(audit.getThoiGianThayDoiVietnam());
            entry.setIcon(audit.getActionIcon());
            entry.setColor(audit.getActionColor());
            entry.setDetails(audit.getChiTietThayDoi());
            entry.setUser(audit.getNguoiThucHien());
            entry.setReason(audit.getLyDoThayDoi());
            entry.setChannel(audit.getChannel());
            entry.setOrderId(audit.getOrderId());
            entry.setBatchId(audit.getBatchOperationId());
            return entry;
        }
    }

    /**
     * Summary for dashboard display
     */
    @Data
    public static class AuditSummary implements Serializable {
        private long totalActions;
        private long affectedSerials;
        private long activeUsers;
        private long actionTypes;
        private String period;

        public static AuditSummary create(long totalActions, long affectedSerials, long activeUsers, long actionTypes, String period) {
            AuditSummary summary = new AuditSummary();
            summary.setTotalActions(totalActions);
            summary.setAffectedSerials(affectedSerials);
            summary.setActiveUsers(activeUsers);
            summary.setActionTypes(actionTypes);
            summary.setPeriod(period);
            return summary;
        }
    }

    /**
     * Statistics for reporting
     */
    @Data
    public static class ActionStatistics implements Serializable {
        private String action;
        private String actionDisplay;
        private long count;
        private double percentage;

        public static ActionStatistics create(String action, long count, long total) {
            ActionStatistics stats = new ActionStatistics();
            stats.setAction(action);
            stats.setActionDisplay(getActionDisplay(action));
            stats.setCount(count);
            stats.setPercentage(total > 0 ? (double) count / total * 100 : 0);
            return stats;
        }

        private static String getActionDisplay(String action) {
            switch (action) {
                case "CREATE": return "Tạo mới";
                case "UPDATE": return "Cập nhật";
                case "DELETE": return "Xóa";
                case "STATUS_CHANGE": return "Thay đổi trạng thái";
                case "RESERVE": return "Đặt trước";
                case "RELEASE": return "Hủy đặt trước";
                case "SELL": return "Bán hàng";
                case "RETURN": return "Trả hàng";
                case "IMPORT": return "Nhập kho";
                case "GENERATE": return "Tạo hàng loạt";
                default: return action;
            }
        }
    }
}
