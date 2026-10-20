package com.ridehailing.rating.service;

import com.ridehailing.rating.dto.RatingResponse;
import com.ridehailing.rating.dto.RatingSummaryResponse;
import com.ridehailing.rating.dto.SubmitRatingRequest;
import com.ridehailing.rating.entity.RatingEntity;
import com.ridehailing.rating.repository.RatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public RatingResponse submitRating(SubmitRatingRequest request) {
        if (request.score() < 1 || request.score() > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }

        if (ratingRepository.existsByRideIdAndRaterId(request.rideId(), request.raterId())) {
            throw new IllegalStateException("Rating already submitted for ride #" + request.rideId() + " by rater #" + request.raterId());
        }

        RatingEntity entity = new RatingEntity(
            request.rideId(),
            request.raterId(),
            request.raterType().toUpperCase(),
            request.rateeId(),
            request.score(),
            request.feedback()
        );

        try {
            RatingEntity saved = ratingRepository.save(entity);
            log.info("Recorded rating #{}: rideId={}, raterId={}, rateeId={}, score={}",
                saved.getId(), saved.getRideId(), saved.getRaterId(), saved.getRateeId(), saved.getScore());
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate rating constraint violation for rideId={} and raterId={}", request.rideId(), request.raterId());
            throw new IllegalStateException("Rating already submitted for ride #" + request.rideId() + " by rater #" + request.raterId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRateeSummary(Long rateeId) {
        List<RatingEntity> ratings = ratingRepository.findByRateeId(rateeId);

        Map<Integer, Long> breakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            breakdown.put(i, 0L);
        }

        if (ratings.isEmpty()) {
            return new RatingSummaryResponse(rateeId, 5.0, 0, breakdown);
        }

        long sum = 0;
        for (RatingEntity r : ratings) {
            int score = r.getScore();
            sum += score;
            breakdown.put(score, breakdown.getOrDefault(score, 0L) + 1);
        }

        double average = (double) sum / ratings.size();
        double roundedAverage = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue();

        return new RatingSummaryResponse(rateeId, roundedAverage, ratings.size(), breakdown);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsByRideId(Long rideId) {
        return ratingRepository.findByRideId(rideId).stream()
            .map(this::toResponse)
            .toList();
    }

    private RatingResponse toResponse(RatingEntity entity) {
        return new RatingResponse(
            entity.getId(),
            entity.getRideId(),
            entity.getRaterId(),
            entity.getRaterType(),
            entity.getRateeId(),
            entity.getScore(),
            entity.getFeedback(),
            entity.getCreatedAt()
        );
    }
}
