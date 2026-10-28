package com.aitutor.backend.dto.response;

import java.util.List;

public record MaterialDetailResponse(
        MaterialResponse material,
        List<DocumentChunkResponse> chunks
) {
}
