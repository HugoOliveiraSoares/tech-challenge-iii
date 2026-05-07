CREATE TABLE payment (
    payment_id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
