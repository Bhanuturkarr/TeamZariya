package com.aitutor.backend.assessment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDiagnosticAssessmentRequest(
        @NotNull UUID materialId,
        @Min(1) @Max(10) Integer questionCount
) {
    public int effectiveQuestionCount() {
        return questionCount == null ? 5 : questionCount;
    }
}
