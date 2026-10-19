CREATE TABLE IF NOT EXISTS drivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    vehicle_make VARCHAR(50) NOT NULL,
    vehicle_model VARCHAR(50) NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
    rating DOUBLE NOT NULL DEFAULT 5.0,
    total_trips INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_drivers_user_id UNIQUE (user_id),
    CONSTRAINT uk_drivers_email UNIQUE (email),
    CONSTRAINT uk_drivers_license_plate UNIQUE (license_plate)
);
