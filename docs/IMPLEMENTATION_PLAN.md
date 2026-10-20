# Implementation Plan: Ride-Hailing DevOps & Microservices Platform

This document outlines the phased, locally testable implementation plan for engineering the complete ride-hailing microservices ecosystem using **Java 21**, **Spring Boot 3.x**, **MySQL**, **Redis**, **RabbitMQ**, **Docker**, **Kubernetes**, **Prometheus/Grafana/Alertmanager**, **GitHub Actions**, **Terraform**, and **Ansible**.

Every phase specifies strict **Acceptance Criteria** and **Exact Verification Commands** (tested for Windows PowerShell compatibility). An early vertical slice is implemented in Phase 2 to validate end-to-end architectural flow before expanding into all domain services and infrastructure modules.

---

## 🗺️ Implementation Roadmap Summary

| Phase | Title | Focus Area | Deliverables |
| :--- | :--- | :--- | :--- |
| **Phase 0** | Workspace & Planning Baseline | Repository initialization, governance, architecture | Specifications, architecture guides, .gitignore |
| **Phase 1** | Maven Multi-Module & Java 21 Foundation | Parent POM, Maven Wrapper, shared DTOs | Aggregator POM, `services/common-dto`, `mvnw.cmd` |
| **Phase 2** | Early Vertical Slice (Core Journey) | Rapid end-to-end flow validation | Auth, Passenger, and Ride minimal flow with mock dispatch |
| **Phase 3** | Relational Persistence & Migrations | Database-per-service MySQL schemas | Flyway migrations (`V1__init.sql`), JPA entities, Testcontainers |
| **Phase 4** | Geospatial Driver Tracking & Matching | Real-time driver location queries | Redis `GEOADD`/`GEOSEARCH`, Driver & Matching services |
| **Phase 5** | Asynchronous Messaging & Choreography | RabbitMQ event-driven choreography | Topic exchanges, queues, DLQ, retry interceptors |
| **Phase 6** | Pricing, Idempotent Billing & Domain Expansion| Financial ledgers, reviews, analytics | Pricing, Billing (idempotency keys), Rating, Reporting |
| **Phase 7** | API Gateway & Security Perimeter | Edge ingress, authentication, rate limiting | Spring Cloud Gateway, JWT filter, Redis token bucket |
| **Phase 8** | Multi-Stage Containerization & Local Topology | Production container images, Docker Compose | Java 21 Dockerfiles, `docker-compose.yml`, exporters |
| **Phase 9** | Local Kubernetes Orchestration | Declarative cluster deployment | K8s manifests, Ingress, Kustomize overlays, HPA |
| **Phase 10** | Full-Stack Observability & Telemetry | Prometheus, Grafana, Alertmanager | Metrics scraping, business dashboards, alert rules, exporters |
| **Phase 11** | Continuous Integration & Security (CI/CD) | GitHub Actions automation workflows | CI pipeline, security scan, container build workflows |
| **Phase 12** | Infrastructure as Code & Configuration | Terraform modules & Ansible playbooks | Local & AWS Terraform (plan only), Ansible hardening |
| **Phase 13** | Viva Notes, Troubleshooting & Acceptance | Academic deliverables & system validation | Runbooks, viva cheat-sheet, syllabus map, E2E test script |

---

## Phase 0: Workspace & Planning Baseline

### Scope
- Validate isolated workspace directory: `F:\devprod\ride-hailing-devops-clean`.
- Author foundational architecture guides, project specifications, implementation plan, and `.gitignore`.

### Acceptance Criteria
1. Active workspace and git root are strictly `F:\devprod\ride-hailing-devops-clean`.
2. Git status reports no unintended files; `.terraform.lock.hcl` is eligible for git tracking while secrets and build outputs are ignored.
3. No application source code or build artifacts exist prior to planning sign-off.

### Verification Commands (PowerShell)
```powershell
# Type: Configuration / Workspace Check
Get-Location
git rev-parse --show-toplevel
Get-ChildItem -Force
git status --short
```

---

## Phase 1: Maven Multi-Module & Java 21 Foundation

### Scope
- Author root `pom.xml` with:
  - `<java.version>21</java.version>`
  - `<maven.compiler.release>21</maven.compiler.release>`
  - Spring Boot 3.3.x dependency management
  - Spring Cloud 2023.x dependency management
