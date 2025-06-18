package com.lapxpert.backend.hoadon.service;

import com.lapxpert.backend.hoadon.entity.HoaDon;
import com.lapxpert.backend.hoadon.enums.LoaiHoaDon;
import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.enums.TrangThaiDonHang;
import com.lapxpert.backend.hoadon.enums.TrangThaiThanhToan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced payment method validation service with sophisticated business rules.
 * Provides comprehensive validation for payment methods based on order context,
 * business hours, amount limits, and other business constraints.
 */
@Service
@Slf4j
public class PaymentMethodValidationService {

    // Business configuration constants
    private static final BigDecimal CASH_LIMIT_AMOUNT = new BigDecimal("10000000"); // 10M VND (includes COD consolidated)
    private static final BigDecimal VNPAY_MIN_AMOUNT = new BigDecimal("10000");     // 10K VND
    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);            // 8:00 AM
    private static final LocalTime BUSINESS_END = LocalTime.of(22, 0);             // 10:00 PM

    /**
     * Comprehensive validation result containing validation status and detailed feedback.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final String recommendation;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings, String recommendation) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.recommendation = recommendation;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public String getRecommendation() { return recommendation; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }

    /**
     * Validate payment method for order creation.
     * 
     * @param hoaDon Order to validate
     * @param phuongThucThanhToan Payment method to validate
     * @return Comprehensive validation result
     */
    public ValidationResult validatePaymentMethod(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String recommendation = null;

        // Basic null checks
        if (hoaDon == null) {
            errors.add("Thông tin đơn hàng không được để trống");
            return new ValidationResult(false, errors, warnings, null);
        }

        if (phuongThucThanhToan == null) {
            errors.add("Phương thức thanh toán không được để trống");
            return new ValidationResult(false, errors, warnings, null);
        }

        // Validate based on order type
        validateOrderTypeCompatibility(hoaDon, phuongThucThanhToan, errors, warnings);

        // Validate amount limits
        validateAmountLimits(hoaDon, phuongThucThanhToan, errors, warnings);

        // Validate business hours
        validateBusinessHours(phuongThucThanhToan, warnings);

        // Validate order status
        validateOrderStatus(hoaDon, phuongThucThanhToan, errors, warnings);

        // Generate recommendation
        recommendation = generateRecommendation(hoaDon, phuongThucThanhToan, errors, warnings);

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings, recommendation);
    }

    /**
     * Validate payment method for payment confirmation.
     */
    public ValidationResult validatePaymentConfirmation(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check if payment is already confirmed
        if (hoaDon.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            errors.add("Đơn hàng đã được thanh toán");
        }

        // Check if order is cancelled
        if (hoaDon.getTrangThaiDonHang() == TrangThaiDonHang.DA_HUY) {
            errors.add("Không thể xác nhận thanh toán cho đơn hàng đã hủy");
        }

        // Note: Payment method is not stored in HoaDon entity, so we skip this validation
        // This could be enhanced if payment method is added to the entity in the future

        // Additional validation for specific payment methods
        validateSpecificPaymentMethod(hoaDon, phuongThucThanhToan, errors, warnings);

        boolean isValid = errors.isEmpty();
        String recommendation = generateConfirmationRecommendation(hoaDon, phuongThucThanhToan);
        
        return new ValidationResult(isValid, errors, warnings, recommendation);
    }

    /**
     * Validate order type compatibility with payment method.
     */
    private void validateOrderTypeCompatibility(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan, 
                                              List<String> errors, List<String> warnings) {
        LoaiHoaDon loaiHoaDon = hoaDon.getLoaiHoaDon();

        switch (phuongThucThanhToan) {
            case TIEN_MAT:
                // TIEN_MAT now includes both cash payments and cash on delivery (consolidated from COD)
                // Allow for both POS and delivery scenarios
                if (loaiHoaDon == LoaiHoaDon.ONLINE && hoaDon.getDiaChiGiaoHang() == null) {
                    errors.add("Thanh toán tiền mặt cho đơn hàng online yêu cầu địa chỉ giao hàng");
                }
                break;
            case VNPAY:
                // VNPAY is flexible for both order types
                if (loaiHoaDon == LoaiHoaDon.TAI_QUAY) {
                    warnings.add("VNPAY cho đơn hàng POS có thể cần thêm thời gian xử lý");
                }
                break;
            case MOMO:
                // MoMo e-wallet payment is flexible for both order types
                if (loaiHoaDon == LoaiHoaDon.TAI_QUAY) {
                    warnings.add("MoMo cho đơn hàng POS có thể cần thêm thời gian xử lý");
                }
                break;
            case VIETQR:
                // VietQR bank transfer payment is flexible for both order types
                if (loaiHoaDon == LoaiHoaDon.TAI_QUAY) {
                    warnings.add("VietQR cho đơn hàng POS có thể cần thêm thời gian xử lý");
                }
                break;
        }
    }

    /**
     * Validate amount limits for different payment methods.
     */
    private void validateAmountLimits(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan, 
                                    List<String> errors, List<String> warnings) {
        BigDecimal tongThanhToan = hoaDon.getTongThanhToan();
        if (tongThanhToan == null) {
            warnings.add("Không thể xác định tổng tiền thanh toán");
            return;
        }

        switch (phuongThucThanhToan) {
            case TIEN_MAT:
                // TIEN_MAT now includes both cash and COD scenarios
                if (tongThanhToan.compareTo(CASH_LIMIT_AMOUNT) > 0) {
                    errors.add(String.format("Thanh toán tiền mặt không được vượt quá %s VND",
                                            formatCurrency(CASH_LIMIT_AMOUNT)));
                } else if (tongThanhToan.compareTo(new BigDecimal("1000000")) > 0) {
                    warnings.add("Số tiền lớn - khuyến nghị sử dụng phương thức thanh toán điện tử");
                }
                break;
            case VNPAY:
                if (tongThanhToan.compareTo(VNPAY_MIN_AMOUNT) < 0) {
                    errors.add(String.format("Thanh toán VNPAY yêu cầu tối thiểu %s VND",
                                            formatCurrency(VNPAY_MIN_AMOUNT)));
                }
                break;
            case MOMO:
                if (tongThanhToan.compareTo(VNPAY_MIN_AMOUNT) < 0) {
                    errors.add(String.format("Thanh toán MoMo yêu cầu tối thiểu %s VND",
                                            formatCurrency(VNPAY_MIN_AMOUNT)));
                }
                break;
            case VIETQR:
                if (tongThanhToan.compareTo(VNPAY_MIN_AMOUNT) < 0) {
                    errors.add(String.format("Thanh toán VietQR yêu cầu tối thiểu %s VND",
                                            formatCurrency(VNPAY_MIN_AMOUNT)));
                }
                break;
        }
    }

    /**
     * Validate business hours for payment methods.
     */
    private void validateBusinessHours(PhuongThucThanhToan phuongThucThanhToan, List<String> warnings) {
        LocalTime currentTime = LocalTime.now();
        
        if (phuongThucThanhToan == PhuongThucThanhToan.VNPAY) {
            if (currentTime.isBefore(BUSINESS_START) || currentTime.isAfter(BUSINESS_END)) {
                warnings.add("Thanh toán VNPAY ngoài giờ làm việc có thể gặp chậm trễ");
            }
        }
    }

    /**
     * Validate order status for payment method.
     */
    private void validateOrderStatus(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan, 
                                   List<String> errors, List<String> warnings) {
        TrangThaiDonHang trangThai = hoaDon.getTrangThaiDonHang();
        
        if (trangThai == TrangThaiDonHang.DA_HUY) {
            errors.add("Không thể thiết lập phương thức thanh toán cho đơn hàng đã hủy");
        } else if (trangThai == TrangThaiDonHang.HOAN_THANH) {
            warnings.add("Đơn hàng đã hoàn thành - thay đổi phương thức thanh toán có thể không cần thiết");
        }
    }

    /**
     * Validate specific payment method requirements.
     */
    private void validateSpecificPaymentMethod(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan,
                                             List<String> errors, List<String> warnings) {
        switch (phuongThucThanhToan) {
            case TIEN_MAT:
                // Cash payment specific validations (includes COD scenarios)
                if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE && hoaDon.getDiaChiGiaoHang() == null) {
                    errors.add("Thanh toán tiền mặt cho đơn hàng online yêu cầu địa chỉ giao hàng");
                }
                if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE &&
                    (hoaDon.getNguoiNhanSdt() == null || hoaDon.getNguoiNhanSdt().trim().isEmpty())) {
                    warnings.add("Khuyến nghị có số điện thoại người nhận cho thanh toán tiền mặt khi giao hàng");
                }
                break;
            case VNPAY:
                // Additional VNPAY specific validations can be added here
                break;
            case MOMO:
                // Additional MoMo specific validations can be added here
                break;
            case VIETQR:
                // Additional VietQR specific validations can be added here
                break;
        }
    }

    /**
     * Generate recommendation based on validation results.
     */
    private String generateRecommendation(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan, 
                                        List<String> errors, List<String> warnings) {
        if (!errors.isEmpty()) {
            return "Vui lòng khắc phục các lỗi trước khi tiếp tục";
        }

        BigDecimal tongThanhToan = hoaDon.getTongThanhToan();
        if (tongThanhToan == null) {
            return null;
        }

        // Generate smart recommendations
        if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
            if (tongThanhToan.compareTo(new BigDecimal("500000")) > 0) {
                return "Khuyến nghị sử dụng VNPAY cho đơn hàng online có giá trị cao";
            } else {
                return "Tiền mặt khi giao hàng hoặc VNPAY đều phù hợp cho đơn hàng này";
            }
        } else {
            if (tongThanhToan.compareTo(new BigDecimal("1000000")) > 0) {
                return "Khuyến nghị sử dụng VNPAY cho đơn hàng POS có giá trị cao";
            } else {
                return "Tiền mặt hoặc VNPAY đều phù hợp cho đơn hàng POS này";
            }
        }
    }

    /**
     * Generate recommendation for payment confirmation.
     */
    private String generateConfirmationRecommendation(HoaDon hoaDon, PhuongThucThanhToan phuongThucThanhToan) {
        switch (phuongThucThanhToan) {
            case TIEN_MAT:
                if (hoaDon.getLoaiHoaDon() == LoaiHoaDon.ONLINE) {
                    return "Xác nhận khi shipper đã thu tiền thành công";
                } else {
                    return "Xác nhận đã nhận đủ tiền mặt từ khách hàng";
                }
            case VNPAY:
                return "Xác nhận sau khi nhận được thông báo thanh toán thành công từ VNPAY";
            case MOMO:
                return "Xác nhận sau khi nhận được thông báo thanh toán thành công từ MoMo";
            case VIETQR:
                return "Xác nhận sau khi nhận được thông báo chuyển khoản thành công từ ngân hàng";
            default:
                return "Xác nhận thanh toán sau khi hoàn tất giao dịch";
        }
    }

    /**
     * Format currency for display.
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
