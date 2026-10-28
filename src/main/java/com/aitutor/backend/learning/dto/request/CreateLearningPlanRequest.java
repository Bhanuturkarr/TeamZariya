package com.aitutor.backend.learning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLearningPlanRequest(
        @NotNull UUID assessmentId,
        @Min(10) @Max(600) Integer availableStudyMinutes,
        @Size(max = 500) String learningPreference
) {
    public int effectiveStudyMinutes() {
        return availableStudyMinutes == null ? 60 : availableStudyMinutes;
    }
}
