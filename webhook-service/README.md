# Webhook Service

Secure webhook ingestion and processing service for payment providers (Paystack, Interswitch, SquadCo).

## 🎯 Purpose

This service receives asynchronous webhook notifications from payment providers when payment status changes, validates
them, and updates the corresponding transactions in the payment-service.

## 🏗️ Architecture

┌─────────────────────────────────────────────────────────────┐
│ PAYMENT PROVIDERS (Paystack, Interswitch, SquadCo) │
│ │
│ Payment completed → POST webhook to /webhooks/{provider} │
└────────────────────┬────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────┐
│ WEBHOOK SERVICE │
│ │
│ 1. WebhookController │
│ - Receives HTTP POST │
│ - Persists RAW payload to database (audit trail) │
│ - Returns HTTP 200 immediately │
│ │
│ 2. WebhookService │
│ - Validates signature (HMAC-SHA512) │
│ - Checks idempotency (prevents duplicates) │
│ - Normalizes event type │
│ - Publishes to Kafka topic "webhook-events" │
│ │
│ 3. WebhookEventConsumer │
│ - Consumes from Kafka │
│ - Calls payment-service to update transaction status │
│ - Marks webhook as PROCESSED in database │
└────────────────────┬────────────────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────┐
│ PAYMENT SERVICE │
│ │
│ PATCH /internal/payments/status │
│ - Updates transaction status in database │
│ - Publishes PaymentStatusUpdatedEvent to Kafka │
└─────────────────────────────────────────────────────────────┘

## 📡 Webhook Endpoints

All endpoints are **PUBLIC** (no authentication required). Security is enforced via signature validation.

| Provider    | Endpoint                     | Signature Header       |
|-------------|------------------------------|------------------------|
| Paystack    | `POST /webhooks/paystack`    | `x-paystack-signature` |
| Interswitch | `POST /webhooks/interswitch` | `Authorization`        |
| SquadCo     | `POST /webhooks/squadco`     | (Provider-specific)    |

### Example: Paystack Webhook

```bash
curl -X POST http://localhost:8083/webhooks/paystack \
  -H "x-paystack-signature: t=1234567890,v1=abc123def456..." \
  -H "Content-Type: application/json" \
  -d '{
    "event": "charge.success",
    "data": {
      "id": 123456789,
      "reference": "txn_abc123xyz",
      "amount": 150000,
      "currency": "NGN",
      "status": "success",
      "customer": {
        "email": "customer@example.com"
      }
    }
  }'
```

Response: HTTP 200 OK (immediate acknowledgment)
🔐 Security Features

1. Signature Validation
   Each provider signs webhook payloads with a secret key. This service validates the signature before processing.
   Paystack Signature Format:

```text
x-paystack-signature: t=<timestamp>,v1=<hmac-sha512-signature>
```

Validation Process:
Extract v1 signature from header
Compute HMAC-SHA512 of raw payload using stored secret key
Compare computed signature with received signature (constant-time comparison)
Reject if mismatch

2. Idempotency Handling
   Providers retry webhooks if they don't receive HTTP 200. This service tracks processed event IDs to prevent duplicate
   processing.
   Idempotency Table: processed_webhooks
   Unique constraint on (provider, event_id)
   Check before processing → skip if already processed
3. Raw Payload Persistence
   All webhook payloads are persisted to database before any processing:
   Immutable audit trail
   Debugging capability
   Compliance with PCI DSS
   Replay capability for failed events
   🗄️ Database Schema
   Table: webhook_events

```sql
CREATE TABLE webhook_events
(
    id                 UUID PRIMARY KEY,
    provider           VARCHAR(50) NOT NULL,
    event_type         VARCHAR(50) NOT NULL,
    external_reference VARCHAR(255),
    raw_payload        TEXT        NOT NULL,
    signature_header   VARCHAR(500),
    status             VARCHAR(20) DEFAULT 'PENDING',
    failure_reason     TEXT,
    received_at        TIMESTAMP   DEFAULT NOW(),
    processed_at       TIMESTAMP,

    INDEX              idx_provider (provider),
    INDEX              idx_status (status),
    INDEX              idx_external_ref (external_reference),
    INDEX              idx_received_at (received_at)
);
```

Status Values:
PENDING - Received, not yet processed
PROCESSING - Being processed by Kafka consumer
PROCESSED - Successfully processed
FAILED - Processing failed (check failure_reason)
INVALID_SIGNATURE - Signature validation failed
Table: processed_webhooks

```sql
CREATE TABLE processed_webhooks
(
    id           UUID PRIMARY KEY,
    provider     VARCHAR(50)  NOT NULL,
    event_id     VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP DEFAULT NOW(),

    UNIQUE KEY uk_provider_event (provider, event_id),
    INDEX        idx_event_id (event_id)
);
```





