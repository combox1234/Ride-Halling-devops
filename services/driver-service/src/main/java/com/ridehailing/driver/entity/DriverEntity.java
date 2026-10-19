package com.ridehailing.driver.entity;

import com.ridehailing.common.dto.DriverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "drivers")
public class DriverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "vehicle_make", nullable = false, length = 50)
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false, length = 50)
    private String vehicleModel;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DriverStatus status;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Column(name = "total_trips", nullable = false)
    private Integer totalTrips;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DriverEntity() {
    }

    public DriverEntity(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String vehicleMake,
        String vehicleModel,
        String licensePlate,
        DriverStatus status,
        Double rating,
        Integer totalTrips,
        Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.licensePlate = licensePlate;
        this.status = status != null ? status : DriverStatus.OFFLINE;
        this.rating = rating != null ? rating : 5.0;
        this.totalTrips = totalTrips != null ? totalTrips : 0;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public com.ridehailing.driver.model.DriverProfile toModel() {
        return new com.ridehailing.driver.model.DriverProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.vehicleMake,
            this.vehicleModel,
            this.licensePlate,
            this.status,
            this.rating,
            this.totalTrips,
            this.createdAt
        );
    }

    public static DriverEntity fromModel(com.ridehailing.driver.model.DriverProfile model) {
        if (model == null) {
            return null;
        }
        return new DriverEntity(
            model.id(),
            model.userId(),
            model.name(),
            model.email(),
            model.phone(),
            model.vehicleMake(),
            model.vehicleModel(),
            model.licensePlate(),
            model.status(),
            model.rating(),
            model.totalTrips(),
            model.createdAt()
        );
    }
}
