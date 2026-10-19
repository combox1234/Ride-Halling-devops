package com.ridehailing.passenger.entity;

import com.ridehailing.passenger.model.PassengerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passengers")
public class PassengerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    private Double rating;

    @Column(name = "total_rides", nullable = false)
    private int totalRides;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PassengerEntity() {
    }

    public PassengerEntity(Long id, Long userId, String name, String email, String phone, Double rating, int totalRides, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.rating = rating != null ? rating : 5.0;
        this.totalRides = totalRides;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PassengerEntity fromModel(PassengerProfile model) {
        return new PassengerEntity(
            model.id(),
            model.userId(),
            model.name(),
            model.email() != null ? model.email().toLowerCase().trim() : null,
            model.phone(),
            model.rating(),
            model.totalRides(),
            model.createdAt()
        );
    }

    public PassengerProfile toModel() {
        return new PassengerProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.rating,
            this.totalRides,
            this.createdAt
        );
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
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public int getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(int totalRides) {
        this.totalRides = totalRides;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
