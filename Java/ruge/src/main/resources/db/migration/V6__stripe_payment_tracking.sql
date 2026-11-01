ALTER TABLE transaction
    ADD COLUMN provider VARCHAR(50),
    ADD COLUMN provider_payment_id VARCHAR(255);

CREATE UNIQUE INDEX uk_tx_provider_payment
    ON transaction(provider, provider_payment_id);
