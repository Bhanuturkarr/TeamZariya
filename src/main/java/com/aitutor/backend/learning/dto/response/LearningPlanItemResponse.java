package com.aitutor.backend.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record LearningPlanItemResponse(
        UUID id,
        int sequence,
        String topic,
        int masteryScore,
        int estimatedMinutes,
        String rationale,
        String status,
        String prerequisiteMethod,
        List<String> prerequisites,
        LearningLessonResponse lesson
) {
}
