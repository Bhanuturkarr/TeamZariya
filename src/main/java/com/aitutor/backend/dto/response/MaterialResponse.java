package com.aitutor.backend.dto.response;

import com.aitutor.backend.document.MaterialStatus;

import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        UUID projectId,
        String originalFilename,
        long fileSizeBytes,
        MaterialStatus status,
        int pageCount,
        int extractedCharCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
