package com.aitutor.backend.progress.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectActivityEvent(
        Instant occurredAt,
        String type,
        String title,
        Boolean correct,
        Integer scorePercent,
        Integer attemptNumber,
        UUID resourceId
) { }
