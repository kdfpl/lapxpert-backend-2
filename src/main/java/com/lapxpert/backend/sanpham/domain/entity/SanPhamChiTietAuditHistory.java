package com.lapxpert.backend.sanpham.domain.entity;

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
 * Entity to track complete audit history for SanPhamChiTiet changes.
 * Stores every change made to product variants for complete timeline tracking.
 * Each record represents one change event with before/after values.
 * Separate from SanPham audit as variants have independent business logic.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "san_pham_chi_tiet_audit_history", indexes = {
    @Index(name = "idx_audit_san_pham_chi_tiet_id", columnList = "san_pham_chi_tiet_id"),
    @Index(name = "idx_san_pham_chi_tiet_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_san_pham_chi_tiet_audit_action", columnList = "hanh_dong")
})
public class SanPhamChiTietAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Reference to the product variant being audited
     */
    @NotNull(message = "ID sản phẩm chi tiết không được để trống")
    @Column(name = "san_pham_chi_tiet_id", nullable = false)
    private Long sanPhamChiTietId;

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
     * Create audit history entry for product variant creation
     */
    public static SanPhamChiTietAuditHistory createEntry(Long sanPhamChiTietId, String newValues, 
                                                        String nguoiThucHien, String lyDo) {
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for product variant update
     */
    public static SanPhamChiTietAuditHistory updateEntry(Long sanPhamChiTietId, String oldValues, String newValues,
                                                        String nguoiThucHien, String lyDo) {
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for product variant deletion/deactivation
     */
    public static SanPhamChiTietAuditHistory deleteEntry(Long sanPhamChiTietId, String oldValues,
                                                        String nguoiThucHien, String lyDo) {
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("DELETE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(null)
                .build();
    }

    /**
     * Create audit history entry for inventory status changes
     */
    public static SanPhamChiTietAuditHistory statusChangeEntry(Long sanPhamChiTietId, String oldStatus, String newStatus,
                                                              String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThai\":\"%s\"}", oldStatus);
        String newValues = String.format("{\"trangThai\":\"%s\"}", newStatus);
        
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái sản phẩm chi tiết")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for price changes
     */
    public static SanPhamChiTietAuditHistory priceChangeEntry(Long sanPhamChiTietId, String oldPrice, String newPrice,
                                                             String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"giaBan\":\"%s\"}", oldPrice);
        String newValues = String.format("{\"giaBan\":\"%s\"}", newPrice);
        
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("PRICE_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi giá bán")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for discount campaign assignments
     */
    public static SanPhamChiTietAuditHistory discountAssignmentEntry(Long sanPhamChiTietId, String oldDiscounts, String newDiscounts,
                                                                    String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"dotGiamGias\":%s}", oldDiscounts);
        String newValues = String.format("{\"dotGiamGias\":%s}", newDiscounts);
        
        return SanPhamChiTietAuditHistory.builder()
                .sanPhamChiTietId(sanPhamChiTietId)
                .hanhDong("DISCOUNT_ASSIGNMENT")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi đợt giảm giá")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }
}
