ALTER TABLE super_like DROP CONSTRAINT IF EXISTS chk_superlike_amount_positive;
ALTER TABLE super_like ADD CONSTRAINT chk_superlike_amount_positive CHECK (amount >= 0);
ALTER TABLE transaction DROP CONSTRAINT IF EXISTS chk_tx_amount_positive;