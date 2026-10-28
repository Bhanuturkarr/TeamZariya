package com.aitutor.backend.assessment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitAssessmentRequest(
        @NotEmpty List<@NotNull @Valid SubmittedAnswerRequest> answers
) {
    public record SubmittedAnswerRequest(
            @NotNull UUID questionId,
            @NotNull @Min(0) @Max(3) Integer selectedOptionIndex
    ) {
    }
}
