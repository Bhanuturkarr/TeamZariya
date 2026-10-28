package com.aitutor.backend.ai.dto.response;

import java.util.List;

public record AiQuestionResponse(
        String question,
        List<String> options,
        String difficulty,
        String topic,
        int sourcePage
) {
}
