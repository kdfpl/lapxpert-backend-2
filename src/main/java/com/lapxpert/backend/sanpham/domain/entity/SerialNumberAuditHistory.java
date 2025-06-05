package com.lapxpert.backend.sanpham.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entity to track complete audit history for SerialNumber changes.
 * Stores every change made to serial numbers for complete timeline tracking.
 * Each record represents one change event with before/after values.
 * Provides complete traceability for individual laptop units.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "serial_number_audit_history", indexes = {
    @Index(name = "idx_audit_serial_number_id", columnList = "serial_number_id"),
    @Index(name = "idx_serial_number_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_serial_number_audit_action", columnList = "hanh_dong"),
    @Index(name = "idx_serial_number_audit_user", columnList = "nguoi_thuc_hien"),
    @Index(name = "idx_serial_number_audit_batch", columnList = "batch_operation_id")
})
public class SerialNumberAuditHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "serial_number_audit_history_id_gen")
    @SequenceGenerator(name = "serial_number_audit_history_id_gen", sequenceName = "serial_number_audit_history_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * ID of the serial number that was changed
     */
    @Column(name = "serial_number_id", nullable = false)
    private Long serialNumberId;

    /**
     * Type of action performed (CREATE, UPDATE, DELETE, STATUS_CHANGE, RESERVE, RELEASE, etc.)
     */
    @Column(name = "hanh_dong", nullable = false, length = 50)
    private String hanhDong;

    /**
     * Timestamp when the change occurred
     */
    @Column(name = "thoi_gian_thay_doi", nullable = false)
    private Instant thoiGianThayDoi;

    /**
     * User who performed the action
     */
    @Column(name = "nguoi_thuc_hien", length = 100)
    private String nguoiThucHien;

    /**
     * Reason for the change
     */
    @Column(name = "ly_do_thay_doi", length = 500)
    private String lyDoThayDoi;

    /**
     * Previous values in JSON format
     */
    @Column(name = "gia_tri_cu", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String giaTriCu;

    /**
     * New values in JSON format
     */
    @Column(name = "gia_tri_moi", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String giaTriMoi;

    /**
     * IP address of the user who made the change
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent/browser information
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Batch operation ID for bulk operations
     */
    @Column(name = "batch_operation_id", length = 50)
    private String batchOperationId;

    /**
     * Order ID if this change is related to an order
     */
    @Column(name = "order_id", length = 50)
    private String orderId;

    /**
     * Channel where the change occurred (POS, ONLINE, ADMIN, etc.)
     */
    @Column(name = "channel", length = 20)
    private String channel;

    /**
     * Additional metadata in JSON format
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    // Static factory methods for common audit entries

    /**
     * Create audit entry for serial number creation
     */
    public static SerialNumberAuditHistory createEntry(Long serialNumberId, String newValues, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit entry for serial number update
     */
    public static SerialNumberAuditHistory updateEntry(Long serialNumberId, String oldValues, String newValues, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit entry for status change
     */
    public static SerialNumberAuditHistory statusChangeEntry(Long serialNumberId, String oldStatus, String newStatus, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .giaTriCu("{\"trangThai\":\"" + oldStatus + "\"}")
                .giaTriMoi("{\"trangThai\":\"" + newStatus + "\"}")
                .build();
    }

    /**
     * Create audit entry for reservation
     */
    public static SerialNumberAuditHistory reservationEntry(Long serialNumberId, String channel, String orderId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("RESERVE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .channel(channel)
                .orderId(orderId)
                .giaTriMoi("{\"trangThai\":\"RESERVED\",\"channel\":\"" + channel + "\",\"orderId\":\"" + orderId + "\"}")
                .build();
    }

    /**
     * Create audit entry for reservation release
     */
    public static SerialNumberAuditHistory releaseEntry(Long serialNumberId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("RELEASE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .giaTriMoi("{\"trangThai\":\"AVAILABLE\"}")
                .build();
    }

    /**
     * Create audit entry for sale confirmation
     */
    public static SerialNumberAuditHistory saleEntry(Long serialNumberId, String orderId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("SELL")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .orderId(orderId)
                .giaTriMoi("{\"trangThai\":\"SOLD\",\"orderId\":\"" + orderId + "\"}")
                .build();
    }

    /**
     * Create audit entry for return
     */
    public static SerialNumberAuditHistory returnEntry(Long serialNumberId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("RETURN")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .giaTriMoi("{\"trangThai\":\"RETURNED\"}")
                .build();
    }

    /**
     * Create audit entry for bulk operations
     */
    public static SerialNumberAuditHistory bulkOperationEntry(Long serialNumberId, String action, String batchId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong(action)
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .batchOperationId(batchId)
                .build();
    }

    /**
     * Create audit entry for import operations
     */
    public static SerialNumberAuditHistory importEntry(Long serialNumberId, String importBatchId, String user, String reason) {
        return SerialNumberAuditHistory.builder()
                .serialNumberId(serialNumberId)
                .hanhDong("IMPORT")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(user)
                .lyDoThayDoi(reason)
                .batchOperationId(importBatchId)
                .giaTriMoi("{\"action\":\"IMPORTED\",\"batchId\":\"" + importBatchId + "\"}")
                .build();
    }

    /**
     * Add IP address and user agent information
     */
    public SerialNumberAuditHistory withClientInfo(String ipAddress, String userAgent) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Add metadata information
     */
    public SerialNumberAuditHistory withMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Add channel information
     */
    public SerialNumberAuditHistory withChannel(String channel) {
        this.channel = channel;
        return this;
    }
}
