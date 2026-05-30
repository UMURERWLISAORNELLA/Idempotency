package com.finsafe.gateway.service;

import com.finsafe.gateway.exception.IdempotencyConflictException;
import com.finsafe.gateway.model.IdempotencyRecord;
import com.finsafe.gateway.model.PaymentRequest;
import com.finsafe.gateway.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages idempotency records in memory.
 *
 * Thread-safety strategy:
 *  - ConcurrentHashMap for the store.
 *  - computeIfAbsent is atomic, so only one thread can "win" the first insert.
 *  - synchronized on the record object handles the IN_FLIGHT wait (bonus story).
 *  - A scheduled cleanup removes expired keys based on TTL.
 */
@Service
public class IdempotencyService {

    private final ConcurrentHashMap<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @Value("${idempotency.key.ttl-minutes:1440}")
    private long ttlMinutes;

    /**
     * Returns null  → key is brand new, caller should process the payment.
     * Returns record → key already exists; record.state tells you if it's done or in-flight.
     *
     * Throws IdempotencyConflictException if the key exists but the body differs.
     */
    public IdempotencyRecord getOrCreate(String key, PaymentRequest request) {
        // Atomically insert a new IN_FLIGHT record if the key is absent
        IdempotencyRecord[] holder = new IdempotencyRecord[1];
        boolean[] isNew = {false};

        store.computeIfAbsent(key, k -> {
            IdempotencyRecord rec = new IdempotencyRecord(k, request);
            holder[0] = rec;
            isNew[0] = true;
            return rec;
        });

        if (isNew[0]) {
            // We just created it — caller owns processing
            return null;
        }

        // Key already exists
        IdempotencyRecord existing = store.get(key);

        // Body mismatch check
        if (!existing.getOriginalRequest().equals(request)) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used for a different request body.");
        }

        return existing;
    }

    /**
     * Mark a record as COMPLETED and store the response.
     * Notifies any threads waiting on this record (in-flight scenario).
     */
    public void complete(String key, PaymentResponse response) {
        IdempotencyRecord record = store.get(key);
        if (record != null) {
            synchronized (record) {
                record.setResponse(response);
                record.setState(IdempotencyRecord.State.COMPLETED);
                record.notifyAll(); // wake up any waiting duplicate requests
            }
        }
    }

    /**
     * Wait for an IN_FLIGHT record to complete (bonus: race condition handling).
     * Blocks the calling thread until the record transitions to COMPLETED.
     */
    public PaymentResponse waitForCompletion(IdempotencyRecord record) throws InterruptedException {
        synchronized (record) {
            while (record.getState() == IdempotencyRecord.State.IN_FLIGHT) {
                record.wait(30_000); // max 30s wait to avoid indefinite blocking
            }
            return record.getResponse();
        }
    }

    /**
     * Remove expired keys from the store (called by the cleanup scheduler).
     */
    public void evictExpiredKeys() {
        Instant cutoff = Instant.now().minusSeconds(ttlMinutes * 60);
        store.entrySet().removeIf(entry -> entry.getValue().getCreatedAt().isBefore(cutoff));
    }

    /**
     * Returns a snapshot of all stored records (for the dashboard endpoint).
     */
    public ConcurrentHashMap<String, IdempotencyRecord> getStore() {
        return store;
    }
}
