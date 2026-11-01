ALTER TABLE recipe_review
    DROP CONSTRAINT IF EXISTS uk_recipe_review;

CREATE INDEX IF NOT EXISTS idx_recipe_review_user_date
    ON recipe_review(user_id, date_review DESC, id DESC);
