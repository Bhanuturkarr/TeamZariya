package com.aitutor.backend.assessment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentAttemptResultResponse(
        UUID attemptId,
        int attemptNumber,
        int scorePercent,
        int correctAnswers,
        int totalQuestions,
        Instant submittedAt,
        List<TopicAssessmentScoreResponse> topicScores,
        List<AssessmentQuestionFeedbackResponse> feedback
) {
}
