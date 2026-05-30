package com.finsafe.gateway.service;

import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Simulates the actual payment processing logic.
 * In a real system this would call a payment provider (Stripe, Paystack, etc.).
 */
@Service
public class PaymentService {

    /**
     * Simulates a 2-second processing delay and returns a successful payment response.
     */
    public PaymentResponse process(PaymentRequest request) throws InterruptedException {
        // Simulate network/processing latency
        Thread.sleep(2000);

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String message = String.format("Charged %.0f %s", request.getAmount(), request.getCurrency());

        return new PaymentResponse(
                "SUCCESS",
                message,
                transactionId,
                Instant.now(),
                201
        );
    }
}
