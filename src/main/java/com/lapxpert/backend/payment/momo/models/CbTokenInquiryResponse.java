package com.lapxpert.backend.payment.momo.models;

import lombok.Data;

@Data
public class CbTokenInquiryResponse extends Response {
    private String requestId;
    private String callbackToken;

    public CbTokenInquiryResponse(String requestId, String callbackToken) {
        this.requestId = requestId;
        this.callbackToken = callbackToken;
    }
}
