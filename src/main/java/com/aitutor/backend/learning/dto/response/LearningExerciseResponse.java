package com.aitutor.backend.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record LearningExerciseResponse(
        UUID id,
        String question,
        List<String> options,
        String difficulty,
        int sourcePage
) {
}
