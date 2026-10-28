package com.aitutor.backend.assessment.dto.response;

import java.util.List;
import java.util.UUID;

public record AssessmentQuestionResponse(
        UUID id,
        int order,
        String topic,
        String question,
        List<String> options,
        String difficulty,
        int sourcePage
) {
}
