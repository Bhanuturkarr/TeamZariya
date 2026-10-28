package com.aitutor.backend.assessment.dto.response;

import java.util.List;
import java.util.UUID;

public record AssessmentQuestionFeedbackResponse(
        UUID questionId,
        String topic,
        String question,
        List<String> options,
        int selectedOptionIndex,
        boolean correct,
        int correctOptionIndex,
        String correctOption,
        String explanation,
        int sourcePage
) {
}
