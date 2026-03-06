```sql
CREATE
EXTENSION vector;
CREATE TABLE rag_chunks(
    chunk_id      TEXT PRIMARY KEY,
    document_id   TEXT,
    content       TEXT,
    embedding     VECTOR(1536),
    metadata      JSONB,
    last_modified TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_rag_chunks_metadata ON rag_chunks
    USING GIN (metadata jsonb_path_ops);
CREATE INDEX idx_rag_chunks_document ON rag_chunks (document_id);
CREATE INDEX idx_rag_chunks_embedding_hnsw ON rag_chunks USING hnsw (embedding vector_cosine_ops);

select *
from rag_chunks;

ALTER TABLE public.rag_chunks
    ADD COLUMN id TEXT;

UPDATE public.rag_chunks
SET id = chunk_id
where 1 = 1;
```