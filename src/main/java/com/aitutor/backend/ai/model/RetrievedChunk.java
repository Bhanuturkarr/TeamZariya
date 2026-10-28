package com.aitutor.backend.ai.model;

import java.util.UUID;

public record RetrievedChunk(
        UUID id,
        int chunkIndex,
        int pageNumber,
        String content,
        double score
) {
}
