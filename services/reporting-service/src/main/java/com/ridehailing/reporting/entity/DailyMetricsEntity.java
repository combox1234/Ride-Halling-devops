package com.ridehailing.reporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "daily_metrics")
public class DailyMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_rides_requested", nullable = false)
    private int totalRidesRequested;

    @Column(name = "total_rides_started", nullable = false)
    private int totalRidesStarted;

    @Column(name = "total_rides_completed", nullable = false)
    private int totalRidesCompleted;

    @Column(name = "total_rides_cancelled", nullable = false)
    private int totalRidesCancelled;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailyMetricsEntity() {
    }

    public DailyMetricsEntity(LocalDate reportDate) {
        this.reportDate = reportDate;
        this.totalRidesRequested = 0;
        this.totalRidesStarted = 0;
        this.totalRidesCompleted = 0;
        this.totalRidesCancelled = 0;
        this.totalRevenue = BigDecimal.ZERO.setScale(2);
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
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO.setScale(2);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public int getTotalRidesRequested() {
        return totalRidesRequested;
    }

    public void incrementRidesRequested() {
        this.totalRidesRequested++;
    }

    public int getTotalRidesStarted() {
        return totalRidesStarted;
    }

    public void incrementRidesStarted() {
        this.totalRidesStarted++;
    }

    public int getTotalRidesCompleted() {
        return totalRidesCompleted;
    }

    public void incrementRidesCompleted() {
        this.totalRidesCompleted++;
    }

    public int getTotalRidesCancelled() {
        return totalRidesCancelled;
    }

    public void incrementRidesCancelled() {
        this.totalRidesCancelled++;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void addRevenue(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalRevenue = this.totalRevenue.add(amount);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
