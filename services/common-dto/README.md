# Common DTO & Domain Events Module (`services/common-dto`)

The `common-dto` module is a lightweight Java 21 library containing shared immutable records, domain event models, API response envelopes, and domain exception classes used across all microservices in the ride-hailing platform.

## Contents

- **Domain Event Envelope:**
  - `EventEnvelope<T>`: Standardized envelope containing `eventId`, `eventType`, `timestamp`, `traceId`, and generic `payload`.
- **Domain Events:**
  - `RideRequestedEvent`: Published upon passenger booking request.
  - `RideMatchedEvent`: Published when candidate driver is selected.
  - `RideStartedEvent`: Published when trip begins.
  - `RideCompletedEvent`: Published upon trip completion, containing the `idempotencyKey` required for payment.
  - `PaymentProcessedEvent`: Emitted after billing service executes idempotent charge transaction.
- **DTOs and Enums:**
  - `LocationDto`: GPS coordinates (`latitude`, `longitude`) with validation and optional address.
  - `RideStatus`: State enum modeling the ride lifecycle state machine (`REQUESTED`, `PRICE_ESTIMATED`, `SEARCHING`, `DRIVER_ASSIGNED`, `DRIVER_ARRIVED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).
  - `PaymentStatus`: Payment state enum (`PENDING`, `SUCCESS`, `FAILED`).
  - `ApiResponse<T>`: Standardized REST response wrapper.
  - `ApiErrorResponse`: Standardized REST error payload.
- **Exceptions:**
  - `DomainException`: Base domain exception.
  - `InvalidStateTransitionException`: Thrown on illegal FSM state transitions.

## Verification & Build Instructions

### 1. Verify Java 21 and Maven Wrapper
```powershell
# Verify Java runtime (Java 21 required)
java -version

# Verify Maven Wrapper
.\mvnw.cmd -version
```

### 2. Compile and Test common-dto
```powershell
# Compile common-dto module
.\mvnw.cmd clean compile -pl services/common-dto

# Run unit tests for common-dto
.\mvnw.cmd clean test -pl services/common-dto

# Run full project build and verification from root
.\mvnw.cmd clean verify
```
