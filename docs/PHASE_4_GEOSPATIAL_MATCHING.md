# Phase 4: Geospatial Driver Tracking & Proximity Matching Guide

This document details the geospatial tracking, driver lifecycle management, Redis proximity matching, and dispatch integration implemented in Phase 4 of the **Ride-Hailing DevOps & Microservices Platform**.

---

## 1. Architecture & Service Boundaries

Phase 4 introduces two core services dedicated to driver supply and ride dispatch:

| Service | Port | Relational Store | Fast In-Memory Store | Primary Responsibilities |
| :--- | :---: | :--- | :--- | :--- |
| **`driver-service`** | `8083` | MySQL `driver_db` (`drivers`) | Redis (`drivers:available`, `driver:heartbeat:*`) | Driver onboarding, vehicle registration, availability lifecycle (`OFFLINE`, `AVAILABLE`, `ON_TRIP`), high-frequency location ping ingestion. |
| **`matching-service`** | `8085` | N/A (Stateless candidate ranking) | Redis (`drivers:available`, `driver:heartbeat:*`, `driver:lock:*`) | Proximity-based candidate discovery (`GEOSEARCH`), companion heartbeat verification, stale driver eviction, atomic reservation locks, and dispatch. |

---

## 2. Driver Relational Persistence (`driver_db`)

The `driver-service` maintains relational driver profile records using Flyway migrations (`src/main/resources/db/migration/V1__init.sql`):

```sql
CREATE TABLE IF NOT EXISTS drivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    vehicle_make VARCHAR(50) NOT NULL,
    vehicle_model VARCHAR(50) NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    rating DOUBLE NOT NULL DEFAULT 5.0,
    total_trips INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drivers_user_id UNIQUE (user_id),
    CONSTRAINT uk_drivers_email UNIQUE (email),
    CONSTRAINT uk_drivers_license_plate UNIQUE (license_plate)
);
```

### Driver Lifecycle States
- **`OFFLINE`**: Default initial state. The driver is not taking requests and is excluded from Redis.
- **`AVAILABLE`**: Driver is online and broadcasting location. Broadcasts are recorded in Redis geospatial set with companion heartbeats.
- **`ON_TRIP`**: Driver has accepted a ride and is actively fulfilling a trip. Driver is evicted from `drivers:available` to prevent concurrent assignments.

---

## 3. Redis Geospatial Key Schema & Matching Algorithm

### 3.1. Key Structures
1. **Available Drivers Geospatial Set (`drivers:available`)**
   - Type: Redis Geospatial Set (backed by Sorted Set `ZSET`).
   - Command: `GEOADD drivers:available <longitude> <latitude> <driver_id>`
   - Longitude is X coordinate, Latitude is Y coordinate.
2. **Availability Heartbeat Key (`driver:heartbeat:<driver_id>`)**
   - Type: String with 60-second TTL.
   - Command: `SET driver:heartbeat:<driver_id> "AVAILABLE" EX 60`
   - Purpose: Detects network drops, crashes, or ungraceful app exits.
3. **Atomic Driver Reservation Lock (`driver:lock:<driver_id>`)**
   - Type: String with 30-second TTL.
   - Command: `SET driver:lock:<driver_id> <ride_id> NX EX 30`
   - Purpose: Prevents concurrent double-booking of a single driver during simultaneous ride requests.

### 3.2. Proximity Matching Algorithm
When a ride is requested at pickup coordinates `(lat, lon)`:
1. **Geospatial Proximity Search**:
   ```redis
   GEOSEARCH drivers:available FROMLONLAT <lon> <lat> BYRADIUS 5 km ASC WITHDIST WITHCOORD COUNT 10
   ```
2. **Heartbeat & Stale Eviction Check**:
   - For each candidate returned, check if `driver:heartbeat:<driver_id>` exists.
   - If the heartbeat key has expired, the driver is considered stale. The system automatically evicts the driver (`ZREM drivers:available <driver_id>`) and discards them from candidates.
3. **Proximity & Rating Ranking**:
   - Non-stale candidates are sorted by proximity `ASC` (nearest first).
   - Tie-breaks utilize driver rating `DESC`.
4. **Concurrency Reservation Lock**:
   - The engine attempts `SET driver:lock:<driver_id> <ride_id> NX EX 30`.
   - If acquired, the driver is assigned. If the lock is already held by another matching request, the algorithm safely skips to the next nearest candidate.

---

## 4. API Endpoints

### 4.1. Driver Service (`:8083`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/drivers` | Register driver profile | Bearer JWT (`ROLE_DRIVER` or `ROLE_ADMIN`) |
| `GET` | `/api/v1/drivers/{id}` | Retrieve driver profile | Bearer JWT (Self or `ROLE_ADMIN`) |
| `GET` | `/api/v1/drivers/user/{userId}` | Retrieve profile by user ID | Bearer JWT (Self or `ROLE_ADMIN`) |
| `PUT` | `/api/v1/drivers/{id}/status` | Update driver availability status | Bearer JWT (Self or `ROLE_ADMIN`) |
| `POST` | `/api/v1/drivers/location` | High-frequency GPS ping | Bearer JWT (Self or `ROLE_ADMIN`) |

### 4.2. Matching Service (`:8085`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/matching/find-driver` | Finds and reserves the best candidate within radius |
| `GET` | `/api/v1/matching/nearby-drivers` | Queries active non-stale drivers within radius |

---

## 5. Security & Reliability Controls

1. **Authentication & IDOR Prevention**:
   - Requests require valid HMAC-SHA256 Bearer JWTs validated via `JwtTokenValidator`.
   - Drivers can only register, view, update status, and ping location for their own user ID.
   - Arbitrary `X-User-Id` spoofing is rejected without a cryptographic token.
2. **Fail-Fast Secret Validation**:
   - Production startup checks reject empty, short (<32 chars), or default development secrets.
3. **Coordinate Validation**:
   - Latitudes must fall between -90.0 and 90.0; Longitudes must fall between -180.0 and 180.0.
4. **Resilient Data Store Handling**:
   - If Redis is temporarily unreachable, requests fail safely with HTTP `503 Service Unavailable` instead of exposing internal connection exceptions or stack traces.
5. **No Duplicate Driver Assignment**:
   - Atomic reservation locks (`driver:lock:<id>`) guarantee mutual exclusion under concurrent ride booking requests.

---

## 6. Verification & Automated Test Commands

```powershell
# Run Driver Service tests
.\mvnw.cmd test -pl services/driver-service

# Run Matching Service tests
.\mvnw.cmd test -pl services/matching-service

# Run Ride Service dispatch integration tests
.\mvnw.cmd test -pl services/ride-service

# Full platform verification
.\mvnw.cmd clean verify
```
