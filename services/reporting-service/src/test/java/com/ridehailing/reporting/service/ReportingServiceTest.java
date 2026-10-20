package com.ridehailing.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.PaymentStatus;
import com.ridehailing.common.event.*;
import com.ridehailing.reporting.dto.DailyMetricsResponse;
import com.ridehailing.reporting.dto.DriverPerformanceResponse;
import com.ridehailing.reporting.dto.PlatformOverviewResponse;
import com.ridehailing.reporting.entity.DailyMetricsEntity;
import com.ridehailing.reporting.entity.DriverPerformanceEntity;
import com.ridehailing.reporting.entity.ReportingEventEntity;
import com.ridehailing.reporting.repository.DailyMetricsRepository;
import com.ridehailing.reporting.repository.DriverPerformanceRepository;
import com.ridehailing.reporting.repository.ReportingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportingEventRepository eventRepository;

    @Mock
    private DailyMetricsRepository dailyMetricsRepository;

    @Mock
    private DriverPerformanceRepository driverPerformanceRepository;

    private ObjectMapper objectMapper;
    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        reportingService = new ReportingService(
            eventRepository,
            dailyMetricsRepository,
            driverPerformanceRepository,
            objectMapper
        );
    }

    @Test
    void shouldRecordRideRequestedEvent() {
        String eventId = UUID.randomUUID().toString();
        RideRequestedEvent payload = new RideRequestedEvent(
            100L, 200L,
            new LocationDto(37.7749, -122.4194, "A"),
            new LocationDto(37.8049, -122.4194, "B"),
            BigDecimal.valueOf(25.00),
            Instant.now()
        );
        EventEnvelope<RideRequestedEvent> envelope = new EventEnvelope<>(
            eventId, "RideRequestedEvent", Instant.now(), "trace-1", payload
        );

        when(eventRepository.existsByEventId(eventId)).thenReturn(false);
        when(dailyMetricsRepository.findByReportDate(any())).thenReturn(Optional.empty());

        reportingService.recordEvent(envelope);

        ArgumentCaptor<DailyMetricsEntity> metricsCaptor = ArgumentCaptor.forClass(DailyMetricsEntity.class);
        verify(dailyMetricsRepository, times(1)).save(metricsCaptor.capture());
        assertEquals(1, metricsCaptor.getValue().getTotalRidesRequested());

        verify(eventRepository, times(1)).save(any(ReportingEventEntity.class));
    }

    @Test
    void shouldRecordRideCompletedAndDriverPerformance() {
        String eventId = UUID.randomUUID().toString();
        RideCompletedEvent payload = new RideCompletedEvent(
            101L, 202L, 303L, BigDecimal.valueOf(50.00), "PAY-101", Instant.now()
        );
        EventEnvelope<RideCompletedEvent> envelope = new EventEnvelope<>(
            eventId, "RideCompletedEvent", Instant.now(), "trace-2", payload
        );

        when(eventRepository.existsByEventId(eventId)).thenReturn(false);
        when(dailyMetricsRepository.findByReportDate(any())).thenReturn(Optional.empty());
        when(driverPerformanceRepository.findByDriverIdAndReportDate(eq(303L), any())).thenReturn(Optional.empty());

        reportingService.recordEvent(envelope);

        ArgumentCaptor<DailyMetricsEntity> metricsCaptor = ArgumentCaptor.forClass(DailyMetricsEntity.class);
        verify(dailyMetricsRepository, times(1)).save(metricsCaptor.capture());
        assertEquals(1, metricsCaptor.getValue().getTotalRidesCompleted());

        ArgumentCaptor<DriverPerformanceEntity> driverCaptor = ArgumentCaptor.forClass(DriverPerformanceEntity.class);
        verify(driverPerformanceRepository, times(1)).save(driverCaptor.capture());
        assertEquals(1, driverCaptor.getValue().getCompletedTrips());
        // 80% of $50.00 = $40.00
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), driverCaptor.getValue().getTotalEarnings());
    }

    @Test
    void shouldRecordPaymentProcessedEventRevenue() {
        String eventId = UUID.randomUUID().toString();
        PaymentProcessedEvent payload = new PaymentProcessedEvent(
            1L, 101L, 202L, BigDecimal.valueOf(50.00), PaymentStatus.SUCCESS, "TXN-1", "PAY-101", Instant.now()
        );
        EventEnvelope<PaymentProcessedEvent> envelope = new EventEnvelope<>(
            eventId, "PaymentProcessedEvent", Instant.now(), "trace-3", payload
        );

        when(eventRepository.existsByEventId(eventId)).thenReturn(false);
        when(dailyMetricsRepository.findByReportDate(any())).thenReturn(Optional.empty());

        reportingService.recordEvent(envelope);

        ArgumentCaptor<DailyMetricsEntity> metricsCaptor = ArgumentCaptor.forClass(DailyMetricsEntity.class);
        verify(dailyMetricsRepository, times(1)).save(metricsCaptor.capture());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), metricsCaptor.getValue().getTotalRevenue());
    }

    @Test
    void shouldBeIdempotentOnDuplicateEventId() {
        String eventId = "DUPLICATE-EVENT-123";
        RideRequestedEvent payload = new RideRequestedEvent(
            100L, 200L,
            new LocationDto(37.7749, -122.4194, "A"),
            new LocationDto(37.8049, -122.4194, "B"),
            BigDecimal.valueOf(25.00),
            Instant.now()
        );
        EventEnvelope<RideRequestedEvent> envelope = new EventEnvelope<>(
            eventId, "RideRequestedEvent", Instant.now(), "trace-1", payload
        );

        when(eventRepository.existsByEventId(eventId)).thenReturn(true);

        reportingService.recordEvent(envelope);

        // IDEMPOTENCY: No database updates on duplicate eventId!
        verify(dailyMetricsRepository, never()).save(any());
        verify(driverPerformanceRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldReturnPlatformOverview() {
        when(dailyMetricsRepository.sumAllRidesRequested()).thenReturn(150L);
        when(dailyMetricsRepository.sumAllRidesCompleted()).thenReturn(120L);
        when(dailyMetricsRepository.sumAllRidesCancelled()).thenReturn(10L);
        when(dailyMetricsRepository.sumAllRevenue()).thenReturn(BigDecimal.valueOf(3600.00));

        PlatformOverviewResponse overview = reportingService.getOverview();

        assertNotNull(overview);
        assertEquals(150L, overview.totalRidesRequested());
        assertEquals(120L, overview.totalRidesCompleted());
        assertEquals(10L, overview.totalRidesCancelled());
        assertEquals(BigDecimal.valueOf(3600.00), overview.totalPlatformRevenue());
    }
}
