package com.ridehailing.reporting.repository;

import com.ridehailing.reporting.entity.DriverPerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverPerformanceRepository extends JpaRepository<DriverPerformanceEntity, Long> {

    Optional<DriverPerformanceEntity> findByDriverIdAndReportDate(Long driverId, LocalDate reportDate);

    List<DriverPerformanceEntity> findByDriverIdOrderByReportDateDesc(Long driverId);
}
