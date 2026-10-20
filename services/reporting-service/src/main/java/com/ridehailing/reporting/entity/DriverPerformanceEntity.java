package com.ridehailing.reporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(
    name = "driver_daily_performance",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_performance_date", columnNames = {"driver_id", "report_date"})
    }
)
public class DriverPerformanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "completed_trips", nullable = false)
    private int completedTrips;

    @Column(name = "total_earnings", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEarnings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DriverPerformanceEntity() {
    }

    public DriverPerformanceEntity(Long driverId, LocalDate reportDate) {
        this.driverId = driverId;
        this.reportDate = reportDate;
        this.completedTrips = 0;
        this.totalEarnings = BigDecimal.ZERO.setScale(2);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO.setScale(2);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public int getCompletedTrips() {
        return completedTrips;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void addCompletedTrip(BigDecimal earnings) {
        this.completedTrips++;
        if (earnings != null && earnings.compareTo(BigDecimal.ZERO) > 0) {
            this.totalEarnings = this.totalEarnings.add(earnings);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
