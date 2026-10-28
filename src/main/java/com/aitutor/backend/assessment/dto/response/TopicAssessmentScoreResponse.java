package com.aitutor.backend.assessment.dto.response;

public record TopicAssessmentScoreResponse(
        String topic,
        int correctAnswers,
        int totalQuestions,
        int scorePercent,
        boolean needsReview
) {
}
