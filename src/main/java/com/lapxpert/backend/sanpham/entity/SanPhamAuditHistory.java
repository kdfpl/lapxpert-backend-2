package com.lapxpert.backend.sanpham.entity;

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
 * Entity to track complete audit history for SanPham changes.
 * Stores every change made to products for complete timeline tracking.
 * Each record represents one change event with before/after values.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "san_pham_audit_history", indexes = {
    @Index(name = "idx_audit_san_pham_id", columnList = "san_pham_id"),
    @Index(name = "idx_san_pham_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_san_pham_audit_action", columnList = "hanh_dong")
})
public class SanPhamAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Reference to the product being audited
     */
    @NotNull(message = "ID sản phẩm không được để trống")
    @Column(name = "san_pham_id", nullable = false)
    private Long sanPhamId;

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
     * Create audit history entry for product creation
     */
    public static SanPhamAuditHistory createEntry(Long sanPhamId, String newValues, 
                                                String nguoiThucHien, String lyDo) {
        return SanPhamAuditHistory.builder()
                .sanPhamId(sanPhamId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for product update
     */
    public static SanPhamAuditHistory updateEntry(Long sanPhamId, String oldValues, String newValues,
                                                String nguoiThucHien, String lyDo) {
        return SanPhamAuditHistory.builder()
                .sanPhamId(sanPhamId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for product deletion/deactivation
     */
    public static SanPhamAuditHistory deleteEntry(Long sanPhamId, String oldValues,
                                                String nguoiThucHien, String lyDo) {
        return SanPhamAuditHistory.builder()
                .sanPhamId(sanPhamId)
                .hanhDong("DELETE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(null)
                .build();
    }

    /**
     * Create audit history entry for product status changes
     */
    public static SanPhamAuditHistory statusChangeEntry(Long sanPhamId, String oldStatus, String newStatus,
                                                       String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThai\":\"%s\"}", oldStatus);
        String newValues = String.format("{\"trangThai\":\"%s\"}", newStatus);
        
        return SanPhamAuditHistory.builder()
                .sanPhamId(sanPhamId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái sản phẩm")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for category changes
     */
    public static SanPhamAuditHistory categoryChangeEntry(Long sanPhamId, String oldCategories, String newCategories,
                                                         String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"danhMucs\":%s}", oldCategories);
        String newValues = String.format("{\"danhMucs\":%s}", newCategories);
        
        return SanPhamAuditHistory.builder()
                .sanPhamId(sanPhamId)
                .hanhDong("CATEGORY_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi danh mục sản phẩm")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }
}
