package com.aitutor.backend.ai.model;

import java.util.List;

public record RetrievalResult(String mode, List<RetrievedChunk> chunks) {

    public RetrievalResult {
        chunks = List.copyOf(chunks);
    }
}
