package com.aitutor.backend.ai.dto.response;

import java.time.Instant;
import java.util.List;

public record AiTopicsResponse(
        String provider,
        String retrievalMode,
        List<AiTopicResponse> topics,
        Instant generatedAt
) {
}
