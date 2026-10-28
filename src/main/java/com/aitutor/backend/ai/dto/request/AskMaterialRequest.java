package com.aitutor.backend.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskMaterialRequest(
        @NotBlank @Size(max = 1000) String question
) {
}
