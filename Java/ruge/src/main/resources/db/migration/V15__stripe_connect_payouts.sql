ALTER TABLE app_user
    ADD COLUMN stripe_connect_account_id VARCHAR(255);

CREATE UNIQUE INDEX uk_app_user_stripe_connect_account
    ON app_user(stripe_connect_account_id)
    WHERE stripe_connect_account_id IS NOT NULL;
