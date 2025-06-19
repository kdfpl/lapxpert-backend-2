package com.lapxpert.backend.payment.momo.models;

import lombok.Data;

@Data
public class BindingTokenResponse extends Response{
    private String requestId;
    private String partnerClientId;
    private String aesToken;

    public BindingTokenResponse(String requestId, String partnerClientId, String aesToken) {
        this.requestId = requestId;
        this.partnerClientId = partnerClientId;
        this.aesToken = aesToken;
    }
}
