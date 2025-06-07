package com.lapxpert.backend.sanpham.application.mapper;

import com.lapxpert.backend.sanpham.application.dto.SerialNumberAuditHistoryDto;
import com.lapxpert.backend.sanpham.domain.entity.SerialNumberAuditHistory;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for SerialNumberAuditHistory entity and DTO conversion.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SerialNumberAuditHistoryMapper {

    @Named("toDto")
    @Mapping(source = ".", target = "hanhDongDisplay", qualifiedByName = "mapActionDisplay")
    @Mapping(source = ".", target = "thoiGianThayDoiVietnam", qualifiedByName = "mapTimestampVietnam")
    @Mapping(source = ".", target = "chiTietThayDoi", qualifiedByName = "mapChangeDetails")
    @Mapping(source = ".", target = "actionIcon", qualifiedByName = "mapActionIcon")
    @Mapping(source = ".", target = "actionColor", qualifiedByName = "mapActionColor")
    SerialNumberAuditHistoryDto toDto(SerialNumberAuditHistory auditHistory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "thoiGianThayDoi", ignore = true)
    SerialNumberAuditHistory toEntity(SerialNumberAuditHistoryDto auditHistoryDto);

    @IterableMapping(qualifiedByName = "toDto")
    List<SerialNumberAuditHistoryDto> toDtoList(List<SerialNumberAuditHistory> auditHistories);

    List<SerialNumberAuditHistory> toEntityList(List<SerialNumberAuditHistoryDto> auditHistoryDtos);

    // Custom mapping methods

    @Named("mapActionDisplay")
    default String mapActionDisplay(SerialNumberAuditHistory auditHistory) {
        if (auditHistory.getHanhDong() == null) return "";
        
        switch (auditHistory.getHanhDong()) {
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
                return auditHistory.getHanhDong();
        }
    }

    @Named("mapTimestampVietnam")
    default String mapTimestampVietnam(SerialNumberAuditHistory auditHistory) {
        if (auditHistory.getThoiGianThayDoi() == null) return "";
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("HH:mm:ss dd/MM/yyyy")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        return formatter.format(auditHistory.getThoiGianThayDoi());
    }

    @Named("mapChangeDetails")
    default String mapChangeDetails(SerialNumberAuditHistory auditHistory) {
        String oldValue = auditHistory.getGiaTriCu();
        String newValue = auditHistory.getGiaTriMoi();
        
        if (oldValue == null && newValue == null) {
            return "";
        }
        
        StringBuilder details = new StringBuilder();
        
        if (oldValue != null && !oldValue.trim().isEmpty()) {
            details.append("Trước: ").append(formatJsonForDisplay(oldValue));
        }
        
        if (newValue != null && !newValue.trim().isEmpty()) {
            if (details.length() > 0) {
                details.append(" | ");
            }
            details.append("Sau: ").append(formatJsonForDisplay(newValue));
        }
        
        return details.toString();
    }

    @Named("mapActionIcon")
    default String mapActionIcon(SerialNumberAuditHistory auditHistory) {
        if (auditHistory.getHanhDong() == null) return "pi pi-circle";
        
        switch (auditHistory.getHanhDong()) {
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

    @Named("mapActionColor")
    default String mapActionColor(SerialNumberAuditHistory auditHistory) {
        if (auditHistory.getHanhDong() == null) return "text-gray-500";
        
        switch (auditHistory.getHanhDong()) {
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

    // Helper method for JSON formatting
    default String formatJsonForDisplay(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "";
        }
        
        // Simple JSON formatting for display
        return json.replace("{", "").replace("}", "").replace("\"", "").replace(",", ", ");
    }

    // Specialized mapping methods for different use cases

    /**
     * Map for timeline display (minimal information)
     */
    @Named("toTimelineDto")
    @Mapping(source = ".", target = "hanhDongDisplay", qualifiedByName = "mapActionDisplay")
    @Mapping(source = ".", target = "thoiGianThayDoiVietnam", qualifiedByName = "mapTimestampVietnam")
    @Mapping(source = ".", target = "actionIcon", qualifiedByName = "mapActionIcon")
    @Mapping(source = ".", target = "actionColor", qualifiedByName = "mapActionColor")
    @Mapping(target = "chiTietThayDoi", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "userAgent", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    SerialNumberAuditHistoryDto toTimelineDto(SerialNumberAuditHistory auditHistory);

    /**
     * Map for detailed audit view (full information)
     */
    @Named("toDetailedDto")
    @Mapping(source = ".", target = "hanhDongDisplay", qualifiedByName = "mapActionDisplay")
    @Mapping(source = ".", target = "thoiGianThayDoiVietnam", qualifiedByName = "mapTimestampVietnam")
    @Mapping(source = ".", target = "chiTietThayDoi", qualifiedByName = "mapChangeDetails")
    @Mapping(source = ".", target = "actionIcon", qualifiedByName = "mapActionIcon")
    @Mapping(source = ".", target = "actionColor", qualifiedByName = "mapActionColor")
    SerialNumberAuditHistoryDto toDetailedDto(SerialNumberAuditHistory auditHistory);

    /**
     * Map for summary/report view (basic information)
     */
    @Named("toSummaryDto")
    @Mapping(source = ".", target = "hanhDongDisplay", qualifiedByName = "mapActionDisplay")
    @Mapping(source = ".", target = "thoiGianThayDoiVietnam", qualifiedByName = "mapTimestampVietnam")
    @Mapping(target = "chiTietThayDoi", ignore = true)
    @Mapping(target = "actionIcon", ignore = true)
    @Mapping(target = "actionColor", ignore = true)
    @Mapping(target = "giaTriCu", ignore = true)
    @Mapping(target = "giaTriMoi", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "userAgent", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    SerialNumberAuditHistoryDto toSummaryDto(SerialNumberAuditHistory auditHistory);

    // List mapping methods for specialized use cases

    @IterableMapping(qualifiedByName = "toTimelineDto")
    List<SerialNumberAuditHistoryDto> toTimelineDtoList(List<SerialNumberAuditHistory> auditHistories);

    @IterableMapping(qualifiedByName = "toDetailedDto")
    List<SerialNumberAuditHistoryDto> toDetailedDtoList(List<SerialNumberAuditHistory> auditHistories);

    @IterableMapping(qualifiedByName = "toSummaryDto")
    List<SerialNumberAuditHistoryDto> toSummaryDtoList(List<SerialNumberAuditHistory> auditHistories);

    // Timeline entry mapping
    @Mapping(source = "hanhDongDisplay", target = "title")
    @Mapping(source = "serialNumberValue", target = "subtitle")
    @Mapping(source = "thoiGianThayDoiVietnam", target = "timestamp")
    @Mapping(source = "actionIcon", target = "icon")
    @Mapping(source = "actionColor", target = "color")
    @Mapping(source = "chiTietThayDoi", target = "details")
    @Mapping(source = "nguoiThucHien", target = "user")
    @Mapping(source = "lyDoThayDoi", target = "reason")
    @Mapping(source = "channel", target = "channel")
    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "batchOperationId", target = "batchId")
    SerialNumberAuditHistoryDto.TimelineEntry toTimelineEntry(SerialNumberAuditHistoryDto auditHistoryDto);

    List<SerialNumberAuditHistoryDto.TimelineEntry> toTimelineEntryList(List<SerialNumberAuditHistoryDto> auditHistoryDtos);
}
