# Ride-Hailing DevOps & Microservices Platform

A production-grade, cloud-native microservices ride-hailing platform built with **Java 21**, **Spring Boot 3.x**, and a complete local-to-cloud DevOps automation suite.

---

## 🛠️ Technology Stack & Core Decisions

| Domain | Technology Decision | Key Rationale |
| :--- | :--- | :--- |
| **Runtime & Language** | **Java 21 (LTS)** | Virtual Threads (Project Loom), pattern matching, modern JVM performance |
| **Framework** | **Spring Boot 3.x** | Modern native observability, Jakarta EE 10, Spring Cloud integration |
| **Build Tool** | **Maven Multi-Module** | Standardized dependency management and submodule isolation |
| **Relational Storage** | **MySQL 8.x** | ACID transactions per service schema, battle-tested indexing |
| **In-Memory & Cache** | **Redis 7.x** | Geospatial driver location indexing (`GEOADD`/`GEOSEARCH`), token bucket rate limiting |
| **Message Broker** | **RabbitMQ 3.x** | Single broker for transactional ride events, AMQP routing, retries, and dead-letter queues |
| **Containerization** | **Docker & Docker Compose** | Multi-stage Java 21 builds, one-command local topology orchestration |
| **Orchestration** | **Kubernetes** | Local-first deployment (`minikube`/`kind`), followed by cloud targets |
| **CI/CD Platform** | **GitHub Actions** | Automated workflow testing, security scanning, container packaging with Java 21 |
| **Infrastructure as Code** | **Terraform** | Declarative cloud VPC, compute, and database provisioning |
| **Configuration Mgmt** | **Ansible** | Automated host baseline hardening, runtime installation, runner setup |
| **Observability** | **Prometheus + Grafana + Alertmanager** | Metrics scraping, full-stack visualization, proactive alerting, system exporters |

---

## 🧩 Service Boundaries (11 Application Components)

The platform is partitioned into bounded contexts following the Database-per-Service pattern:

1. **API Gateway (`api-gateway`)**: Central reactive entry point (Spring Cloud Gateway), JWT validation, Redis-backed rate limiting, route dispatching.
2. **Auth Service (`auth-service`)**: User authentication, bcrypt credential hashing, role issuance (`PASSENGER`, `DRIVER`, `ADMIN`), and signed JWT tokens.
3. **Passenger Service (`passenger-service`)**: Passenger profile management, saved payment methods, and passenger trip histories (MySQL `passenger_db`).
4. **Driver Service (`driver-service`)**: Driver onboarding, vehicle registration, availability status, and location heartbeats (MySQL `driver_db` + Redis geospatial cache).
5. **Ride Service (`ride-service`)**: Core ride lifecycle state machine (`REQUESTED` -> `COMPLETED`/`CANCELLED`), trip logging (MySQL `ride_db`).
6. **Matching Service (`matching-service`)**: Algorithmic driver discovery via Redis geospatial queries, proximity candidate ranking, and dispatching.
7. **Pricing Service (`pricing-service`)**: Dynamic base fare estimation, distance/time computation, and surge multiplier calculation (MySQL `pricing_db`).
8. **Billing/Payment Service (`billing-service`)**: Invoicing, idempotent payment transactions via unique tokens, receipt ledgers (MySQL `billing_db`).
9. **Notification Service (`notification-service`)**: Event-driven alert dispatching (SMS/Email simulations) and WebSocket real-time trip status push.
10. **Rating Service (`rating-service`)**: Post-trip ratings, driver/passenger feedback reviews, and aggregate reputation scoring (MySQL `rating_db`).
11. **Reporting Service (`reporting-service`)**: Business intelligence, operational metrics, trip summaries, and analytics aggregation (MySQL `reporting_db`).
- **Common Module (`common-dto`)**: Shared Java 21 records, DTOs, domain events, and utilities.

---

## 📊 Observability & Telemetry

Full-stack observability is built-in from the ground up:
- **Application Metrics:** Spring Boot Actuator with Micrometer exposes Prometheus metrics at `/actuator/prometheus`.
- **Infrastructure Exporters:** Dedicated exporters capture MySQL (`mysqld_exporter`), Redis (`redis_exporter`), RabbitMQ (`rabbitmq_exporter`), node metrics (`node_exporter`), and container metrics (`cadvisor`).
- **Live Business Metrics:**
  - `rides_requested_total`: Cumulative counter of ride booking attempts.
  - `rides_completed_total`: Counter of successfully finished rides.
  - `matching_duration_seconds`: Histogram measuring time from request to driver acceptance.
  - `payment_failures_total`: Counter tracking transaction payment exceptions.
  - `active_drivers_count`: Gauge tracking currently online, available drivers.
- **Alertmanager:** Proactive alert routing for elevated error rates (5xx spikes), database connection pool exhaustion, and message queue DLQ accumulation.

---

## 📚 Documentation Index

