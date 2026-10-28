package com.aitutor.backend.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateQuestionsRequest(
        @NotBlank @Size(max = 120) String topic,
        @Min(1) @Max(5) int count
) {
}
