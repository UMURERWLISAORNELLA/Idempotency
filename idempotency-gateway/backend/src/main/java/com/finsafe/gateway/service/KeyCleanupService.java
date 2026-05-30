package com.finsafe.gateway.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Developer's Choice Feature: Automatic TTL-based key expiration.
 *
 * Why: In a real Fintech system, idempotency keys should not live forever.
 * Keeping them indefinitely wastes memory and could allow key collisions
 * across unrelated billing cycles. A 24-hour TTL (configurable) is a common
 * industry standard (used by Stripe, Adyen, etc.).
 *
 * This scheduler runs every 10 minutes and evicts keys older than the TTL.
 */
@Service
@EnableScheduling
public class KeyCleanupService {

    private final IdempotencyService idempotencyService;

    public KeyCleanupService(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(fixedDelay = 600_000) // every 10 minutes
    public void cleanupExpiredKeys() {
        idempotencyService.evictExpiredKeys();
    }
}
