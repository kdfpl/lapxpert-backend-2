package com.lapxpert.backend.hoadon.domain.enums;

/**
 * Payment methods supporting flexible business scenarios.
 * Each method can be used with different order types based on business rules.
 */
public enum PhuongThucThanhToan {
    TIEN_MAT,           // Cash - for in-store/POS transactions (requires physical presence)
    COD,                // Cash On Delivery - for both online and in-store orders with delivery
    VNPAY               // VNPAY digital payment - versatile for both online and POS channels
}
