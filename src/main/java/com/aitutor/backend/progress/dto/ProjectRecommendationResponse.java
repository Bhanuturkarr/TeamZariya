package com.aitutor.backend.progress.dto;

import java.util.UUID;

public record ProjectRecommendationResponse(
        UUID projectId,
        String action,
        String message,
        UUID planId,
        Integer sequence,
        String topic,
        Integer masteryScore,
        Integer estimatedMinutes,
        UUID lessonId,
        String lessonTitle,
        Integer sourcePage
) { }
