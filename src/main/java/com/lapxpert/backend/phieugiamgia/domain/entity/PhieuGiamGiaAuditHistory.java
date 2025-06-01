package com.lapxpert.backend.phieugiamgia.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entity to track complete audit history for PhieuGiamGia changes.
 * Stores every change made to vouchers for complete timeline tracking.
 * Each record represents one change event with before/after values.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "phieu_giam_gia_audit_history", indexes = {
    @Index(name = "idx_audit_phieu_id", columnList = "phieu_giam_gia_id"),
    @Index(name = "idx_phieu_giam_gia_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_phieu_giam_gia_audit_action", columnList = "hanh_dong")
})
public class PhieuGiamGiaAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Reference to the voucher being audited
     */
    @Column(name = "phieu_giam_gia_id", nullable = false)
    private Long phieuGiamGiaId;

    /**
     * Type of action performed (CREATE, UPDATE, DELETE, STATUS_CHANGE, etc.)
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
     * Create audit history entry for voucher creation
     */
    public static PhieuGiamGiaAuditHistory createEntry(Long phieuGiamGiaId, String newValues,
                                                      String nguoiThucHien, String lyDo) {
        return PhieuGiamGiaAuditHistory.builder()
                .phieuGiamGiaId(phieuGiamGiaId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for voucher update
     */
    public static PhieuGiamGiaAuditHistory updateEntry(Long phieuGiamGiaId, String oldValues, String newValues,
                                                      String nguoiThucHien, String lyDo) {
        return PhieuGiamGiaAuditHistory.builder()
                .phieuGiamGiaId(phieuGiamGiaId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for voucher deletion/closure
     */
    public static PhieuGiamGiaAuditHistory deleteEntry(Long phieuGiamGiaId, String oldValues,
                                                      String nguoiThucHien, String lyDo) {
        return PhieuGiamGiaAuditHistory.builder()
                .phieuGiamGiaId(phieuGiamGiaId)
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
    public static PhieuGiamGiaAuditHistory statusChangeEntry(Long phieuGiamGiaId, String oldStatus, String newStatus,
                                                            String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThai\":\"%s\"}", oldStatus);
        String newValues = String.format("{\"trangThai\":\"%s\"}", newStatus);

        return PhieuGiamGiaAuditHistory.builder()
                .phieuGiamGiaId(phieuGiamGiaId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái tự động")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }
}
