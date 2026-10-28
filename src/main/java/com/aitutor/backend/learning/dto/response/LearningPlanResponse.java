package com.aitutor.backend.learning.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningPlanResponse(
        UUID id,
        UUID projectId,
        UUID materialId,
        UUID assessmentId,
        UUID assessmentAttemptId,
        String status,
        int availableStudyMinutes,
        String learningPreference,
        int masteryThreshold,
        int reviewThreshold,
        Instant createdAt,
        List<LearningPlanItemResponse> items
) {
}
