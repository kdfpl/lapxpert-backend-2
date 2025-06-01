package com.lapxpert.backend.hoadon.domain.entity;

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
 * Entity to track complete audit history for HoaDon changes.
 * Stores every change made to orders for complete timeline tracking.
 * Each record represents one change event with before/after values.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "hoa_don_audit_history", indexes = {
    @Index(name = "idx_audit_hoa_don_id", columnList = "hoa_don_id"),
    @Index(name = "idx_hoa_don_audit_timestamp", columnList = "thoi_gian_thay_doi"),
    @Index(name = "idx_hoa_don_audit_action", columnList = "hanh_dong")
})
public class HoaDonAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Reference to the order being audited
     */
    @NotNull(message = "ID hóa đơn không được để trống")
    @Column(name = "hoa_don_id", nullable = false)
    private Long hoaDonId;

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
     * Create audit history entry for order creation
     */
    public static HoaDonAuditHistory createEntry(Long hoaDonId, String newValues, 
                                               String nguoiThucHien, String lyDo) {
        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("CREATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for order update
     */
    public static HoaDonAuditHistory updateEntry(Long hoaDonId, String oldValues, String newValues,
                                               String nguoiThucHien, String lyDo) {
        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("UPDATE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for order cancellation
     */
    public static HoaDonAuditHistory cancelEntry(Long hoaDonId, String oldValues,
                                               String nguoiThucHien, String lyDo) {
        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("CANCEL")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien)
                .lyDoThayDoi(lyDo)
                .giaTriCu(oldValues)
                .giaTriMoi(null)
                .build();
    }

    /**
     * Create audit history entry for order status changes
     */
    public static HoaDonAuditHistory statusChangeEntry(Long hoaDonId, String oldStatus, String newStatus,
                                                      String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThaiDonHang\":\"%s\"}", oldStatus);
        String newValues = String.format("{\"trangThaiDonHang\":\"%s\"}", newStatus);
        
        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái đơn hàng")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for payment status changes
     */
    public static HoaDonAuditHistory paymentStatusChangeEntry(Long hoaDonId, String oldPaymentStatus, String newPaymentStatus,
                                                            String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"trangThaiThanhToan\":\"%s\"}", oldPaymentStatus);
        String newValues = String.format("{\"trangThaiThanhToan\":\"%s\"}", newPaymentStatus);

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("PAYMENT_STATUS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi trạng thái thanh toán")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for customer changes
     */
    public static HoaDonAuditHistory customerChangeEntry(Long hoaDonId, Long oldCustomerId, Long newCustomerId,
                                                       String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"khachHangId\":\"%s\"}", oldCustomerId);
        String newValues = String.format("{\"khachHangId\":\"%s\"}", newCustomerId);

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("CUSTOMER_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi thông tin khách hàng")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for voucher applications
     */
    public static HoaDonAuditHistory voucherApplicationEntry(Long hoaDonId, String voucherCode, String discountAmount,
                                                           String nguoiThucHien, String lyDo) {
        String newValues = String.format("{\"voucherCode\":\"%s\",\"discountAmount\":\"%s\"}", voucherCode, discountAmount);

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("VOUCHER_APPLIED")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Áp dụng voucher giảm giá")
                .giaTriCu(null)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for voucher removal
     */
    public static HoaDonAuditHistory voucherRemovalEntry(Long hoaDonId, String voucherCode, String discountAmount,
                                                       String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"voucherCode\":\"%s\",\"discountAmount\":\"%s\"}", voucherCode, discountAmount);

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("VOUCHER_REMOVED")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Gỡ bỏ voucher giảm giá")
                .giaTriCu(oldValues)
                .giaTriMoi(null)
                .build();
    }

    /**
     * Create audit history entry for order item changes
     */
    public static HoaDonAuditHistory orderItemChangeEntry(Long hoaDonId, String itemChanges,
                                                         String nguoiThucHien, String lyDo) {
        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("ITEM_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi sản phẩm trong đơn hàng")
                .giaTriCu(null)
                .giaTriMoi(itemChanges)
                .build();
    }

    /**
     * Create audit history entry for delivery address changes
     */
    public static HoaDonAuditHistory deliveryAddressChangeEntry(Long hoaDonId, String oldAddress, String newAddress,
                                                              String nguoiThucHien, String lyDo) {
        String oldValues = oldAddress != null ? String.format("{\"diaChiGiaoHang\":\"%s\"}", oldAddress) : null;
        String newValues = newAddress != null ? String.format("{\"diaChiGiaoHang\":\"%s\"}", newAddress) : null;

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("DELIVERY_ADDRESS_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi địa chỉ giao hàng")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }

    /**
     * Create audit history entry for payment method changes
     */
    public static HoaDonAuditHistory paymentMethodChangeEntry(Long hoaDonId, String oldMethod, String newMethod,
                                                            String nguoiThucHien, String lyDo) {
        String oldValues = String.format("{\"phuongThucThanhToan\":\"%s\"}", oldMethod);
        String newValues = String.format("{\"phuongThucThanhToan\":\"%s\"}", newMethod);

        return HoaDonAuditHistory.builder()
                .hoaDonId(hoaDonId)
                .hanhDong("PAYMENT_METHOD_CHANGE")
                .thoiGianThayDoi(Instant.now())
                .nguoiThucHien(nguoiThucHien != null ? nguoiThucHien : "SYSTEM")
                .lyDoThayDoi(lyDo != null ? lyDo : "Thay đổi phương thức thanh toán")
                .giaTriCu(oldValues)
                .giaTriMoi(newValues)
                .build();
    }
}
