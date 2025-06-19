package com.lapxpert.backend.hoadon.dto;

import com.lapxpert.backend.hoadon.enums.PhuongThucThanhToan;
import com.lapxpert.backend.hoadon.enums.TrangThaiGiaoDich;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentDetailDto {
    private Long paymentId;
    private BigDecimal amount;
    private PhuongThucThanhToan paymentMethod;
    private String transactionRef;
    private Instant paymentTime;
    private TrangThaiGiaoDich status;
    private String notes;
    private Instant createdAt;
}
