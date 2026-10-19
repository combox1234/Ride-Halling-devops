# Phase 3: Relational Persistence, Schema Migrations & Concurrency Guide

This document details the relational persistence architecture, Flyway schema migrations, Spring Data JPA repositories, concurrency control, and testing strategies implemented in Phase 3 of the **Ride-Hailing DevOps & Microservices Platform**.

---

## 1. Architecture Overview (Database-per-Service)

Following cloud-native microservices principles, each service owns its private database schema and never directly accesses the database of another service:

| Service | Database Schema | Port | Primary Entities | Key Relational Features |
| :--- | :--- | :---: | :--- | :--- |
| **`auth-service`** | `auth_db` | `8081` | `UserEntity` (`users`) | Unique index on `email`, BCrypt hashed passwords, role assignment, Flyway versioning. |
| **`passenger-service`** | `passenger_db` | `8082` | `PassengerEntity` (`passengers`) | Unique index on `user_id` and `email`, profile ratings, ride counters. |
| **`ride-service`** | `ride_db` | `8084` | `RideEntity` (`rides`) | State machine FSM, Optimistic Locking (`@Version`), unique `idempotency_key`, indexed `passenger_id` and `status`. |

---

## 2. Flyway Schema Migrations

Each service defines versioned Flyway SQL migration scripts under `src/main/resources/db/migration/V1__init.sql`.

### 2.1. `auth-service` (`db/migration/V1__init.sql`)
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users(role);
```

### 2.2. `passenger-service` (`db/migration/V1__init.sql`)
```sql
CREATE TABLE IF NOT EXISTS passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    rating DOUBLE NOT NULL DEFAULT 5.0,
    total_rides INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_passengers_user_id UNIQUE (user_id),
    CONSTRAINT uk_passengers_email UNIQUE (email)
);
```

### 2.3. `ride-service` (`db/migration/V1__init.sql`)
```sql
CREATE TABLE IF NOT EXISTS rides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_latitude DOUBLE NOT NULL,
    pickup_longitude DOUBLE NOT NULL,
    pickup_address VARCHAR(255),
    dropoff_latitude DOUBLE NOT NULL,
    dropoff_longitude DOUBLE NOT NULL,
    dropoff_address VARCHAR(255),
    estimated_fare DECIMAL(10, 2) NOT NULL,
    final_fare DECIMAL(10, 2),
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_rides_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_rides_passenger_id ON rides(passenger_id);
CREATE INDEX idx_rides_status ON rides(status);
```

---

## 3. Database Concurrency & Idempotency Controls

### 3.1. Database-Level Optimistic Locking (`@Version`)
- `RideEntity` incorporates JPA optimistic locking via `@Version private Long version = 0L;`.
- When concurrent transactions attempt to alter the same ride simultaneously (e.g. driver starting while passenger attempts cancellation), the second committing transaction encounters a stale version and raises an `OptimisticLockingFailureException`. This ensures atomic state machine transitions at the database row level rather than relying on in-memory maps.

### 3.2. Idempotent Ride Completion
- `completeRide` checks if the ride is already in `COMPLETED` status. If so, it returns the existing `RideResponse` without altering timestamps or regenerating billing idempotency keys.
- At the database schema level, `CONSTRAINT uk_rides_idempotency_key UNIQUE (idempotency_key)` guarantees that duplicate billing keys cannot be persisted.

### 3.3. Transaction Boundaries
- All mutation operations in `AuthService`, `PassengerService`, and `RideService` are guarded with Spring's `@Transactional`. Multi-step state transitions execute within an atomic transaction boundary.

---

## 4. Configuration & Environment Variables

All database connection parameters are fully externalized without hardcoded credentials.

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | Auto-constructed from `DB_HOST`, `DB_PORT`, `DB_NAME` | Full JDBC connection URL override |
| `DB_HOST` | `localhost` | Database host name or container network alias |
| `DB_PORT` | `3306` | MySQL server port |
| `DB_NAME` | `auth_db`, `passenger_db`, or `ride_db` | Target database name per service |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `""` (empty) | Database password |

### 4.1. Local MySQL Setup (Docker or Native)
To create the databases in a local MySQL instance:
```sql
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS passenger_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ride_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

When running services with MySQL:
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="yourpassword"
.\mvnw.cmd spring-boot:run -pl services/auth-service
```

### 4.2. Automated Testing Environment
Automated tests execute against in-memory H2 in MySQL compatibility mode (`MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`):
- Flyway migrations parse and apply DDL to the schema.
- Hibernate validates the JPA entity mappings against Flyway DDL (`ddl-auto: validate`).
- Tests do not require Docker or an active MySQL network daemon, ensuring 100% reproducible execution in CI/CD environments.

---

## 5. Automated Verification

Run full Maven verification across all modules:
```powershell
.\mvnw.cmd clean verify
```

Run persistence and repository tests specifically:
```powershell
.\mvnw.cmd test -pl services/auth-service,services/passenger-service,services/ride-service
```
