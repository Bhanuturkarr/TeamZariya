package com.aitutor.backend.ai.dto.response;

import java.util.List;

public record AiTopicResponse(
        String name,
        String description,
        int sourcePage,
        List<String> prerequisites
) {
}