- [**Project Specification (`docs/PROJECT_SPEC.md`)**](docs/PROJECT_SPEC.md): In-depth service contracts, data models, APIs, RabbitMQ choreography, and NFRs.
- [**System Architecture (`docs/ARCHITECTURE.md`)**](docs/ARCHITECTURE.md): Microservices topology diagram, RabbitMQ exchange/queue routing, Redis geospatial matching, idempotent payment flow, and telemetry architecture.
- [**Implementation Plan (`docs/IMPLEMENTATION_PLAN.md`)**](docs/IMPLEMENTATION_PLAN.md): Step-by-step 14-phase roadmap (Phase 0 through Phase 13), strict acceptance criteria, early vertical slice, exact PowerShell verification commands, and viva preparation deliverables.
- [**Phase 2: Vertical Slice & Security (`docs/PHASE_2_VERTICAL_SLICE.md`)**](docs/PHASE_2_VERTICAL_SLICE.md): Early vertical slice trip journey, cryptographic identity verification, and role hardening.
- [**Phase 3: Relational Persistence & Migrations (`docs/PHASE_3_PERSISTENCE.md`)**](docs/PHASE_3_PERSISTENCE.md): MySQL schemas, Flyway migrations, Spring Data JPA entities, and database concurrency controls.
- [**Phase 4: Geospatial Driver Tracking & Proximity Matching (`docs/PHASE_4_GEOSPATIAL_MATCHING.md`)**](docs/PHASE_4_GEOSPATIAL_MATCHING.md): Redis geospatial index (`GEOADD`/`GEOSEARCH`), companion 60s heartbeats, stale eviction, and atomic dispatch locks.
- [**Phase 5: Asynchronous Messaging & Choreography (`docs/PHASE_5_RABBITMQ_MESSAGING.md`)**](docs/PHASE_5_RABBITMQ_MESSAGING.md): RabbitMQ exchanges, topic routing, dead-letter queue recovery (`RepublishMessageRecoverer`), and decoupled notification dispatch.
- [**Phase 6: Pricing, Idempotent Billing, Rating & Reporting (`docs/PHASE_6_DOMAIN_SERVICES.md`)**](docs/PHASE_6_DOMAIN_SERVICES.md): Dynamic fare calculation, idempotent payment transactions, mock payment provider, post-trip ratings, and operational reporting data marts.
- [**Phase 7: API Gateway & Security Perimeter (`docs/PHASE_7_API_GATEWAY.md`)**](docs/PHASE_7_API_GATEWAY.md): Reactive Spring Cloud Gateway, JWT authentication perimeter, identity header stripping, claim propagation, Redis token-bucket rate limiting, and CORS.

---

## 🏗️ Repository Layout (Target)

```text
ride-hailing-devops-clean/
├── .github/
│   └── workflows/              # GitHub Actions CI/CD pipelines (Java 21)
├── ansible/                    # Host hardening and baseline playbooks
├── terraform/                  # Infrastructure as Code (Local & AWS modules)
│   ├── modules/                # Reusable VPC, K8s, and Database modules
│   └── environments/           # Environment definitions (local, aws)
├── k8s/                        # Kubernetes manifests (deployments, ingress, config)
├── monitoring/                 # Observability stack configuration
│   ├── prometheus/             # Prometheus scraping configurations & alert rules
│   ├── alertmanager/           # Alertmanager routing & receivers
│   └── grafana/                # Pre-built Grafana dashboards & datasources
├── services/                   # Java 21 / Spring Boot 3.x microservices
│   ├── common-dto/             # Shared DTOs, records, and domain events
│   ├── api-gateway/            # Spring Cloud Gateway (port 8080)
│   ├── auth-service/           # Identity & JWT authentication (port 8081)
│   ├── passenger-service/      # Passenger management (port 8082)
│   ├── driver-service/         # Driver profiles & telemetry (port 8083)
│   ├── ride-service/           # Ride lifecycle state machine (port 8084)
│   ├── matching-service/       # Proximity matching engine (port 8085)
│   ├── pricing-service/        # Fare & surge calculation (port 8086)
│   ├── billing-service/        # Idempotent payments & invoicing (port 8087)
│   ├── notification-service/   # Asynchronous alerts & WebSockets (port 8088)
│   ├── rating-service/         # Ratings & review aggregates (port 8089)
│   └── reporting-service/      # Analytics & reporting data marts (port 8090)
├── pom.xml                     # Maven root aggregator POM (Java 21)
└── docs/                       # Specifications, architecture, and runbooks
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK:** OpenJDK 21 or Oracle JDK 21 (LTS)
- **Build Tool:** Apache Maven 3.9+ or project Maven Wrapper (`mvnw.cmd`)
- **Containers:** Docker Desktop / Docker Engine 24+
- **Kubernetes CLI:** `kubectl` with `minikube` or `kind`
- **IaC & Tooling:** `terraform` (v1.5+), `ansible-core` (2.15+)

### Baseline Environment Verification
```powershell
# Verify Java runtime
java -version

# Verify Maven Wrapper (once generated in Phase 1)
.\mvnw.cmd -version

# Full project compile and verification
.\mvnw.cmd clean verify
```

> [!IMPORTANT]
> **Human Approval Gate:** AWS deployment targets are managed under `terraform/environments/aws`. All remote infrastructure provisioning commands (`terraform apply` and `terraform destroy`) require explicit human authorization and are never executed autonomously.
