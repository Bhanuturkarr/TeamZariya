package com.aitutor.backend.ai.model;

import java.util.List;

public record AiQuestionDraft(
        String question,
        List<String> options,
        int correctOptionIndex,
        String explanation,
        String difficulty,
        int sourcePage
) {
}
