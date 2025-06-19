package com.lapxpert.backend.payment.momo.models;

import com.lapxpert.backend.payment.momo.enums.ConfirmRequestType;

public class ConfirmResponse extends Response {
    private Long amount;
    private Long transId;
    private String requestId;
    private ConfirmRequestType requestType;
}
