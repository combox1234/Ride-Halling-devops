package com.ridehailing.rating.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "ratings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ratings_ride_rater", columnNames = {"ride_id", "rater_id"})
    }
)
public class RatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "rater_id", nullable = false)
    private Long raterId;

    @Column(name = "rater_type", nullable = false, length = 20)
    private String raterType; // PASSENGER or DRIVER

    @Column(name = "ratee_id", nullable = false)
    private Long rateeId;

    @Column(name = "score", nullable = false)
    private int score; // 1 to 5

    @Column(name = "feedback", length = 500)
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RatingEntity() {
    }

    public RatingEntity(Long rideId, Long raterId, String raterType, Long rateeId, int score, String feedback) {
        this.rideId = rideId;
        this.raterId = raterId;
        this.raterType = raterType;
        this.rateeId = rateeId;
        this.score = score;
        this.feedback = feedback;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public Long getRaterId() {
        return raterId;
    }

    public void setRaterId(Long raterId) {
        this.raterId = raterId;
    }

    public String getRaterType() {
        return raterType;
    }

    public void setRaterType(String raterType) {
        this.raterType = raterType;
    }

    public Long getRateeId() {
        return rateeId;
    }

    public void setRateeId(Long rateeId) {
        this.rateeId = rateeId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
