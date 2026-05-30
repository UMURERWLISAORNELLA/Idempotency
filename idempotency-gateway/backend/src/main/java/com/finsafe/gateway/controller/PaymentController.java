package com.finsafe.gateway.controller;

import com.finsafe.gateway.exception.MissingIdempotencyKeyException;
import com.finsafe.gateway.model.IdempotencyRecord;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import com.finsafe.gateway.service.IdempotencyService;
import com.finsafe.gateway.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;

    public PaymentController(IdempotencyService idempotencyService, PaymentService paymentService) {
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
    }

    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) throws InterruptedException {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required.");
        }

        // Try to register this key — throws 422 on body mismatch
        IdempotencyRecord existing = idempotencyService.getOrCreate(idempotencyKey, request);

        if (existing == null) {
            // Brand new key — process the payment
            PaymentResponse response = paymentService.process(request);
            idempotencyService.complete(idempotencyKey, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        if (existing.getState() == IdempotencyRecord.State.IN_FLIGHT) {
            // Bonus: duplicate arrived while first is still processing — wait for it
            PaymentResponse response = idempotencyService.waitForCompletion(existing);
            return ResponseEntity.status(existing.getResponse().getHttpStatus())
                    .header("X-Cache-Hit", "true")
                    .body(response);
        }

        // Already completed — return cached response
        return ResponseEntity.status(existing.getResponse().getHttpStatus())
                .header("X-Cache-Hit", "true")
                .body(existing.getResponse());
    }

    /**
     * Dashboard endpoint: returns all stored idempotency records (for the React UI).
     */
    @GetMapping("/keys")
    public ResponseEntity<Object> listKeys() {
        var records = idempotencyService.getStore().entrySet().stream()
                .map(e -> Map.of(
                        "key", e.getKey(),
                        "state", e.getValue().getState().name(),
                        "amount", e.getValue().getOriginalRequest().getAmount(),
                        "currency", e.getValue().getOriginalRequest().getCurrency(),
                        "createdAt", e.getValue().getCreatedAt().toString(),
                        "transactionId", e.getValue().getResponse() != null
                                ? e.getValue().getResponse().getTransactionId() : "pending"
                ))
                .toList();
        return ResponseEntity.ok(records);
    }
}
