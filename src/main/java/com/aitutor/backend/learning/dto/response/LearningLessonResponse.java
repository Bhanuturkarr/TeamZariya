package com.aitutor.backend.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record LearningLessonResponse(
        UUID id,
        String topic,
        String title,
        String objective,
        String explanation,
        String workedExample,
        int sourcePage,
        boolean completed,
        List<LearningExerciseResponse> exercises
) {
}
