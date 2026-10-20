CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ride_id BIGINT NOT NULL,
    rater_id BIGINT NOT NULL,
    rater_type VARCHAR(20) NOT NULL,
    ratee_id BIGINT NOT NULL,
    score INT NOT NULL,
    feedback VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ratings_ride_rater UNIQUE (ride_id, rater_id)
);

CREATE INDEX idx_ratings_ratee_id ON ratings(ratee_id);
CREATE INDEX idx_ratings_ride_id ON ratings(ride_id);
