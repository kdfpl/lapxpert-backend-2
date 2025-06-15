package com.lapxpert.backend.hoadon.domain.enums;

/**
 * Payment methods supporting flexible business scenarios.
 * Each method can be used with different order types based on business rules.
 *
 * Note: COD payment method has been consolidated with TIEN_MAT for simplified payment processing.
 * Existing COD orders are automatically migrated to TIEN_MAT for backward compatibility.
 */
public enum PhuongThucThanhToan {
    TIEN_MAT,           // Cash - for in-store/POS transactions and cash on delivery (consolidated from COD)
    VNPAY,              // VNPAY digital payment - versatile for both online and POS channels
    MOMO,               // MoMo e-wallet payment - mobile-first payment solution
    VIETQR              // VietQR bank transfer payment - QR code-based bank transfers
}
