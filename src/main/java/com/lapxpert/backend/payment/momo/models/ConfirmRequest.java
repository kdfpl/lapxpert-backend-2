package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.ConfirmRequestType;
import com.lapxpert.backend.payment.momo.enums.Language;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConfirmRequest extends Request {
    private Long amount;
    private String description;
    private ConfirmRequestType requestType;
    private String signature;

    public ConfirmRequest(Long amount, String description, ConfirmRequestType requestType, String signature) {
        this.amount = amount;
        this.description = description;
        this.requestType = requestType;
        this.signature = signature;
    }

    public ConfirmRequest(String partnerCode, String orderId, String requestId, Language lang, Long amount, String description, ConfirmRequestType requestType, String signature) {
        super(partnerCode, orderId, requestId, lang);
        this.amount = amount;
        this.description = description;
        this.requestType = requestType;
        this.signature = signature;
    }
}
