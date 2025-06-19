package com.lapxpert.backend.payment.momo.models;

import lombok.Data;

@Data
public class PaymentResponse extends Response {

    public PaymentResponse(Integer resultCode, String message) {
        this.resultCode = resultCode;
        this.message = message;
    }

    private String requestId;

    private Long amount;

    private String payUrl;

    private String shortLink;

    private String deeplink;

    private String qrCodeUrl;

    private String deeplinkWebInApp;

    private Long transId;

    private String applink;

    private String partnerClientId;

    private String bindingUrl;

    private String deeplinkMiniApp;

}
