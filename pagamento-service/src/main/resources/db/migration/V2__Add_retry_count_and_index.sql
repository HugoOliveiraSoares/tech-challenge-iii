ALTER TABLE payment ADD COLUMN retry_count INTEGER DEFAULT 0;
CREATE INDEX idx_payment_status_created_at ON payment(payment_status, created_at);
