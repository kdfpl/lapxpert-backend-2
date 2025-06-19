package com.lapxpert.backend.payment.momo.models;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hai.Nguyen Date: 19-01-2018
 */
@Data
public class QueryStatusTransactionResponse extends Response {

    String requestId;
    String metaData;
    List<RefundOfQueryStatusTransaction> refundTrans = new ArrayList<>();
    private Long amount;
    private String partnerUserId;
    private Long transId;
    private String extraData;
    private String payType;

    public QueryStatusTransactionResponse(String requestId, String metaData, List<RefundOfQueryStatusTransaction> refundTrans, Long amount, String partnerUserId, Long transId, String extraData, String payType) {
        this.requestId = requestId;
        this.metaData = metaData;
        this.refundTrans = refundTrans;
        this.amount = amount;
        this.partnerUserId = partnerUserId;
        this.transId = transId;
        this.extraData = extraData;
        this.payType = payType;
    }
}
