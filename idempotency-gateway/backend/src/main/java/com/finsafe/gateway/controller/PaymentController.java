package com.finsafe.gateway.controller;

import com.finsafe.gateway.exception.MissingIdempotencyKeyException;
import com.finsafe.gateway.model.IdempotencyRecord;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import com.finsafe.gateway.service.IdempotencyService;
import com.finsafe.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Payment Gateway", description = "Idempotent payment processing API")
public class PaymentController {

    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;

    public PaymentController(IdempotencyService idempotencyService, PaymentService paymentService) {
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
    }

    @Operation(
        summary = "Process a payment",
        description = "Processes a payment exactly once. Safe to retry with the same Idempotency-Key."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
        @ApiResponse(responseCode = "400", description = "Missing Idempotency-Key header or invalid body"),
        @ApiResponse(responseCode = "422", description = "Same key used with a different request body")
    })
    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "Unique key per payment attempt (UUID recommended)", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) throws InterruptedException {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required.");
        }

        IdempotencyRecord existing = idempotencyService.getOrCreate(idempotencyKey, request);

        if (existing == null) {
            PaymentResponse response = paymentService.process(request);
            idempotencyService.complete(idempotencyKey, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        if (existing.getState() == IdempotencyRecord.State.IN_FLIGHT) {
            PaymentResponse response = idempotencyService.waitForCompletion(existing);
            return ResponseEntity.status(existing.getResponse().getHttpStatus())
                    .header("X-Cache-Hit", "true")
                    .body(response);
        }

        return ResponseEntity.status(existing.getResponse().getHttpStatus())
                .header("X-Cache-Hit", "true")
                .body(existing.getResponse());
    }

    @Operation(summary = "List all stored idempotency keys")
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
