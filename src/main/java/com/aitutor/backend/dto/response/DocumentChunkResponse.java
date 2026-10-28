package com.aitutor.backend.dto.response;

public record DocumentChunkResponse(
        int chunkIndex,
        int pageNumber,
        String content
) {
}
