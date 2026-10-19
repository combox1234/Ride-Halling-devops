# Phase 5: Asynchronous Messaging & Choreography with RabbitMQ

## 1. Overview & Objectives
Phase 5 implements asynchronous event-driven choreography across the microservices platform using Spring AMQP (RabbitMQ). Instead of tight synchronous coupling between ride state transitions and downstream processes (such as proximity driver matching, billing, and passenger/driver notifications), domain events are published to a central topic exchange and consumed asynchronously by decoupled subscribers.

This milestone also introduces `notification-service` (listening on port 8088) to receive domain events and manage notifications for passengers and drivers.

---

## 2. RabbitMQ Topology & Architecture

### Exchanges
| Exchange Name | Type | Durability | Description |
|---|---|---|---|
| `ride.topic.exchange` | Topic | Durable | Primary domain event exchange for all ride lifecycle events |
| `billing.direct.exchange` | Direct | Durable | Dedicated direct exchange for billing and payment settlement |
| `dlx.topic.exchange` | Topic | Durable | Dead-Letter Exchange (DLX) capturing failed or corrupt messages |

### Queues & Dead-Letter Routing
All operational queues configure `x-dead-letter-exchange: dlx.topic.exchange` and `x-dead-letter-routing-key: dlx.failed` so that unhandled or rejected messages route automatically to the Dead-Letter Queue.

| Queue Name | Bound Exchange | Routing Key Pattern | Dead-Letter Configuration |
|---|---|---|---|
| `q.ride.matching` | `ride.topic.exchange` | `ride.requested` | `x-dead-letter-exchange: dlx.topic.exchange`<br>`x-dead-letter-routing-key: dlx.failed` |
| `q.ride.notification` | `ride.topic.exchange` | `ride.*` (all ride events) | `x-dead-letter-exchange: dlx.topic.exchange`<br>`x-dead-letter-routing-key: dlx.failed` |
| `q.ride.billing` | `billing.direct.exchange` & `ride.topic.exchange` | `ride.completed` | `x-dead-letter-exchange: dlx.topic.exchange`<br>`x-dead-letter-routing-key: dlx.failed` |
| `q.dead-letter` | `dlx.topic.exchange` | `dlx.#` | Terminal queue for inspection & telemetry alerting |

---

## 3. Resilience, Retry, and Dead-Letter Handling

### Retry Policy
Every message consumer configures a `RetryOperationsInterceptor` with:
- **Maximum attempts**: 3
- **Initial interval**: 1000 ms (1 second)
- **Multiplier**: 2.0 (exponential backoff: 1s, 2s, 4s...)
- **Max interval**: 10000 ms (10 seconds)

### Dead-Letter Recovery (`RepublishMessageRecoverer`)
When all 3 consumer delivery attempts fail due to unhandled exceptions:
1. `RepublishMessageRecoverer` intercepts the message before rejection.
2. It enriches the message headers with diagnostic telemetry:
   - `x-exception-message`
   - `x-exception-stacktrace`
   - `x-original-exchange`
   - `x-original-routingKey`
3. It republishes the message to `dlx.topic.exchange` with routing key `dlx.failed`, which routes directly into `q.dead-letter`.

### Offline Broker Graceful Fallback
`RideEventPublisher` and `MatchingEventPublisher` catch `org.springframework.amqp.AmqpException` (e.g. connection refused when RabbitMQ broker is offline) and log a warning without throwing an unhandled 500 error on the initiating HTTP thread.

Listener containers default to `auto-startup: ${RABBITMQ_LISTENER_AUTO_STARTUP:false}` during testing to maintain 100% reproducible test suites when external brokers are offline.

---

## 4. Shared Event Contracts (`common-dto`)

All domain events are wrapped in a generic standard envelope (`EventEnvelope<T>`):
```json
{
  "eventId": "3c914bf1-0814-4fc6-b51e-e248f2195f01",
  "eventType": "RideRequestedEvent",
  "timestamp": "2026-10-20T01:30:00.000Z",
  "traceId": "trace-uuid-1234",
  "payload": { ... }
}
```

### Event Payloads
- `RideRequestedEvent`: `rideId`, `passengerId`, `pickupLocation`, `dropoffLocation`, `estimatedFare`, `requestedAt`
- `RideMatchedEvent`: `rideId`, `driverId`, `passengerId`, `matchedAt`
- `RideStartedEvent`: `rideId`, `driverId`, `passengerId`, `startedAt`
- `RideCompletedEvent`: `rideId`, `passengerId`, `driverId`, `finalFare`, `idempotencyKey`, `completedAt`
- `RideCancelledEvent`: `rideId`, `passengerId`, `driverId`, `reason`, `cancelledAt`

---

## 5. Notification Service (`notification-service`)

### Configuration
- **Port**: 8088
- **Subscription**: `q.ride.notification` listening to `ride.*` topic pattern

### REST Endpoints
- `GET /api/v1/notifications/rides/{rideId}`: Returns all notifications generated for a specific ride.
- `GET /api/v1/notifications`: Returns all notifications.
- `GET /api/v1/notifications/{id}`: Returns a notification by its unique ID.

---

## 6. How to Run & Verify

### Run Targeted Tests (Phase 5 Messaging Modules)
```powershell
.\mvnw.cmd test -pl services/common-dto,services/ride-service,services/matching-service,services/notification-service
```

### Run Full Multi-Module Verification
```powershell
.\mvnw.cmd clean verify
```

---

## 7. Infrastructure Status & Verification Scope Note
- **Tested & Verified**: All queue declarations, exchange bindings, DLQ routing arguments, JSON serialization, consumer retry interceptors (3 attempts with exponential backoff), recoverers (`RepublishMessageRecoverer`), and REST APIs pass automated unit and integration tests cleanly with 0 failures, 0 errors, and 0 skips.
- **Real Infrastructure Disclaimer**: Verification was conducted against the Spring AMQP / Mockito test infrastructure without requiring a live RabbitMQ broker daemon on `localhost:5672`. As noted in pre-flight checks, port 5672 was offline during testing; live broker end-to-end integration is slated for multi-container topology testing (Phase 8 Docker Compose).
