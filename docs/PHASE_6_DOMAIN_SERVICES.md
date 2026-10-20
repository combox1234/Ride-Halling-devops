# Phase 6: Pricing, Idempotent Billing, Rating, and Reporting Services

## 1. Overview & Objectives

Phase 6 expands the **Ride-Hailing DevOps & Microservices Platform** by implementing the four core financial and analytical domain microservices:
1. **Pricing Service (`services/pricing-service`, Port 8086)**: Dynamic fare estimation, base fare plus distance and duration calculation, minimum fare guarantees, and zone-based surge multipliers.
2. **Billing Service (`services/billing-service`, Port 8087)**: High-concurrency, strictly idempotent payment transactions via unique tokens (`idempotency_key`), safe simulated payment provider integration, immutable invoice ledgers, and event publication.
3. **Rating Service (`services/rating-service`, Port 8089)**: Post-trip star reviews (1–5 scale), feedback comments, duplicate rating prevention per trip, and rolling average reputation scoring.
4. **Reporting Service (`services/reporting-service`, Port 8090)**: Read-only aggregated reporting data mart subscribing to wildcard domain events (`ride.*`, `billing.*`), deduplicating events by `eventId`, and maintaining real-time daily metrics and driver earnings summaries.

---

## 2. Microservice Architecture & Port Allocations

| Service | Port | Relational Store | Message Broker Integrations | Key Responsibilities |
|---|:---:|---|---|---|
| **`pricing-service`** | `8086` | MySQL `pricing_db` | N/A (Synchronous REST query) | Zone rate cards, dynamic surge multipliers, distance/time fare calculations |
| **`billing-service`** | `8087` | MySQL `billing_db` | Consumes `q.ride.billing`<br>Publishes to `billing.direct.exchange` | Idempotent payment token handling, mock payment provider, invoice ledgers |
| **`rating-service`** | `8089` | MySQL `rating_db` | N/A (Event-correlated via `ride_id`) | Post-trip rider/driver reviews, star distribution, rolling average ratings |
| **`reporting-service`** | `8090` | MySQL `reporting_db` | Consumes `q.ride.reporting` & `q.billing.reporting` | Event audit ledger, daily KPI data mart, driver earnings aggregation |

---

## 3. Database Persistence & Flyway Migrations

Each service manages its schema independently through Flyway migrations under `src/main/resources/db/migration/V1__init.sql`.

### 3.1. Pricing Schema (`pricing_db`)
- `pricing_zones`: `zone_code` (UNIQUE), `zone_name`, `base_fare`, `per_km_rate`, `per_minute_rate`, `surge_multiplier`, `min_fare`, `active`. Seeded with `DEFAULT`, `DOWNTOWN`, `AIRPORT`, and `SUBURBAN` zones.

### 3.2. Billing Schema (`billing_db`)
- `payment_transactions`: `idempotency_key` (UNIQUE `uk_payment_idempotency_key`), `ride_id`, `passenger_id`, `amount`, `status`, `payment_method`, `transaction_ref`, `error_code`, `error_message`.
- `invoices`: `invoice_number` (UNIQUE `uk_invoices_number`), `payment_transaction_id` (FK), `ride_id` (UNIQUE `uk_invoices_ride`), `passenger_id`, `amount`, `currency`, `issued_at`.

### 3.3. Rating Schema (`rating_db`)
- `ratings`: `ride_id`, `rater_id`, `rater_type`, `ratee_id`, `score` (1 to 5), `feedback`. Enforces `UNIQUE(ride_id, rater_id)` to prevent double reviews for a single trip.

### 3.4. Reporting Schema (`reporting_db`)
- `reporting_events`: `event_id` (UNIQUE `uk_reporting_event_id`), `event_type`, `aggregate_id`, `trace_id`, `payload_json`, `occurred_at`, `recorded_at`.
- `daily_metrics`: `report_date` (UNIQUE `uk_daily_metrics_date`), `total_rides_requested`, `total_rides_started`, `total_rides_completed`, `total_rides_cancelled`, `total_revenue`.
- `driver_daily_performance`: `driver_id`, `report_date` (UNIQUE `uk_driver_performance_date`), `completed_trips`, `total_earnings`.

---

## 4. Idempotency & Concurrency Guarantees

### 4.1. Payment Idempotency Engine
To prevent double-charging passengers during network retries, AMQP redelivery, or concurrent client submissions:
1. Every payment request carries an `idempotencyKey` (e.g., `PAY-RIDE-<rideId>`).
2. The service checks `payment_transactions` for the key. If an existing `SUCCESS` record exists, the service returns the existing transaction and cached invoice without calling the payment gateway.
3. If new, the transaction is inserted with a database-level `UNIQUE(idempotency_key)` constraint. In high-concurrency race conditions where two threads attempt insertion simultaneously, the loser catches `DataIntegrityViolationException` and safely retrieves the winning record.
4. The payment gateway simulation never stores credit cards, CVVs, or credentials. It produces deterministic simulated transaction references (`TXN-SIM-<UUID>`).

### 4.2. Reporting Event Deduplication
1. Incoming domain events (`EventEnvelope<?>`) contain a globally unique `eventId`.
2. `ReportingService` verifies `existsByEventId(eventId)`.
3. If the event was previously recorded, it is discarded immediately without mutating daily counters or driver earnings.

---

## 5. API Reference

### Pricing Service (`:8086`)
- `POST /api/v1/pricing/estimate`: Estimates trip fare given pickup/dropoff coordinates and optional zone.
- `GET /api/v1/pricing/surge-zone/{zoneCode}`: Fetches zone configuration and surge multiplier.
- `GET /api/v1/pricing/zones`: Returns all active pricing zones.
- `PUT /api/v1/pricing/surge-zone/{zoneCode}/multiplier`: Adjusts surge multiplier (validated between 1.00 and 5.00).

### Billing Service (`:8087`)
- `POST /api/v1/billing/payments`: Idempotently initiates or retrieves a payment transaction.
- `GET /api/v1/billing/payments/{idempotencyKey}`: Retrieves payment transaction by idempotency key.
- `GET /api/v1/billing/rides/{rideId}`: Retrieves payment transaction by ride ID.
- `GET /api/v1/billing/invoices/{rideId}`: Retrieves invoice for a completed trip.

### Rating Service (`:8089`)
- `POST /api/v1/ratings`: Submits a passenger or driver review (score 1–5).
- `GET /api/v1/ratings/drivers/{driverId}/summary`: Rolling average rating, total count, and 1–5 star breakdown.
- `GET /api/v1/ratings/passengers/{passengerId}/summary`: Rolling average rating and breakdown for passengers.
- `GET /api/v1/ratings/rides/{rideId}`: All reviews submitted for a specific ride.

### Reporting Service (`:8090`)
- `GET /api/v1/reports/daily-summary?date=YYYY-MM-DD`: Platform KPIs for a given date.
- `GET /api/v1/reports/driver-performance/{driverId}?date=YYYY-MM-DD`: Completed trips and earnings for a driver.
- `GET /api/v1/reports/driver-performance/{driverId}/history`: Historic daily earnings breakdown for a driver.
- `GET /api/v1/reports/overview`: Platform-wide aggregate totals (rides requested, completed, cancelled, total revenue).
- `GET /api/v1/reports/events`: Audit trail of recently ingested domain events.

---

## 6. Verification Commands

Run targeted unit and integration tests across Phase 6 services:
```powershell
.\mvnw.cmd test -pl services/pricing-service,services/billing-service,services/rating-service,services/reporting-service
```
