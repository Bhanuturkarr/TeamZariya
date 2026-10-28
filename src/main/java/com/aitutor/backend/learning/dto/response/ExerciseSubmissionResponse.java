package com.aitutor.backend.learning.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ExerciseSubmissionResponse(
        UUID attemptId,
        boolean correct,
        int correctOptionIndex,
        String correctOption,
        String explanation,
        int masteryBefore,
        int masteryAfter,
        String nextAction,
        String recommendation,
        String nextRecommendedTopic,
        UUID nextRecommendedLessonId,
        Instant submittedAt
) {
}
