package com.aitutor.backend.ai.dto.response;

import java.util.UUID;

public record SourceReferenceResponse(
        UUID chunkId,
        int chunkIndex,
        int pageNumber,
        String excerpt
) {
}
