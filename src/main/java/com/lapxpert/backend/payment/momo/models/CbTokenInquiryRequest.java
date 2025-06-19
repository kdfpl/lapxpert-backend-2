package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.Language;
import lombok.Data;

@Data
public class CbTokenInquiryRequest extends Request {
    private String partnerClientId;
    private String signature;

    public CbTokenInquiryRequest(String partnerClientId, String signature) {
        this.partnerClientId = partnerClientId;
        this.signature = signature;
    }

    public CbTokenInquiryRequest(String partnerCode, String orderId, String requestId, Language lang, String partnerClientId, String signature) {
        super(partnerCode, orderId, requestId, lang);
        this.partnerClientId = partnerClientId;
        this.signature = signature;
    }
}
