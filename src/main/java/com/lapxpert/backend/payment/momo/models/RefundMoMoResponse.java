package com.lapxpert.backend.payment.momo.models;

import lombok.Data;

@Data
public class RefundMoMoResponse extends Response {
    private String requestId;
    private Long amount;
    private Long transId;

    public RefundMoMoResponse(String requestId, Long amount, Long transId) {
        this.requestId = requestId;
        this.amount = amount;
        this.transId = transId;
    }
}