- Generate the official Maven Wrapper (`mvnw` and `mvnw.cmd`) using `mvn wrapper:wrapper`.
- Scaffold `services/common-dto`:
  - Java 21 Records for domain events: `RideRequestedEvent`, `RideMatchedEvent`, `RideCompletedEvent`, `PaymentProcessedEvent`.
  - Common API response envelopes and exception definitions.

### Acceptance Criteria
1. `java -version` returns JDK 21.
2. `.\mvnw.cmd -version` executes successfully using the local Maven Wrapper and references Java 21.
3. `.\mvnw.cmd clean compile` succeeds across root aggregator and `services/common-dto`.

### Verification Commands (PowerShell)
```powershell
# Type: Runtime & Build Tool Verification
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean compile -pl services/common-dto
```

---

## Phase 2: Early Vertical Slice (Core Ride Journey)

### Scope
- Construct a minimal, testable vertical slice proving the core domain flow early:
  - `services/auth-service`: In-memory user credentials, basic JWT generation.
  - `services/passenger-service`: Minimal passenger profile lookup.
  - `services/ride-service`: Initial ride state machine (`REQUESTED` -> `DRIVER_ASSIGNED` -> `IN_PROGRESS` -> `COMPLETED`) with mock driver dispatch.
- Author unit and MockMvc integration tests verifying that a passenger can register, request a ride, have a mock driver assigned, and complete the ride.

### Acceptance Criteria
1. Minimal vertical slice compiles and runs without requiring external database or broker infrastructure.
2. Unit tests verify the ride state machine transitions from `REQUESTED` to `COMPLETED`.
3. Spring MockMvc tests achieve 100% pass rate for the vertical slice endpoints.

### Verification Commands (PowerShell)
```powershell
# Type: Unit & Component Tests
.\mvnw.cmd test -pl services/auth-service,services/passenger-service,services/ride-service
```

---

## Phase 3: Relational Persistence & Schema Migrations

### Scope
- Add Spring Data JPA and MySQL Connector/J to services requiring persistent relational state:
  - `auth-service` (`auth_db`)
  - `passenger-service` (`passenger_db`)
  - `driver-service` (`driver_db`)
  - `ride-service` (`ride_db`)
- Create Flyway database migration scripts (`src/main/resources/db/migration/V1__init.sql`) for each service defining strict schemas, indexes, foreign keys, and audit timestamps.
- Configure Spring profiles: `local-test` (using H2/Testcontainers) and `local-docker` (connecting to MySQL 8.x).

### Acceptance Criteria
1. All Flyway `V1__init.sql` scripts parse and execute without syntax errors.
2. JPA entities match database tables with exact column definitions and unique constraints.
3. Repository unit/integration tests pass against automated test databases.

### Verification Commands (PowerShell)
```powershell
# Type: Persistence & Repository Tests
.\mvnw.cmd clean test -pl services/auth-service,services/passenger-service,services/driver-service,services/ride-service
```

---

## Phase 4: Geospatial Driver Tracking & Proximity Matching

### Scope
- Configure Spring Data Redis in `services/driver-service` and `services/matching-service`.
- Implement `driver-service` location ping endpoint (`POST /api/v1/drivers/location`) executing `GEOADD drivers:available <lng> <lat> <driver_id>` with a companion 60-second TTL heartbeat key (`driver:heartbeat:<id>`).
- Implement `services/matching-service` candidate lookup executing `GEOSEARCH drivers:available FROMLONLAT <lng> <lat> BYRADIUS 5 km ASC COUNT 10`.
- Implement automated unit tests for geospatial coordinate calculations and candidate ranking algorithms.

### Acceptance Criteria
1. Redis geospatial queries return nearest drivers ordered ascending by distance.
2. Stale driver heartbeats (>60 seconds) are excluded from matching candidates.
3. Matching service tests pass with mock Redis and embedded test structures.

### Verification Commands (PowerShell)
```powershell
# Type: Geospatial & Unit Tests
.\mvnw.cmd test -pl services/driver-service,services/matching-service
```

---

## Phase 5: Asynchronous Messaging & Choreography (RabbitMQ)

