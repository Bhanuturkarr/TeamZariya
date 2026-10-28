package com.aitutor.backend.progress.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectProgressResponse(
        UUID projectId,
        int topicsTracked,
        int averageMasteryScore,
        int masteredTopics,
        int topicsNeedingReview,
        int completedPlanItems,
        int totalPlanItems,
        int assessmentAttempts,
        int exerciseAttempts,
        String currentPlanStatus,
        Instant lastActivityAt
) { }
