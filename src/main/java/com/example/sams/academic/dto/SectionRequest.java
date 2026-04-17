package com.example.sams.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SectionRequest(
        @NotBlank(message = "name is required")
        @Size(max = 50, message = "name must be at most 50 characters")
        String name,
        @NotNull(message = "programId is required")
        Long programId,
        Long currentTermId
) {
}
