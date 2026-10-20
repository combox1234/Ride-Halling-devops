CREATE TABLE IF NOT EXISTS pricing_zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_code VARCHAR(50) NOT NULL,
    zone_name VARCHAR(100) NOT NULL,
    base_fare DECIMAL(10, 2) NOT NULL,
    per_km_rate DECIMAL(10, 2) NOT NULL,
    per_minute_rate DECIMAL(10, 2) NOT NULL,
    surge_multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
    min_fare DECIMAL(10, 2) NOT NULL DEFAULT 5.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_pricing_zones_code UNIQUE (zone_code)
);

INSERT INTO pricing_zones (zone_code, zone_name, base_fare, per_km_rate, per_minute_rate, surge_multiplier, min_fare, active)
VALUES 
('DEFAULT', 'Standard City Zone', 5.00, 2.50, 0.35, 1.00, 5.00, TRUE),
('DOWNTOWN', 'Downtown Metro Core', 6.00, 3.00, 0.50, 1.25, 7.00, TRUE),
('AIRPORT', 'International Airport Zone', 10.00, 3.50, 0.60, 1.50, 15.00, TRUE),
('SUBURBAN', 'Suburban Transit Zone', 4.50, 2.00, 0.25, 1.00, 5.00, TRUE);
