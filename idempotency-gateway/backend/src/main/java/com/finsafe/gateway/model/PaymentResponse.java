package com.finsafe.gateway.model;

import java.time.Instant;

public class PaymentResponse {

    private String status;
    private String message;
    private String transactionId;
    private Instant processedAt;
    private int httpStatus;

    public PaymentResponse() {}

    public PaymentResponse(String status, String message, String transactionId, Instant processedAt, int httpStatus) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
        this.processedAt = processedAt;
        this.httpStatus = httpStatus;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
}