### Scope
- Integrate Spring AMQP (RabbitMQ) across all relevant services.
- Define RabbitMQ configuration classes declaring:
  - Topic Exchange: `ride.topic.exchange`
  - Direct Exchange: `billing.direct.exchange`
  - Dead Letter Exchange: `dlx.topic.exchange`
  - Queues: `q.ride.matching`, `q.ride.notification`, `q.ride.billing`, `q.dead-letter`
- Implement Spring AMQP message retry template with 3 attempts, exponential backoff, and DLQ routing.
- Implement consumers in `matching-service`, `billing-service`, and `notification-service`.

### Acceptance Criteria
1. Domain events published by `ride-service` are serialized to JSON with standard headers (`traceId`, `timestamp`).
2. RabbitMQ configuration components compile and unit tests verify queue/exchange bindings.
3. Consumer failure tests verify messages route to the Dead-Letter Queue after 3 failed attempts.

### Verification Commands (PowerShell)
```powershell
# Type: Messaging & Integration Tests
.\mvnw.cmd test -pl services/ride-service,services/matching-service,services/notification-service
```

---

## Phase 6: Pricing, Idempotent Billing & Remaining Domain Services

### Scope
- Implement `services/pricing-service`: Fare estimation (base fare + distance rate + duration rate * surge multiplier) with zone surge lookup.
- Implement `services/billing-service`:
  - Consume `ride.completed` from RabbitMQ.
  - Enforce **idempotent payment processing**: Unique database constraint on `idempotency_key` in `payment_transactions` table.
  - Payment simulation logic returning cached receipt on duplicate transaction keys.
- Implement `services/rating-service`: Post-trip reviews, star ratings (1-5), and rolling average calculations.
- Implement `services/reporting-service`: Read-only aggregated reporting data mart consuming all domain events.

### Acceptance Criteria
1. Duplicate payment requests with identical `idempotency_key` return HTTP 200 with the existing receipt without duplicate charges.
2. Pricing algorithm correctly calculates fares for various distances and surge conditions in unit tests.
3. Rating and Reporting service test suites achieve >80% code coverage.

### Verification Commands (PowerShell)
```powershell
# Type: Business Domain & Idempotency Tests
.\mvnw.cmd test -pl services/pricing-service,services/billing-service,services/rating-service,services/reporting-service
```

---

## Phase 7: API Gateway & Security Perimeter

### Scope
- Implement `services/api-gateway` using Spring Cloud Gateway (Reactive / WebFlux).
- Implement JWT authentication filter:
  - Intercepts requests, validates Bearer token against Auth Service public key.
  - Extracts user claims and populates downstream headers (`X-User-Id`, `X-User-Role`).
- Implement Redis-backed token bucket rate limiter filter protecting all public API endpoints.
- Configure circuit breakers with Resilience4j fallbacks.

### Acceptance Criteria
1. Unauthenticated requests to protected routes receive HTTP `401 Unauthorized`.
2. Requests exceeding configured rate limits receive HTTP `429 Too Many Requests`.
3. Downstream headers (`X-User-Id`, `X-User-Role`) are accurately propagated on valid JWT requests.

### Verification Commands (PowerShell)
```powershell
# Type: Gateway & Security Tests
.\mvnw.cmd test -pl services/api-gateway
```

---

## Phase 8: Multi-Stage Containerization & Local Topology

### Scope
- Create production-ready multi-stage `Dockerfile`s for each of the 11 microservices:
  - Build stage: `eclipse-temurin:21-jdk-jammy`
  - Runtime stage: `eclipse-temurin:21-jre-jammy` with non-root user (`appuser:10001`).
- Create root `docker-compose.yml` declaring:
  - All 11 microservices with explicit health checks and environment variables.
  - MySQL 8.x, Redis 7.x, RabbitMQ 3.x with management UI.
  - Exporters: `mysqld_exporter`, `redis_exporter`, `rabbitmq_exporter`, `node_exporter`, `cadvisor`.
- Create `.env.example` defining all required configuration variables.

### Acceptance Criteria
1. `docker compose config` validates cleanly with zero syntax or dependency errors.
2. All 11 service Docker images build successfully on Java 21 without embedded build tools in the final image.
3. `docker compose up -d` brings up the local topology and all services reach `healthy` status.

### Verification Commands (PowerShell)
```powershell
# Type: Container Configuration & Build Verification
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
curl.exe -f http://localhost:8080/actuator/health
```

