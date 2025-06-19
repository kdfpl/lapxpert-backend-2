package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.Language;
import lombok.Data;

@Data
public class BindingTokenRequest extends Request {
    private String partnerClientId;
    private String callbackToken;
    private String signature;

    public BindingTokenRequest(String partnerClientId, String callbackToken, String signature) {
        this.partnerClientId = partnerClientId;
        this.callbackToken = callbackToken;
        this.signature = signature;
    }

    public BindingTokenRequest(String partnerCode, String orderId, String requestId, Language lang, String partnerClientId, String callbackToken, String signature) {
        super(partnerCode, orderId, requestId, lang);
        this.partnerClientId = partnerClientId;
        this.callbackToken = callbackToken;
        this.signature = signature;
    }
}
