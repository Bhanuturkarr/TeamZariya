CREATE TABLE materials (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES learning_projects(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(2048) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED')),
    page_count INTEGER NOT NULL DEFAULT 0 CHECK (page_count >= 0),
    extracted_char_count INTEGER NOT NULL DEFAULT 0 CHECK (extracted_char_count >= 0),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_materials_project_created_at
    ON materials (project_id, created_at DESC);

CREATE INDEX idx_materials_status
    ON materials (status);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index > 0),
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    content TEXT NOT NULL,
    CONSTRAINT uq_document_chunks_material_index UNIQUE (material_id, chunk_index)
);

CREATE INDEX idx_document_chunks_material_page
    ON document_chunks (material_id, page_number, chunk_index);

CREATE TABLE topics (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    source_page INTEGER NOT NULL CHECK (source_page > 0),
    extraction_method VARCHAR(24) NOT NULL CHECK (extraction_method IN ('HEADING', 'KEYWORD', 'FALLBACK')),
    CONSTRAINT uq_topics_material_name UNIQUE (material_id, name)
);

CREATE INDEX idx_topics_material_name
    ON topics (material_id, name);
