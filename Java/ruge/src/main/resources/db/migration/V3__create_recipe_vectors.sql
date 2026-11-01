CREATE TABLE recipe_ai (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           recipe_id BIGINT NOT NULL,
    -- Le texte de la recette (ou une partie de celle-ci) que l'on veut vectoriser
                           content TEXT NOT NULL,
    -- Le vecteur mathématique de 1024 dimensions (spécifique à mxbai-embed-large)
                           embedding vector(1024)
);

-- Création d'un index pour rendre la recherche de similarité plus rapide
CREATE INDEX recipe_ai_embedding_idx ON recipe_ai USING hnsw (embedding vector_cosine_ops);