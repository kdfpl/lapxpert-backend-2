package com.lapxpert.backend.thongke.application.enity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface HoaDonSanPhamView {
    String getTenSanPham();
    LocalDateTime getNgayTao();
    BigDecimal getGiaBan();
    String getTrangThaiGiaoHang();
}