package com.aitutor.backend.ai.dto.response;

import java.time.Instant;
import java.util.List;

public record AiAnswerResponse(
        String provider,
        String retrievalMode,
        String answer,
        List<SourceReferenceResponse> sources,
        Instant generatedAt
) {
}
