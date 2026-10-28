package com.aitutor.backend.ai.dto.response;

import java.time.Instant;
import java.util.List;

public record AiQuestionsResponse(
        String provider,
        String retrievalMode,
        List<AiQuestionResponse> questions,
        Instant generatedAt
) {
}
