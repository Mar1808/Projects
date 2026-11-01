CREATE TABLE profile_photo (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0
);

INSERT INTO profile_photo (code, label, image_url, enabled, display_order) VALUES
    ('warm_orange', 'Ambre', '/images/avatars/avatar-1.svg', TRUE, 1),
    ('soft_blue', 'Bleu ciel', '/images/avatars/avatar-2.svg', TRUE, 2),
    ('mint_green', 'Menthe', '/images/avatars/avatar-3.svg', TRUE, 3),
    ('rose_red', 'Rose', '/images/avatars/avatar-4.svg', TRUE, 4),
    ('violet_pop', 'Violet', '/images/avatars/avatar-5.svg', TRUE, 5),
    ('forest_night', 'Foret', '/images/avatars/avatar-6.svg', TRUE, 6);

ALTER TABLE app_user
    ADD COLUMN profile_photo_id BIGINT;

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_profile_photo
        FOREIGN KEY (profile_photo_id)
            REFERENCES profile_photo(id)
            ON DELETE SET NULL;

UPDATE app_user
SET profile_photo_id = (
    SELECT id
    FROM profile_photo
    WHERE enabled = TRUE
    ORDER BY display_order ASC, id ASC
    LIMIT 1
),
    image_url = COALESCE(image_url, (
        SELECT image_url
        FROM profile_photo
        WHERE enabled = TRUE
        ORDER BY display_order ASC, id ASC
        LIMIT 1
    ))
WHERE profile_photo_id IS NULL
  AND (image_url IS NULL OR image_url = '');
