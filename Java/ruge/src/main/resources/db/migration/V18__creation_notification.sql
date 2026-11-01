CREATE TABLE global_notification (
                                     id BIGSERIAL PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL,
                                     message VARCHAR(1000) NOT NULL,
                                     created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_created_at ON global_notification(created_at DESC);