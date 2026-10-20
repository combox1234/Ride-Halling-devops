package com.ridehailing.reporting.repository;

import com.ridehailing.reporting.entity.DailyMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetricsEntity, Long> {

    Optional<DailyMetricsEntity> findByReportDate(LocalDate reportDate);

    @Query("SELECT COALESCE(SUM(d.totalRidesRequested), 0) FROM DailyMetricsEntity d")
    long sumAllRidesRequested();

    @Query("SELECT COALESCE(SUM(d.totalRidesCompleted), 0) FROM DailyMetricsEntity d")
    long sumAllRidesCompleted();

    @Query("SELECT COALESCE(SUM(d.totalRidesCancelled), 0) FROM DailyMetricsEntity d")
    long sumAllRidesCancelled();

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0.00) FROM DailyMetricsEntity d")
    BigDecimal sumAllRevenue();
}
