CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE recipe_ai (
                           id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
                           content text,
                           metadata jsonb,
                           embedding vector(1024)
);

CREATE INDEX recipe_ai_embedding_idx ON recipe_ai USING hnsw (embedding vector_cosine_ops);