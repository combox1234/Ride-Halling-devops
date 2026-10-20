package com.ridehailing.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.event.*;
import com.ridehailing.reporting.dto.DailyMetricsResponse;
import com.ridehailing.reporting.dto.DriverPerformanceResponse;
import com.ridehailing.reporting.dto.PlatformOverviewResponse;
import com.ridehailing.reporting.dto.ReportingEventResponse;
import com.ridehailing.reporting.entity.DailyMetricsEntity;
import com.ridehailing.reporting.entity.DriverPerformanceEntity;
import com.ridehailing.reporting.entity.ReportingEventEntity;
import com.ridehailing.reporting.repository.DailyMetricsRepository;
import com.ridehailing.reporting.repository.DriverPerformanceRepository;
import com.ridehailing.reporting.repository.ReportingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final ReportingEventRepository eventRepository;
    private final DailyMetricsRepository dailyMetricsRepository;
    private final DriverPerformanceRepository driverPerformanceRepository;
    private final ObjectMapper objectMapper;

    public ReportingService(
        ReportingEventRepository eventRepository,
        DailyMetricsRepository dailyMetricsRepository,
        DriverPerformanceRepository driverPerformanceRepository,
        ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.driverPerformanceRepository = driverPerformanceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordEvent(EventEnvelope<?> envelope) {
        String eventId = envelope.eventId();
        String eventType = envelope.eventType();

        // 1. Idempotency Check: deduplicate event processing
        if (eventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event ignored by reporting data mart: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        Long aggregateId = null;
        String payloadJson = null;
        try {
            payloadJson = objectMapper.writeValueAsString(envelope.payload());
        } catch (Exception e) {
            log.warn("Failed to serialize event payload to JSON: {}", e.getMessage());
        }

        LocalDate reportDate = envelope.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
        DailyMetricsEntity dailyMetrics = dailyMetricsRepository.findByReportDate(reportDate)
            .orElseGet(() -> new DailyMetricsEntity(reportDate));

        switch (eventType) {
            case "RideRequestedEvent" -> {
                RideRequestedEvent event = convert(envelope.payload(), RideRequestedEvent.class);
                aggregateId = event.rideId();
                dailyMetrics.incrementRidesRequested();
            }
            case "RideStartedEvent" -> {
                RideStartedEvent event = convert(envelope.payload(), RideStartedEvent.class);
                aggregateId = event.rideId();
                dailyMetrics.incrementRidesStarted();
            }
            case "RideCompletedEvent" -> {
                RideCompletedEvent event = convert(envelope.payload(), RideCompletedEvent.class);
                aggregateId = event.rideId();
                dailyMetrics.incrementRidesCompleted();

                // Driver earnings computation: standard 80% payout split
                if (event.driverId() != null && event.driverId() > 0) {
                    BigDecimal driverEarnings = event.finalFare().multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
                    DriverPerformanceEntity driverPerf = driverPerformanceRepository
                        .findByDriverIdAndReportDate(event.driverId(), reportDate)
                        .orElseGet(() -> new DriverPerformanceEntity(event.driverId(), reportDate));
                    driverPerf.addCompletedTrip(driverEarnings);
                    driverPerformanceRepository.save(driverPerf);
                }
            }
            case "RideCancelledEvent" -> {
                RideCancelledEvent event = convert(envelope.payload(), RideCancelledEvent.class);
                aggregateId = event.rideId();
                dailyMetrics.incrementRidesCancelled();
            }
            case "PaymentProcessedEvent" -> {
                PaymentProcessedEvent event = convert(envelope.payload(), PaymentProcessedEvent.class);
                aggregateId = event.paymentId();
                dailyMetrics.addRevenue(event.amount());
            }
            default -> log.debug("Reporting service received non-metric event: {}", eventType);
        }

        dailyMetricsRepository.save(dailyMetrics);

        ReportingEventEntity eventEntity = new ReportingEventEntity(
            eventId,
            eventType,
            aggregateId,
            envelope.traceId(),
            payloadJson,
            envelope.timestamp()
        );
        eventRepository.save(eventEntity);
        log.info("Recorded event {} [id={}] into reporting data mart", eventType, eventId);
    }

    @Transactional(readOnly = true)
    public DailyMetricsResponse getDailySummary(LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now(ZoneOffset.UTC);
        DailyMetricsEntity entity = dailyMetricsRepository.findByReportDate(queryDate)
            .orElseGet(() -> new DailyMetricsEntity(queryDate));

        return new DailyMetricsResponse(
            entity.getReportDate(),
            entity.getTotalRidesRequested(),
            entity.getTotalRidesStarted(),
            entity.getTotalRidesCompleted(),
            entity.getTotalRidesCancelled(),
            entity.getTotalRevenue()
        );
    }

    @Transactional(readOnly = true)
    public DriverPerformanceResponse getDriverPerformance(Long driverId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now(ZoneOffset.UTC);
        DriverPerformanceEntity entity = driverPerformanceRepository.findByDriverIdAndReportDate(driverId, queryDate)
            .orElseGet(() -> new DriverPerformanceEntity(driverId, queryDate));

        return new DriverPerformanceResponse(
            entity.getDriverId(),
            entity.getReportDate(),
            entity.getCompletedTrips(),
            entity.getTotalEarnings()
        );
    }

    @Transactional(readOnly = true)
    public List<DriverPerformanceResponse> getDriverPerformanceHistory(Long driverId) {
        return driverPerformanceRepository.findByDriverIdOrderByReportDateDesc(driverId).stream()
            .map(e -> new DriverPerformanceResponse(e.getDriverId(), e.getReportDate(), e.getCompletedTrips(), e.getTotalEarnings()))
            .toList();
    }

    @Transactional(readOnly = true)
    public PlatformOverviewResponse getOverview() {
        long requested = dailyMetricsRepository.sumAllRidesRequested();
        long completed = dailyMetricsRepository.sumAllRidesCompleted();
        long cancelled = dailyMetricsRepository.sumAllRidesCancelled();
        BigDecimal revenue = dailyMetricsRepository.sumAllRevenue();

        return new PlatformOverviewResponse(requested, completed, cancelled, revenue);
    }

    @Transactional(readOnly = true)
    public List<ReportingEventResponse> getRecentEvents() {
        return eventRepository.findTop50ByOrderByOccurredAtDesc().stream()
            .map(e -> new ReportingEventResponse(
                e.getId(),
                e.getEventId(),
                e.getEventType(),
                e.getAggregateId(),
                e.getTraceId(),
                e.getOccurredAt(),
                e.getRecordedAt()
            ))
            .toList();
    }

    private <T> T convert(Object payload, Class<T> clazz) {
        if (clazz.isInstance(payload)) {
            return clazz.cast(payload);
        }
        try {
            return objectMapper.convertValue(payload, clazz);
        } catch (Exception e) {
            log.error("Failed to parse event payload to {}: {}", clazz.getSimpleName(), payload, e);
            throw new IllegalArgumentException("Payload cannot be parsed to " + clazz.getSimpleName(), e);
        }
    }
}
