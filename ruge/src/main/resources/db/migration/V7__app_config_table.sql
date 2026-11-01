CREATE TABLE app_configuration (
   id INT PRIMARY KEY CHECK (id = 1),
   ai_search_price DECIMAL(19,2) NOT NULL,
   super_like_price DECIMAL(19,2) NOT NULL
);

INSERT INTO app_configuration (id, ai_search_price, super_like_price)
VALUES (1, 0.10, 1);