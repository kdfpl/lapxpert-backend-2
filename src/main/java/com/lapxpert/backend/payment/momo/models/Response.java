package com.lapxpert.backend.payment.momo.models;

import lombok.Data;

@Data
public class Response {
    protected long responseTime;

    public long getResponseTime() {
        return  System.currentTimeMillis();
    }
    protected String message;

    private String partnerCode;
    private String orderId;
    protected Integer resultCode;

    public Response() {
        this.responseTime = System.currentTimeMillis();
    }
}
