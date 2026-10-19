package com.ridehailing.ride.entity;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.ride.model.RideRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rides")
public class RideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "dropoff_latitude", nullable = false)
    private Double dropoffLatitude;

    @Column(name = "dropoff_longitude", nullable = false)
    private Double dropoffLongitude;

    @Column(name = "dropoff_address")
    private String dropoffAddress;

    @Column(name = "estimated_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "final_fare", precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RideStatus status;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public RideEntity() {
    }

    public RideEntity(
        Long id,
        Long passengerId,
        Long driverId,
        Double pickupLatitude,
        Double pickupLongitude,
        String pickupAddress,
        Double dropoffLatitude,
        Double dropoffLongitude,
        String dropoffAddress,
        BigDecimal estimatedFare,
        BigDecimal finalFare,
        RideStatus status,
        String idempotencyKey,
        Instant requestedAt,
        Instant completedAt
    ) {
        this.id = id;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.pickupAddress = pickupAddress;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.dropoffAddress = dropoffAddress;
        this.estimatedFare = estimatedFare;
        this.finalFare = finalFare;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestedAt = requestedAt != null ? requestedAt : Instant.now();
        this.completedAt = completedAt;
        this.version = 0L;
    }

    public static RideEntity fromModel(RideRecord model) {
        LocationDto pickup = model.pickupLocation();
        LocationDto dropoff = model.dropoffLocation();
        return new RideEntity(
            model.id(),
            model.passengerId(),
            model.driverId(),
            pickup != null ? pickup.latitude() : 0.0,
            pickup != null ? pickup.longitude() : 0.0,
            pickup != null ? pickup.address() : null,
            dropoff != null ? dropoff.latitude() : 0.0,
            dropoff != null ? dropoff.longitude() : 0.0,
            dropoff != null ? dropoff.address() : null,
            model.estimatedFare(),
            model.finalFare(),
            model.status(),
            model.idempotencyKey(),
            model.requestedAt(),
            model.completedAt()
        );
    }

    public RideRecord toModel() {
        LocationDto pickup = new LocationDto(this.pickupLatitude, this.pickupLongitude, this.pickupAddress);
        LocationDto dropoff = new LocationDto(this.dropoffLatitude, this.dropoffLongitude, this.dropoffAddress);
        return new RideRecord(
            this.id,
            this.passengerId,
            this.driverId,
            pickup,
            dropoff,
            this.estimatedFare,
            this.finalFare,
            this.status,
            this.idempotencyKey,
            this.requestedAt,
            this.completedAt
        );
    }

    public void updateFromModel(RideRecord model) {
        this.passengerId = model.passengerId();
        this.driverId = model.driverId();
        if (model.pickupLocation() != null) {
            this.pickupLatitude = model.pickupLocation().latitude();
            this.pickupLongitude = model.pickupLocation().longitude();
            this.pickupAddress = model.pickupLocation().address();
        }
        if (model.dropoffLocation() != null) {
            this.dropoffLatitude = model.dropoffLocation().latitude();
            this.dropoffLongitude = model.dropoffLocation().longitude();
            this.dropoffAddress = model.dropoffLocation().address();
        }
        this.estimatedFare = model.estimatedFare();
        this.finalFare = model.finalFare();
        this.status = model.status();
        this.idempotencyKey = model.idempotencyKey();
        this.completedAt = model.completedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(Double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public Double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(Double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public BigDecimal getFinalFare() {
        return finalFare;
    }

    public void setFinalFare(BigDecimal finalFare) {
        this.finalFare = finalFare;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
