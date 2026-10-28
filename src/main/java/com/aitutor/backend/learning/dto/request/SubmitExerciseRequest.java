package com.aitutor.backend.learning.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitExerciseRequest(
        @NotNull @Min(0) @Max(3) Integer selectedOptionIndex
) {
}
