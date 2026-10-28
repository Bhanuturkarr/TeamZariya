package com.aitutor.backend.ai.model;

import java.util.List;

public record AiTopicDraft(
        String name,
        String description,
        int sourcePage,
        List<String> prerequisites
) {
}
