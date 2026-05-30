package com.finsafe.gateway.model;

import java.time.Instant;

/**
 * Represents a stored idempotency record in the in-memory cache.
 * Tracks the original request body, the computed response, and processing state.
 */
public class IdempotencyRecord {

    public enum State { IN_FLIGHT, COMPLETED }

    private final String key;
    private final PaymentRequest originalRequest;
    private PaymentResponse response;
    private State state;
    private final Instant createdAt;

    public IdempotencyRecord(String key, PaymentRequest originalRequest) {
        this.key = key;
        this.originalRequest = originalRequest;
        this.state = State.IN_FLIGHT;
        this.createdAt = Instant.now();
    }

    public String getKey() { return key; }
    public PaymentRequest getOriginalRequest() { return originalRequest; }

    public PaymentResponse getResponse() { return response; }
    public void setResponse(PaymentResponse response) { this.response = response; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Instant getCreatedAt() { return createdAt; }
}
