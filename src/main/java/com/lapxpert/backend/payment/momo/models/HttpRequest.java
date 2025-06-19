package com.lapxpert.backend.payment.momo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequest {
    private String method;
    private String endpoint;
    private String payload;
    private String contentType;
}
