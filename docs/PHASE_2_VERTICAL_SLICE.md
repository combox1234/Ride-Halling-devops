# Phase 2: Early Vertical Slice & Security Architecture Guide

This document details the architecture, implemented endpoints, security controls, and verification steps for the Phase 2 early vertical slice of the Ride-Hailing DevOps platform.

---

## 1. Overview & Service Boundaries

The Phase 2 vertical slice implements the core end-to-end trip journey across three microservices using **Java 21** and **Spring Boot 3.3.5**:

| Service | Port | Key Responsibilities |
| :--- | :---: | :--- |
| **`auth-service`** | `8081` | User registration (BCrypt password hashing), credentials authentication, signed HMAC-SHA256 JWT issuance & validation. Rejects fallback to development secrets in production profiles. |
| **`passenger-service`** | `8082` | Passenger profile management, cryptographic Bearer token authentication, ownership checks (IDOR prevention), forged identity header verification. |
| **`ride-service`** | `8084` | Core Ride Lifecycle Finite State Machine (`REQUESTED` -> `DRIVER_ASSIGNED` -> `IN_PROGRESS` -> `COMPLETED`), mock driver dispatch, atomic state transitions (`computeIfPresent`), and idempotent completion. |

All services communicate through the shared model library [services/common-dto](file:///F:/devprod/ride-hailing-devops-clean/services/common-dto).

---

## 2. Security Architecture & Interim Trust Boundary

### 2.1. Trust Boundary Documentation
- **Current State (Phase 2):** In this early vertical slice, the API Gateway (`api-gateway`) is not yet implemented. Therefore, `passenger-service` **does not assume** that incoming traffic has passed through an authenticated gateway or that an internal network perimeter exists.
- **Cryptographic Identity Verification:** Every request to `passenger-service` must supply an `Authorization: Bearer <token>` header signed by the platform's HMAC-SHA256 secret.
- **No Reliance on Unauthenticated `X-User-Id`:** A client cannot access or create resources by merely supplying an `X-User-Id` header. If `X-User-Id` is provided alongside a valid Bearer token, the service strictly asserts that `Long.parseLong(xUserId) == token.userId()`. Any mismatch is rejected with `HTTP 403 Forbidden` ("Provided X-User-Id does not match authenticated token identity").
- **Ownership Checks (IDOR Prevention):**
  - `GET /api/v1/passengers/{id}`: Verifies `profile.userId() == authenticatedUser.userId()` (or `ROLE_ADMIN`). Unauthorized requests return `HTTP 403 Forbidden` without leaking profile data.
  - `PUT /api/v1/passengers/{id}`: Verifies `profile.userId() == authenticatedUser.userId()` (or `ROLE_ADMIN`). Unauthorized modifications return `HTTP 403 Forbidden`.
  - `POST /api/v1/passengers`: Asserts that `request.userId() == authenticatedUser.userId()` (or `ROLE_ADMIN`). Callers cannot register profiles under other user IDs.

### 2.2. JWT Configuration & Production Guardrails
- **Externalization:** The secret is externalized via `${JWT_SECRET}`.
- **Production Fail-Fast:** When active Spring profiles include `prod` or `production`:
  - A missing or blank `JWT_SECRET` immediately halts startup with `IllegalStateException`.
  - Using the known default development key (`404E6352...`) halts startup with `IllegalStateException`.
  - A secret shorter than 32 characters halts startup with `IllegalStateException`.

### 2.3. Sanitized Error Handling
- Unhandled `Exception` instances (HTTP 500) return a generic client-facing message: `"An unexpected internal error occurred. Please contact support."`
- Raw exception messages, SQL traces, and internal stack frames are logged server-side via SLF4J and are never exposed to HTTP clients.

### 2.4. Atomic State Transitions & Idempotency
- **Atomic Concurrency:** `RideRepository.updateAtomically(id, updater)` leverages `ConcurrentHashMap.computeIfPresent(...)` to guarantee that state transitions are evaluated and committed under the bucket lock. Simultaneous racing requests (e.g. driver starting while rider cancels) execute sequentially; exactly one wins and the loser receives `InvalidStateTransitionException` (HTTP 400).
- **Idempotent Ride Completion:** Repeated `POST /api/v1/rides/{id}/complete` calls on an already `COMPLETED` ride return the existing completed `RideResponse` (HTTP 200) without modifying timestamps, fares, or generating duplicate billing idempotency keys.

### 2.5. Public Registration Role Hardening & Admin Provisioning
- **Server-Controlled Role Assignment:** Public self-registration (`POST /api/v1/auth/register`) strictly assigns public roles (`ROLE_PASSENGER` by default, or optionally `ROLE_DRIVER`).
- **Privilege Escalation Prevention:** Self-assignment of `ROLE_ADMIN` via public registration is forbidden. Any request explicitly passing `role: "ROLE_ADMIN"` (or attempting role escalation through extra JSON payload fields) is rejected with `HTTP 400 Bad Request` ("ROLE_ADMIN cannot be self-assigned through public registration").
- **Trusted Administrative Provisioning:** Administrator accounts (`ROLE_ADMIN`) can only be provisioned through a trusted administrative mechanism (`AuthService.provisionAdmin`) used for internal bootstrapping, administrative tooling, or secure internal seeding—never through public-facing endpoints.

---

## 3. Implemented REST Endpoints

### Auth Service (`http://localhost:8081`)
- `POST /api/v1/auth/register`: Register user (`name`, `email`, `password`, optional `role`). Defaults to `ROLE_PASSENGER`. Rejects `ROLE_ADMIN` with `HTTP 400`. Returns JWT token.
- `POST /api/v1/auth/login`: Authenticate with email/password. Returns JWT token.
- `GET /api/v1/auth/validate`: Validates `Authorization: Bearer <token>` header and returns user ID and role.

### Passenger Service (`http://localhost:8082`)
- `POST /api/v1/passengers`: Create a passenger profile (`userId`, `name`, `email`, `phone`). Requires Bearer token matching `userId`.
- `GET /api/v1/passengers/{id}`: Retrieve passenger profile by ID. Enforces ownership check (403 if reading another user's profile).
- `GET /api/v1/passengers/me`: Retrieve caller profile using validated Bearer token.
- `PUT /api/v1/passengers/{id}`: Update passenger contact information. Enforces ownership check.

### Ride Service (`http://localhost:8084`)
- `POST /api/v1/rides/request`: Request a ride (`passengerId`, `pickupLocation`, `dropoffLocation`). Calculates estimated fare and assigns a mock driver (transitions to `DRIVER_ASSIGNED`).
- `POST /api/v1/rides/{id}/start`: Driver begins trip (transitions to `IN_PROGRESS`).
- `POST /api/v1/rides/{id}/complete`: Completes trip (transitions to `COMPLETED`), computes final fare, and generates payment idempotency key (`PAY-RIDE-<id>`). Idempotent on retries.
- `POST /api/v1/rides/{id}/cancel`: Cancels ride. Throws `InvalidStateTransitionException` (HTTP 400) if trip is already `IN_PROGRESS` or `COMPLETED`.
- `GET /api/v1/rides/{id}`: Retrieves current trip status and details.

---

## 4. Automated Test Verification

Run the full verification build across all modules:
```powershell
.\mvnw.cmd clean verify
```

Run targeted tests for Phase 2 services:
```powershell
.\mvnw.cmd test -pl services/auth-service,services/passenger-service,services/ride-service
```

---

## 5. Manual / Local Verification Walkthrough (PowerShell)

### Step 1: Start Services (3 separate windows)
```powershell
# Window 1: Auth Service (Port 8081)
.\mvnw.cmd spring-boot:run -pl services/auth-service

# Window 2: Passenger Service (Port 8082)
.\mvnw.cmd spring-boot:run -pl services/passenger-service

# Window 3: Ride Service (Port 8084)
.\mvnw.cmd spring-boot:run -pl services/ride-service
```

### Step 2: Register & Acquire Bearer Token
```powershell
$authRes = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/register" -Method Post -ContentType "application/json" -Body (@{
    name = "Alice Rider"
    email = "alice@example.com"
    password = "password123"
    role = "ROLE_PASSENGER"
} | ConvertTo-Json)

$token = "Bearer " + $authRes.data.token
$userId = $authRes.data.userId
```

### Step 3: Create & Access Profile with Authentication
```powershell
# Create Profile (Bearer token required)
$profile = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/passengers" -Method Post -ContentType "application/json" -Headers @{
    Authorization = $token
} -Body (@{
    userId = $userId
    name = "Alice Rider"
    email = "alice@example.com"
    phone = "+1-555-0199"
} | ConvertTo-Json)

# Read Own Profile via /me
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/passengers/me" -Method Get -Headers @{
    Authorization = $token
}

# Verify Ownership Enforced: reading with no token returns 401
try {
    Invoke-RestMethod -Uri "http://localhost:8082/api/v1/passengers/$($profile.data.id)" -Method Get
} catch {
    $_.Exception.Response.StatusCode # 401 Unauthorized
}
```

### Step 4: Book, Progress, and Idempotently Complete Ride
```powershell
# Book Ride
$ride = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/rides/request" -Method Post -ContentType "application/json" -Body (@{
    passengerId = $profile.data.id
    pickupLocation = @{
        latitude = 37.7749
        longitude = -122.4194
        address = "Market St, San Francisco"
    }
    dropoffLocation = @{
        latitude = 37.7892
        longitude = -122.4014
        address = "Financial District, San Francisco"
    }
} | ConvertTo-Json)

$rideId = $ride.data.id

# Start Ride
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/rides/$rideId/start" -Method Post

# Complete Ride (First Call)
$comp1 = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/rides/$rideId/complete" -Method Post

# Complete Ride (Retry Call - Idempotent, same response)
$comp2 = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/rides/$rideId/complete" -Method Post

Write-Host "Idempotency Key 1: $($comp1.data.idempotencyKey)"
Write-Host "Idempotency Key 2: $($comp2.data.idempotencyKey)"
```
