CREATE TABLE IF NOT EXISTS reporting_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT,
    trace_id VARCHAR(64),
    payload_json TEXT,
    occurred_at TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reporting_event_id UNIQUE (event_id)
);

CREATE TABLE IF NOT EXISTS daily_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE NOT NULL,
    total_rides_requested INT NOT NULL DEFAULT 0,
    total_rides_started INT NOT NULL DEFAULT 0,
    total_rides_completed INT NOT NULL DEFAULT 0,
    total_rides_cancelled INT NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_metrics_date UNIQUE (report_date)
);

CREATE TABLE IF NOT EXISTS driver_daily_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    completed_trips INT NOT NULL DEFAULT 0,
    total_earnings DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_driver_performance_date UNIQUE (driver_id, report_date)
);

CREATE INDEX idx_reporting_events_type ON reporting_events(event_type);
CREATE INDEX idx_driver_performance_driver ON driver_daily_performance(driver_id);
