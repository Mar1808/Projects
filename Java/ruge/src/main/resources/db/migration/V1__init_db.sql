-- =========================
-- APP_USER
-- =========================
CREATE TABLE app_user (
                          id BIGSERIAL PRIMARY KEY,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          name VARCHAR(255) NOT NULL,
                          password VARCHAR(255) NOT NULL,
                          role VARCHAR(50) NOT NULL,
                          image_url VARCHAR(255),
                          wallet_id BIGINT UNIQUE,
                          status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                          sso_provider VARCHAR(50),
                          sso_id VARCHAR(255),
                          CONSTRAINT uk_app_user_sso UNIQUE (sso_provider, sso_id)
);

-- =========================
-- WALLET
-- =========================
CREATE TABLE wallet (
                        id BIGSERIAL PRIMARY KEY,
                        money DECIMAL(19,2) NOT NULL DEFAULT 0,
                        type VARCHAR(50) NOT NULL
);

ALTER TABLE app_user
    ADD CONSTRAINT fk_user_wallet
        FOREIGN KEY (wallet_id)
            REFERENCES wallet(id)
            ON DELETE SET NULL;

-- =========================
-- FOLLOW
-- =========================
CREATE TABLE follow (
                        follower_id BIGINT NOT NULL,
                        followed_id BIGINT NOT NULL,
                        date_follow DATE NOT NULL,
                        follow_key VARCHAR(255),
                        PRIMARY KEY (follower_id, followed_id),
                        CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES app_user(id) ON DELETE CASCADE,
                        CONSTRAINT fk_follow_followed FOREIGN KEY (followed_id) REFERENCES app_user(id) ON DELETE CASCADE,
                        CONSTRAINT chk_follow_not_self CHECK (follower_id <> followed_id)
);

-- =========================
-- TRANSACTION
-- =========================
CREATE TABLE transaction (
                             id BIGSERIAL PRIMARY KEY,
                             source_wallet_id BIGINT NOT NULL,
                             destination_wallet_id BIGINT NOT NULL,
                             amount DECIMAL(19,2) NOT NULL,
                             description VARCHAR(1000),
                             reference_id BIGINT,
                             date DATE NOT NULL,
                             CONSTRAINT fk_tx_source FOREIGN KEY (source_wallet_id) REFERENCES wallet(id),
                             CONSTRAINT fk_tx_dest FOREIGN KEY (destination_wallet_id) REFERENCES wallet(id),
                             CONSTRAINT chk_tx_amount_positive CHECK (amount > 0),
                             CONSTRAINT chk_tx_wallets_distinct CHECK (source_wallet_id <> destination_wallet_id)
);

CREATE INDEX idx_tx_source_wallet ON transaction(source_wallet_id);
CREATE INDEX idx_tx_dest_wallet ON transaction(destination_wallet_id);
CREATE INDEX idx_tx_date ON transaction(date);

-- =========================
-- RECIPE
-- =========================
CREATE TABLE recipe (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(2000),
                        image_url VARCHAR(255),
                        creator_userid BIGINT,
                        cook_timing INT,
                        variant_id BIGINT,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        CONSTRAINT fk_recipe_variant FOREIGN KEY (variant_id) REFERENCES recipe(id) ON DELETE SET NULL,
                        CONSTRAINT fk_creator_id FOREIGN KEY (creator_userid) REFERENCES app_user(id) ON DELETE SET NULL
);

