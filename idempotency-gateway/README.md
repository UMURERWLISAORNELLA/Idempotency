# FinSafe Idempotency Gateway

A RESTful payment processing API with an idempotency layer that guarantees **exactly-once** payment execution, no matter how many times a client retries.

---

## Architecture Diagram

```
Client (e-commerce shop)
        |
        | POST /api/process-payment
        | Headers: Idempotency-Key: <uuid>
        | Body: { amount, currency }
        |
        v
+---------------------------+
|   PaymentController       |
|  (Spring Boot REST API)   |
+---------------------------+
        |
        | 1. Validate Idempotency-Key header
        v
+---------------------------+
|   IdempotencyService      |
|  (ConcurrentHashMap store)|
+---------------------------+
        |
        |--- Key NOT found? ---------> Insert as IN_FLIGHT
        |                                    |
        |                                    v
        |                          +------------------+
        |                          |  PaymentService  |
        |                          |  (2s simulation) |
        |                          +------------------+
        |                                    |
        |                                    v
        |                          Mark COMPLETED, store response
        |                                    |
        |                                    v
        |                          Return 201 Created
        |
        |--- Key found, same body?
        |       |
        |       |--- State = IN_FLIGHT? --> Wait (block) until COMPLETED
        |       |                               |
        |       |                               v
        |       |                       Return cached response
        |       |                       Header: X-Cache-Hit: true
        |       |
        |       |--- State = COMPLETED? --> Return cached response immediately
        |                                   Header: X-Cache-Hit: true
        |
        |--- Key found, DIFFERENT body? --> 422 Unprocessable Entity
```

**Sequence for duplicate in-flight request (Bonus Story):**

```
Client A ──POST (key=abc)──► Gateway ──► IN_FLIGHT ──► PaymentService (2s)
Client B ──POST (key=abc)──► Gateway ──► sees IN_FLIGHT ──► wait()
                                                                  |
                                              PaymentService done ──► notifyAll()
                                                                  |
Client B ◄── same response ◄──────────────────────────────────────┘
```

---

## Setup Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |

---

### Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:
```bash
cd backend
mvnw.cmd spring-boot:run
```

The API starts on **http://localhost:8080**

> First run downloads dependencies (~1 min). Subsequent runs are instant.

---

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**

The Vite dev server proxies `/api` requests to the Spring Boot backend automatically.

---

## API Documentation

### `POST /api/process-payment`

Process a payment. Idempotent — safe to retry.

**Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `Idempotency-Key` | Yes | Unique string per payment attempt (UUID recommended) |
| `Content-Type` | Yes | `application/json` |

**Request Body:**

```json
{
  "amount": 100,
  "currency": "GHS"
}
```

**Responses:**

| Scenario | Status | Body | Extra Header |
|----------|--------|------|--------------|
| First request (processed) | `201 Created` | `{ "status": "SUCCESS", "message": "Charged 100 GHS", "transactionId": "TXN-XXXXXXXX", "processedAt": "..." }` | — |
| Duplicate request (same body) | `201 Created` | Same as above | `X-Cache-Hit: true` |
| Duplicate in-flight | `201 Created` | Same as above (after wait) | `X-Cache-Hit: true` |
| Same key, different body | `422 Unprocessable Entity` | `{ "error": "Idempotency key already used for a different request body." }` | — |
| Missing key header | `400 Bad Request` | `{ "error": "Idempotency-Key header is required." }` | — |

---

**Example — First Request:**

```bash
curl -X POST http://localhost:8080/api/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"amount": 100, "currency": "GHS"}'
```

```json
{
  "status": "SUCCESS",
  "message": "Charged 100 GHS",
  "transactionId": "TXN-A3F9B2C1",
  "processedAt": "2024-01-15T10:30:00Z",
  "httpStatus": 201
}
```

**Example — Duplicate Request (same key):**

```bash
curl -v -X POST http://localhost:8080/api/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"amount": 100, "currency": "GHS"}'
```

Response includes header: `X-Cache-Hit: true` and the exact same body — no 2-second delay.

**Example — Body Mismatch:**

```bash
curl -X POST http://localhost:8080/api/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"amount": 500, "currency": "GHS"}'
```

```json
{ "error": "Idempotency key already used for a different request body." }
```

---

### `GET /api/keys`

Returns all stored idempotency records (used by the dashboard).

```bash
curl http://localhost:8080/api/keys
```

---

## Design Decisions

### In-Memory Store (`ConcurrentHashMap`)
Chosen for simplicity and zero external dependencies. In production, Redis would be the right choice for distributed deployments. The interface is abstracted enough that swapping the store is straightforward.

### Thread Safety for In-Flight Requests
`computeIfAbsent` on `ConcurrentHashMap` is atomic — only one thread can "win" the first insert. For the in-flight wait, we `synchronized` on the record object and use `wait()`/`notifyAll()`, which is a clean, low-overhead solution for this use case.

### `equals()` on `PaymentRequest`
Body comparison uses `equals()` on `amount` and `currency`. This is intentional — it catches fraud/error cases (User Story 3) without being brittle about field ordering or whitespace.

---

## Developer's Choice Feature: TTL-Based Key Expiration

**What:** Idempotency keys automatically expire after 24 hours (configurable via `idempotency.key.ttl-minutes` in `application.properties`). A background scheduler runs every 10 minutes to evict expired keys.

**Why:** In a real Fintech system, keeping keys forever wastes memory and creates ambiguity across billing cycles. A 24-hour TTL is the industry standard (Stripe, Adyen, and Paystack all use it). It also means a client can safely reuse a key the next day for a new transaction without getting a false conflict error.

**Configuration:**
```properties
# application.properties
idempotency.key.ttl-minutes=1440  # 24 hours (default)
```
the deployed link is : https://idempotency-2.onrender.com on render
