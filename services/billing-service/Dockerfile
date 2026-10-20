# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Build Stage (Maven + Eclipse Temurin JDK 21)
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy Maven wrapper and root POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy module POMs for dependency resolution
COPY services/common-dto/pom.xml services/common-dto/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/passenger-service/pom.xml services/passenger-service/
COPY services/driver-service/pom.xml services/driver-service/
COPY services/ride-service/pom.xml services/ride-service/
COPY services/matching-service/pom.xml services/matching-service/
COPY services/pricing-service/pom.xml services/pricing-service/
COPY services/billing-service/pom.xml services/billing-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY services/rating-service/pom.xml services/rating-service/
COPY services/reporting-service/pom.xml services/reporting-service/
COPY services/api-gateway/pom.xml services/api-gateway/

# Copy sources for common library and target service
COPY services/common-dto/src services/common-dto/src
COPY services/billing-service/src services/billing-service/src

# Build executable jar
RUN ./mvnw -B clean package -pl services/billing-service -am -DskipTests

# =============================================================================
# Stage 2: Runtime Stage (Eclipse Temurin JRE 21)
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="Ride-Hailing Platform Team"
LABEL service="billing-service"

# Install curl for container health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root application user and group (UID/GID 10001)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/sh -d /app appuser

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/services/billing-service/target/billing-service-*.jar /app/app.jar

USER appuser:appgroup

EXPOSE 8087

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