> [!NOTE]
> **Phase 8 Status**: Implemented and documented in [`docs/PHASE_8_DOCKER_COMPOSE.md`](PHASE_8_DOCKER_COMPOSE.md). All 11 microservice multi-stage Dockerfiles, root `docker-compose.yml`, MySQL database bootstrap scripts, `.env.example`, and Spring Boot Actuator probes are configured and verified via `docker compose config` (exit code 0) and `.\mvnw.cmd clean verify` (238 passing tests across 12 modules). Live Docker daemon deployment requires a running Docker Engine.


---

## Phase 9: Local Kubernetes Orchestration

### Scope
- Author declarative Kubernetes manifests in `k8s/`:
  - `k8s/base/`: Deployments, ClusterIP Services, ConfigMaps, Secrets, Ingress.
  - `k8s/overlays/local/`: Kustomize overlay configured for local clusters (`minikube`/`kind`).
- Configure Kubernetes resource requests (`100m` CPU, `256Mi` RAM) and limits (`500m` CPU, `512Mi` RAM).
- Define `livenessProbe` (`/actuator/health/liveness`) and `readinessProbe` (`/actuator/health/readiness`).
- Define Horizontal Pod Autoscaler (HPA) specifications for high-load services (Gateway, Ride, Matching).

### Acceptance Criteria
1. `kubectl apply --dry-run=client -k k8s/overlays/local` validates with zero schema errors.
2. All pods reach `Running` and `Ready` status within 120 seconds on a local Minikube/KinD cluster.
3. Ingress routes traffic accurately to downstream services.

### Verification Commands (PowerShell)
```powershell
# Type: Kubernetes Manifest Validation & Cluster Status
kubectl apply --dry-run=client -k k8s/overlays/local
kubectl get pods -n ride-hailing-local
kubectl get svc -n ride-hailing-local
```

---

## Phase 10: Full-Stack Observability & Telemetry

### Scope
- Configure Prometheus server (`monitoring/prometheus/prometheus.yml`) with scrape jobs for:
  - Spring Boot microservices (`/actuator/prometheus`)
  - `mysqld_exporter` (port 9104)
  - `redis_exporter` (port 9121)
  - `rabbitmq_exporter` (port 15692)
  - `node_exporter` (port 9100)
  - `cadvisor` (port 8080)
- Author Prometheus alerting rules (`monitoring/prometheus/alert.rules.yml`):
  - Service down alert (`up == 0` for 1 minute).
  - High HTTP 5xx rate (>5% over 5m).
  - Dead Letter Queue accumulating messages (>0 for 5m).
- Configure Alertmanager (`monitoring/alertmanager/alertmanager.yml`) with webhook/email receiver stubs.
- Author Grafana dashboard JSON models (`monitoring/grafana/dashboards/`):
  - JVM & Application Performance Dashboard
  - Infrastructure & System Exporters Dashboard
  - Live Business Operations Dashboard (`rides_requested_total`, `rides_completed_total`, `matching_duration_seconds`, `payment_failures_total`, `active_drivers_count`).

### Acceptance Criteria
1. Prometheus configuration and alert rules pass `promtool` validation.
2. Alertmanager configuration passes `amtool` or schema validation.
3. Prometheus targets endpoint returns all configured jobs in `UP` state.
4. Grafana health check returns HTTP 200 and loads pre-provisioned dashboards.

### Verification Commands (PowerShell)
```powershell
# Type: Telemetry Configuration & Endpoint Verification
# Promtool validation (requires promtool installed or executed via docker container)
docker run --rm -v ${PWD}/monitoring/prometheus:/etc/prometheus prom/prometheus:v2.51.0 promtool check config /etc/prometheus/prometheus.yml
docker run --rm -v ${PWD}/monitoring/prometheus:/etc/prometheus prom/prometheus:v2.51.0 promtool check rules /etc/prometheus/alert.rules.yml

# Live status verification
curl.exe -f http://localhost:9090/api/v1/targets
curl.exe -f http://localhost:9093/api/v2/status
curl.exe -f http://localhost:3000/api/health
```

---

## Phase 11: Continuous Integration & Security (GitHub Actions)

