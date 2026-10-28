package com.aitutor.backend.assessment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentResponse(
        UUID id,
        UUID projectId,
        UUID materialId,
        String title,
        String provider,
        Instant createdAt,
        List<AssessmentQuestionResponse> questions
) {
}
