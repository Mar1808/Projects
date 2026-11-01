ALTER TABLE transaction
    ADD COLUMN transaction_type VARCHAR(50),
    ADD COLUMN source_recipe_id BIGINT,
    ADD COLUMN target_recipe_id BIGINT;

ALTER TABLE transaction
    ADD CONSTRAINT fk_tx_source_recipe
        FOREIGN KEY (source_recipe_id)
            REFERENCES recipe(id)
            ON DELETE SET NULL;

ALTER TABLE transaction
    ADD CONSTRAINT fk_tx_target_recipe
        FOREIGN KEY (target_recipe_id)
            REFERENCES recipe(id)
            ON DELETE SET NULL;

CREATE INDEX idx_tx_type ON transaction(transaction_type);
CREATE INDEX idx_tx_source_recipe ON transaction(source_recipe_id);
CREATE INDEX idx_tx_target_recipe ON transaction(target_recipe_id);