### Scope
- Author GitHub Actions workflows under `.github/workflows/`:
  - `ci.yml`: Triggered on pull requests; installs Java 21 via `actions/setup-java`, executes Checkstyle, compiles code, runs unit and integration tests.
  - `security-scan.yml`: Dependency vulnerability scanning (OWASP Dependency Check / Trivy) and container image scanning.
  - `docker-build.yml`: Verifies Docker multi-stage builds and pushes images to container registry on main branch merge.
- Implement automated workflow validation in PowerShell ensuring valid YAML structure and action parameters.

### Acceptance Criteria
1. All workflow files parse cleanly as valid YAML without schema violations.
2. Java 21 is explicitly configured in all setup-java steps (`java-version: '21'`, `distribution: 'temurin'`).
3. Local simulated workflow verification script returns exit code 0.

### Verification Commands (PowerShell)
```powershell
# Type: CI/CD Syntax & Workflow Schema Check
Get-ChildItem -Path .github/workflows/*.yml | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match "java-version:\s*'?21'?") {
        Write-Host "SUCCESS: $($_.Name) enforces Java 21" -ForegroundColor Green
    } else {
        Write-Error "FAILURE: $($_.Name) does not specify Java 21"
    }
}
```

---

## Phase 12: Infrastructure as Code (IaC) & Configuration Management

### Scope
- Implement modular Terraform code (`terraform/`):
  - `terraform/modules/vpc`: VPC, public/private subnets, NAT gateways, route tables.
  - `terraform/modules/kubernetes`: Managed cluster modules (EKS / local KinD provider).
  - `terraform/modules/database`: RDS MySQL database definitions.
  - `terraform/environments/local`: Local infrastructure orchestration.
  - `terraform/environments/aws`: AWS production topology.
- Implement Ansible playbooks (`ansible/`):
  - `ansible/playbooks/hardening.yml`: Baseline host security, SSH hardening, firewall rules.
  - `ansible/playbooks/container-runtime.yml`: Docker engine and prerequisite dependencies.
  - `ansible/playbooks/site.yml`: Master orchestration playbook.
- **Human Approval Safety Guard:** AWS Terraform code is limited strictly to `validate` and `plan`. Never execute `apply` or `destroy` autonomously.

### Acceptance Criteria
1. `terraform fmt -check -recursive terraform/` and `terraform validate` pass with zero syntax or provider errors.
2. Ansible playbooks pass syntax verification (`ansible-playbook --syntax-check`).
3. No remote AWS apply or destroy operations are executed.

### Verification Commands (PowerShell)
```powershell
# Type: IaC & Configuration Management Syntax Checks
terraform -chdir=terraform/environments/local init -backend=false
terraform -chdir=terraform/environments/local validate
ansible-playbook --syntax-check ansible/playbooks/site.yml
```

---

## Phase 13: Viva Preparation, Troubleshooting & End-to-End Acceptance

### Scope
- Author academic and presentation deliverables in `docs/`:
  - `docs/VIVA_NOTES.md`: Conceptual cheat-sheet covering microservices architecture, RabbitMQ vs Kafka trade-offs, Redis geospatial mechanics, idempotent payments, Docker multi-stage builds, and Kubernetes orchestration.
  - `docs/TROUBLESHOOTING.md`: Incident runbooks covering container crashes, RabbitMQ queue backlogs, MySQL connection pool saturation, and metric scraping failures.
  - `docs/SYLLABUS_MAPPING.md`: Matrix mapping DevOps curriculum topics (CI/CD, IaC, Monitoring, Containers, Orchestration) to specific repository files.
- Author automated end-to-end PowerShell verification script: `scripts/test-e2e-flow.ps1`.
  - Simulates complete user journey: Register passenger & driver -> Send driver location -> Request ride -> Match & Accept -> Complete trip -> Verify idempotent payment -> Verify Prometheus business metrics increment.

### Acceptance Criteria
1. `docs/VIVA_NOTES.md`, `docs/TROUBLESHOOTING.md`, and `docs/SYLLABUS_MAPPING.md` exist and comprehensively document the platform.
2. `scripts/test-e2e-flow.ps1` executes unattended against active local environment and returns exit code 0.
3. Final verification confirms 0 failed tests, 0 schema violations, and complete Java 21 compliance across all modules.

### Verification Commands (PowerShell)
```powershell
# Type: Comprehensive End-to-End System Verification
.\mvnw.cmd clean verify
.\scripts\test-e2e-flow.ps1
```
