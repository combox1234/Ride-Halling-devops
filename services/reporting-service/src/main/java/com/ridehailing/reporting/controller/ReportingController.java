package com.ridehailing.reporting.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.reporting.dto.DailyMetricsResponse;
import com.ridehailing.reporting.dto.DriverPerformanceResponse;
import com.ridehailing.reporting.dto.PlatformOverviewResponse;
import com.ridehailing.reporting.dto.ReportingEventResponse;
import com.ridehailing.reporting.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/daily-summary")
    public ResponseEntity<ApiResponse<DailyMetricsResponse>> getDailySummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailyMetricsResponse summary = reportingService.getDailySummary(date);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/driver-performance/{driverId}")
    public ResponseEntity<ApiResponse<DriverPerformanceResponse>> getDriverPerformance(
        @PathVariable Long driverId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DriverPerformanceResponse response = reportingService.getDriverPerformance(driverId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/driver-performance/{driverId}/history")
    public ResponseEntity<ApiResponse<List<DriverPerformanceResponse>>> getDriverPerformanceHistory(
        @PathVariable Long driverId
    ) {
        List<DriverPerformanceResponse> history = reportingService.getDriverPerformanceHistory(driverId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformOverviewResponse>> getOverview() {
        PlatformOverviewResponse overview = reportingService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<ReportingEventResponse>>> getRecentEvents() {
        List<ReportingEventResponse> events = reportingService.getRecentEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
