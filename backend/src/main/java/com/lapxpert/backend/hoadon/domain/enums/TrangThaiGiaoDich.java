package com.lapxpert.backend.hoadon.domain.enums;

public enum TrangThaiGiaoDich {
    CHO_XU_LY,       // Pending
    THANH_CONG,      // Successful
    THAT_BAI,        // Failed
    DA_HOAN_TIEN,    // Refunded
    DANG_XU_LY_HOAN_TIEN, // Processing Refund
    HUY             // Cancelled
}
