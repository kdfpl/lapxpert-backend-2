package com.lapxpert.backend.hoadon.domain.service;

import com.lapxpert.backend.hoadon.domain.entity.HoaDon;
import com.lapxpert.backend.hoadon.domain.entity.HoaDonAuditHistory;
import com.lapxpert.backend.hoadon.domain.repository.HoaDonAuditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enhanced audit service for comprehensive order tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoaDonAuditService {

    private final HoaDonAuditHistoryRepository auditHistoryRepository;

    /**
     * Log customer change in order
     */
    @Transactional
    public void logCustomerChange(Long hoaDonId, Long oldCustomerId, Long newCustomerId, 
                                String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.customerChangeEntry(
                hoaDonId, oldCustomerId, newCustomerId, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged customer change for order {}: {} -> {} by {}", 
                hoaDonId, oldCustomerId, newCustomerId, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log customer change for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log voucher application
     */
    @Transactional
    public void logVoucherApplication(Long hoaDonId, String voucherCode, BigDecimal discountAmount,
                                    String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.voucherApplicationEntry(
                hoaDonId, voucherCode, discountAmount.toString(), nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged voucher application for order {}: {} (discount: {}) by {}", 
                hoaDonId, voucherCode, discountAmount, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log voucher application for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log voucher removal
     */
    @Transactional
    public void logVoucherRemoval(Long hoaDonId, String voucherCode, BigDecimal discountAmount,
                                String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.voucherRemovalEntry(
                hoaDonId, voucherCode, discountAmount.toString(), nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged voucher removal for order {}: {} (discount: {}) by {}", 
                hoaDonId, voucherCode, discountAmount, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log voucher removal for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log order item changes
     */
    @Transactional
    public void logOrderItemChange(Long hoaDonId, String itemChanges, String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.orderItemChangeEntry(
                hoaDonId, itemChanges, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged order item change for order {} by {}: {}", 
                hoaDonId, nguoiThucHien, lyDo);
        } catch (Exception e) {
            log.error("Failed to log order item change for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log delivery address change
     */
    @Transactional
    public void logDeliveryAddressChange(Long hoaDonId, String oldAddress, String newAddress,
                                       String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.deliveryAddressChangeEntry(
                hoaDonId, oldAddress, newAddress, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged delivery address change for order {} by {}", hoaDonId, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log delivery address change for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log payment method change
     */
    @Transactional
    public void logPaymentMethodChange(Long hoaDonId, String oldMethod, String newMethod,
                                     String nguoiThucHien, String lyDo) {
        try {
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.paymentMethodChangeEntry(
                hoaDonId, oldMethod, newMethod, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            log.info("Logged payment method change for order {}: {} -> {} by {}", 
                hoaDonId, oldMethod, newMethod, nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log payment method change for order {}: {}", hoaDonId, e.getMessage(), e);
        }
    }

    /**
     * Log comprehensive order creation with detailed tracking
     */
    @Transactional
    public void logOrderCreation(HoaDon hoaDon, String nguoiThucHien, String lyDo) {
        try {
            // Create detailed audit entry for order creation
            String detailedValues = createDetailedAuditValues(hoaDon);
            HoaDonAuditHistory auditEntry = HoaDonAuditHistory.createEntry(
                hoaDon.getId(), detailedValues, nguoiThucHien, lyDo);
            auditHistoryRepository.save(auditEntry);
            
            // Log individual components for better tracking
            if (hoaDon.getKhachHang() != null) {
                logCustomerChange(hoaDon.getId(), null, hoaDon.getKhachHang().getId(), 
                    nguoiThucHien, "Gán khách hàng khi tạo đơn hàng");
            }
            
            log.info("Logged comprehensive order creation for order {} by {}", 
                hoaDon.getId(), nguoiThucHien);
        } catch (Exception e) {
            log.error("Failed to log order creation for order {}: {}", hoaDon.getId(), e.getMessage(), e);
        }
    }

    /**
     * Get comprehensive audit history for an order
     */
    @Transactional(readOnly = true)
    public List<HoaDonAuditHistory> getComprehensiveAuditHistory(Long hoaDonId) {
        return auditHistoryRepository.findByHoaDonIdOrderByThoiGianThayDoiDesc(hoaDonId);
    }

    /**
     * Get audit history by action type
     */
    @Transactional(readOnly = true)
    public List<HoaDonAuditHistory> getAuditHistoryByAction(Long hoaDonId, String action) {
        return auditHistoryRepository.findByHoaDonIdAndHanhDongOrderByThoiGianThayDoiDesc(hoaDonId, action);
    }

    /**
     * Create detailed JSON representation for audit trail
     */
    private String createDetailedAuditValues(HoaDon hoaDon) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append(String.format("\"maHoaDon\":\"%s\",", hoaDon.getMaHoaDon()));
        json.append(String.format("\"loaiHoaDon\":\"%s\",", hoaDon.getLoaiHoaDon()));
        json.append(String.format("\"trangThaiDonHang\":\"%s\",", hoaDon.getTrangThaiDonHang()));
        json.append(String.format("\"trangThaiThanhToan\":\"%s\",", hoaDon.getTrangThaiThanhToan()));
        json.append(String.format("\"tongThanhToan\":\"%s\",", hoaDon.getTongThanhToan()));
        json.append(String.format("\"phiVanChuyen\":\"%s\",", hoaDon.getPhiVanChuyen()));
        
        if (hoaDon.getKhachHang() != null) {
            json.append(String.format("\"khachHangId\":\"%s\",", hoaDon.getKhachHang().getId()));
            json.append(String.format("\"khachHangEmail\":\"%s\",", hoaDon.getKhachHang().getEmail()));
        }
        
        if (hoaDon.getNhanVien() != null) {
            json.append(String.format("\"nhanVienId\":\"%s\",", hoaDon.getNhanVien().getId()));
        }
        
        if (hoaDon.getDiaChiGiaoHang() != null) {
            json.append(String.format("\"diaChiGiaoHangId\":\"%s\",", hoaDon.getDiaChiGiaoHang().getId()));
        }
        
        json.append(String.format("\"soLuongSanPham\":\"%d\",", hoaDon.getHoaDonChiTiets().size()));
        json.append(String.format("\"soLuongVoucher\":\"%d\"", hoaDon.getHoaDonPhieuGiamGias().size()));
        json.append("}");
        
        return json.toString();
    }
}
