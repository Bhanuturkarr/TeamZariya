package com.aitutor.backend.dto.response;

import com.aitutor.backend.document.TopicExtractionMethod;

import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        int sourcePage,
        TopicExtractionMethod extractionMethod
) {
}
