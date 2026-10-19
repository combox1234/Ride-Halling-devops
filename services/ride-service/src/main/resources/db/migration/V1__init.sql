CREATE TABLE IF NOT EXISTS rides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    driver_id BIGINT,
    pickup_latitude DOUBLE NOT NULL,
    pickup_longitude DOUBLE NOT NULL,
    pickup_address VARCHAR(255),
    dropoff_latitude DOUBLE NOT NULL,
    dropoff_longitude DOUBLE NOT NULL,
    dropoff_address VARCHAR(255),
    estimated_fare DECIMAL(10, 2) NOT NULL,
    final_fare DECIMAL(10, 2),
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_rides_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_rides_passenger_id ON rides(passenger_id);
CREATE INDEX idx_rides_status ON rides(status);
