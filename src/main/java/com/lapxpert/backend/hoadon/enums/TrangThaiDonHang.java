package com.lapxpert.backend.hoadon.enums;

public enum TrangThaiDonHang {
    CHO_XAC_NHAN,       // Chờ xác nhận
    DA_XAC_NHAN,        // Đã xác nhận (chờ xử lý/đóng gói)
    DANG_XU_LY,         // Đang xử lý/đóng gói
    CHO_GIAO_HANG,      // Chờ giao cho đơn vị vận chuyển
    DANG_GIAO_HANG,     // Đang giao hàng
    DA_GIAO_HANG,       // Đã giao hàng thành công
    HOAN_THANH,         // Hoàn thành (sau khi đã giao và không có vấn đề)
    DA_HUY,             // Đã hủy
    YEU_CAU_TRA_HANG,   // Yêu cầu trả hàng
    DA_TRA_HANG         // Đã trả hàng thành công
}
