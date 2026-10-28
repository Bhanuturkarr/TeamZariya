package com.aitutor.backend.progress.dto;

import java.time.Instant;

public record TopicMasteryResponse(
        String topic,
        int masteryScore,
        int attempts,
        int correctAnswers,
        boolean needsReview,
        boolean mastered,
        Instant updatedAt
) { }
