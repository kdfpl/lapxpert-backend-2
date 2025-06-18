package com.lapxpert.backend.nguoidung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entity to track complete audit history for NguoiDung changes.
 * Stores every change made to users for complete timeline tracking.
 * Each record represents one change event with before/after values.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "nguoi_dung_audit_history", indexes = {
    @Index(name = "idx_audit_nguoi_dung_id", columnList = "nguoi_dung_id"),
    @Index(name = "idx_nguoi_dung_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_nguoi_dung_audit_action", columnList = "hanh_dong")
})
public class NguoiDungAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Reference to the user being audited
     */
    @NotNull(message = "ID người dùng không được để trống")
    @Column(name = "nguoi_dung_id", nullable = false)
    private Long nguoiDungId;

    /**
     * Type of action performed (CREATE, UPDATE, DELETE, STATUS_CHANGE, etc.)
     */
    @NotBlank(message = "Hành động không được để trống")
    @Size(max = 50, message = "Hành động không được vượt quá 50 ký tự")
    @Column(name = "hanh_dong", nullable = false, length = 50)
    private String hanhDong;

    /**
     * Timestamp when the change occurred
     */
    @NotNull(message = "Thời gian thay đổi không được để trống")
    @Column(name = "thoi_gian_thay_doi", nullable = false)
    private Instant thoiGianThayDoi;

    /**
     * User who performed the action
     */
    @Size(max = 100, message = "Người thực hiện không được vượt quá 100 ký tự")
    @Column(name = "nguoi_thuc_hien", length = 100)
    private String nguoiThucHien;

    /**
     * Reason for the change
     */
    @Size(max = 500, message = "Lý do thay đổi không được vượt quá 500 ký tự")
    @Column(name = "ly_do_thay_doi", length = 500)
    private String lyDoThayDoi;

    /**
     * Previous values in JSON format (null for CREATE actions)
     */
    @Column(name = "gia_tri_cu", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String giaTriCu;

    /**
     * New values in JSON format (null for DELETE actions)
     */
    @Column(name = "gia_tri_moi", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String giaTriMoi;

    /**
     * Create audit history entry for user creation
     */
    public static NguoiDungAuditHistory createEntry(Long nguoiDungId, String newValues, 
                                                   String nguoiThucHien, String lyDo) {
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(nguoiDungId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for user update
     */
    public static NguoiDungAuditHistory updateEntry(Long nguoiDungId, String oldValues, String newValues,
                                                   String nguoiThucHien, String lyDo) {
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(nguoiDungId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for user deletion/deactivation
     */
    public static NguoiDungAuditHistory deleteEntry(Long nguoiDungId, String oldValues,
                                                   String nguoiThucHien, String lyDo) {
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(nguoiDungId)
                .hanhDong("DELETE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(null)
                .build();
    }

    /**
     * Create audit history entry for status changes
     */
    public static NguoiDungAuditHistory statusChangeEntry(Long nguoiDungId, String oldStatus, String newStatus,
                                                         String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThai\":\"%s\"}", oldStatus);
        String newValues = String.format("{\"trangThai\":\"%s\"}", newStatus);
        
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(nguoiDungId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái tự động")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for role changes
     */
    public static NguoiDungAuditHistory roleChangeEntry(Long nguoiDungId, String oldRole, String newRole,
                                                       String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"vaiTro\":\"%s\"}", oldRole);
        String newValues = String.format("{\"vaiTro\":\"%s\"}", newRole);
        
        return NguoiDungAuditHistory.builder()
                .nguoiDungId(nguoiDungId)
                .hanhDong("ROLE_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi vai trò")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }
}
