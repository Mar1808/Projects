ALTER TABLE transaction
    ALTER COLUMN date TYPE TIMESTAMP(3)
    USING (date::timestamp + LOCALTIME(3));

ALTER TABLE follow
    ALTER COLUMN date_follow TYPE TIMESTAMP(3)
    USING (date_follow::timestamp + LOCALTIME(3));

ALTER TABLE follow_recipe
    ALTER COLUMN date TYPE TIMESTAMP(3)
    USING (date::timestamp + LOCALTIME(3));

ALTER TABLE liker
    ALTER COLUMN date TYPE TIMESTAMP(3)
    USING (date::timestamp + LOCALTIME(3));

ALTER TABLE super_like
    ALTER COLUMN date TYPE TIMESTAMP(3)
    USING (date::timestamp + LOCALTIME(3));

ALTER TABLE promotion
    ALTER COLUMN date_start TYPE TIMESTAMP(3)
    USING (date_start::timestamp + LOCALTIME(3)),
    ALTER COLUMN date_end TYPE TIMESTAMP(3)
    USING (date_end::timestamp + LOCALTIME(3));

ALTER TABLE search_log
    ALTER COLUMN date TYPE TIMESTAMP(3)
    USING (date::timestamp + LOCALTIME(3));

ALTER TABLE recipe_review
    ALTER COLUMN date_review TYPE TIMESTAMP(3)
    USING (date_review::timestamp + LOCALTIME(3));
