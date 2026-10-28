package com.aitutor.backend.assessment.dto.response;

import java.util.List;
import java.util.UUID;

public record AssessmentResultsResponse(
        UUID assessmentId,
        List<AssessmentAttemptResultResponse> attempts
) {
}
