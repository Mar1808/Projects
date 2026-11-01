ALTER TABLE app_configuration
    ADD COLUMN IF NOT EXISTS terms_content TEXT;