-- =========================
-- INGREDIENT
-- =========================
CREATE TABLE ingredient (
                            id BIGSERIAL PRIMARY KEY,
                            recipe_id BIGINT NOT NULL,
                            name VARCHAR(255) NOT NULL,
                            quantity VARCHAR(255),
                            CONSTRAINT fk_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

CREATE INDEX idx_ingredient_recipe ON ingredient(recipe_id);

-- =========================
-- INSTRUCTION
-- =========================
CREATE TABLE instruction (
                             id BIGSERIAL PRIMARY KEY,
                             recipe_id BIGINT NOT NULL,
                             step VARCHAR(2000) NOT NULL,
                             duration INT,
                             CONSTRAINT fk_instruction_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

CREATE INDEX idx_instruction_recipe ON instruction(recipe_id);

-- =========================
-- FOLLOW_RECIPE
-- =========================
CREATE TABLE follow_recipe (
                               user_id BIGINT NOT NULL,
                               recipe_id BIGINT NOT NULL,
                               date DATE NOT NULL,
                               PRIMARY KEY (user_id, recipe_id),
                               CONSTRAINT fk_follow_recipe_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                               CONSTRAINT fk_follow_recipe_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

-- =========================
-- LIKER
-- =========================
CREATE TABLE liker (
                       sender_id BIGINT NOT NULL,
                       receiver_id BIGINT NOT NULL,
                       recipe_id BIGINT NOT NULL,
                       date DATE NOT NULL,
                       PRIMARY KEY (sender_id, receiver_id, recipe_id),
                       CONSTRAINT fk_liker_sender FOREIGN KEY (sender_id) REFERENCES app_user(id) ON DELETE CASCADE,
                       CONSTRAINT fk_liker_receiver FOREIGN KEY (receiver_id) REFERENCES app_user(id) ON DELETE CASCADE,
                       CONSTRAINT fk_liker_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
                       CONSTRAINT chk_liker_not_self CHECK (sender_id <> receiver_id)
);

-- =========================
-- SUPER_LIKE
-- =========================
CREATE TABLE super_like (
                            id BIGSERIAL PRIMARY KEY,
                            sender_id BIGINT NOT NULL,
                            receiver_id BIGINT NOT NULL,
                            recipe_id BIGINT NOT NULL,
                            amount DECIMAL(19,2) NOT NULL,
                            is_redistributed BOOLEAN NOT NULL DEFAULT FALSE,
                            date DATE NOT NULL,
                            CONSTRAINT fk_superlike_sender FOREIGN KEY (sender_id) REFERENCES app_user(id) ON DELETE CASCADE,
                            CONSTRAINT fk_superlike_receiver FOREIGN KEY (receiver_id) REFERENCES app_user(id) ON DELETE CASCADE,
                            CONSTRAINT fk_superlike_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
                            CONSTRAINT chk_superlike_amount_positive CHECK (amount > 0),
                            CONSTRAINT chk_superlike_not_self CHECK (sender_id <> receiver_id)
);

CREATE INDEX idx_superlike_sender ON super_like(sender_id);
CREATE INDEX idx_superlike_receiver ON super_like(receiver_id);
CREATE INDEX idx_superlike_recipe ON super_like(recipe_id);

-- =========================
-- PROMOTION
-- =========================
CREATE TABLE promotion (
                           id BIGSERIAL PRIMARY KEY,
                           date_start DATE NOT NULL,
                           date_end DATE NOT NULL,
                           description VARCHAR(2000),
                           super_like_id BIGINT NOT NULL UNIQUE,
                           CONSTRAINT fk_promo_superlike FOREIGN KEY (super_like_id) REFERENCES super_like(id) ON DELETE CASCADE,
                           CONSTRAINT chk_promo_dates CHECK (date_end >= date_start)
);

-- =========================
-- SEARCH_LOG
-- =========================
CREATE TABLE search_log (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            followed_id BIGINT,
                            date DATE NOT NULL,
                            amount DECIMAL(19,2),
                            CONSTRAINT fk_searchlog_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_searchlog_user ON search_log(user_id);
CREATE INDEX idx_searchlog_date ON search_log(date);

-- =========================
-- RECIPE_REVIEW
-- =========================
CREATE TABLE recipe_review (
                               id BIGSERIAL PRIMARY KEY,
                               recipe_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               comment VARCHAR(1000) NOT NULL,
                               date_review DATE NOT NULL,
                               CONSTRAINT fk_recipe_review_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE,
                               CONSTRAINT fk_recipe_review_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                               CONSTRAINT uk_recipe_review UNIQUE (recipe_id, user_id)
);

CREATE INDEX idx_recipe_review_recipe ON recipe_review(recipe_id);

-- =========================
-- BAN_HISTORY
-- =========================
CREATE TABLE ban_history (
                             id BIGSERIAL PRIMARY KEY,
                             banned_user_id BIGINT NOT NULL,
                             banned_by_id BIGINT NOT NULL,
                             ban_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             reason VARCHAR(250) NOT NULL,
                             duration INT NOT NULL,
                             CONSTRAINT fk_ban_history_banned_user FOREIGN KEY (banned_user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                             CONSTRAINT fk_ban_history_banned_by FOREIGN KEY (banned_by_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- =========================
-- PASSWORD_RESET_TOKEN
-- =========================
CREATE TABLE password_reset_token (
                                      id BIGSERIAL PRIMARY KEY,
                                      token VARCHAR(255) NOT NULL UNIQUE,
                                      user_id BIGINT NOT NULL,
                                      expiry_date TIMESTAMP NOT NULL,
                                      CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
