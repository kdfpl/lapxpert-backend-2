package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.Language;
import lombok.Data;

@Data
public class RefundMoMoRequest extends Request {
    private Long amount;
    private Long transId;
    private String signature;
    private String description;

    public RefundMoMoRequest(Long amount, Long transId, String signature, String description) {
        this.amount = amount;
        this.transId = transId;
        this.signature = signature;
        this.description = description;
    }

    public RefundMoMoRequest(String partnerCode, String orderId, String requestId, Language lang, Long amount, Long transId, String signature, String description) {
        super(partnerCode, orderId, requestId, lang);
        this.amount = amount;
        this.transId = transId;
        this.signature = signature;
        this.description = description;
    }
}
