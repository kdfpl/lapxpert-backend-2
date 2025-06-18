package com.lapxpert.backend.hoadon.dto;

import com.lapxpert.backend.hoadon.enums.TrangThaiThanhToan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PaymentSummaryDto {
    private Long orderId;
    private BigDecimal orderTotal;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private TrangThaiThanhToan paymentStatus;
    private List<PaymentDetailDto> payments;
}
