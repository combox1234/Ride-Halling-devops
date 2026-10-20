CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_ref VARCHAR(100),
    error_code VARCHAR(50),
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL,
    payment_transaction_id BIGINT NOT NULL,
    ride_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_ride UNIQUE (ride_id),
    CONSTRAINT fk_invoices_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id)
);

CREATE INDEX idx_payment_ride_id ON payment_transactions(ride_id);
CREATE INDEX idx_payment_passenger_id ON payment_transactions(passenger_id);
CREATE INDEX idx_invoices_passenger_id ON invoices(passenger_id);
