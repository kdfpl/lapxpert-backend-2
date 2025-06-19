package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.Language;
import lombok.Data;

@Data
public class DeleteTokenRequest extends Request {
    private String partnerClientId;
    private String token;
    private String signature;

    public DeleteTokenRequest(String partnerClientId, String token, String signature) {
        this.partnerClientId = partnerClientId;
        this.token = token;
        this.signature = signature;
    }

    public DeleteTokenRequest(String partnerCode, String orderId, String requestId, Language lang, String partnerClientId, String token, String signature) {
        super(partnerCode, orderId, requestId, lang);
        this.partnerClientId = partnerClientId;
        this.token = token;
        this.signature = signature;
    }

}
