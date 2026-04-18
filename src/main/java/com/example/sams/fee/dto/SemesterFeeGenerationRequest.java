package com.example.sams.fee.dto;

import jakarta.validation.constraints.NotNull;

public record SemesterFeeGenerationRequest(
        @NotNull(message = "studentId is required")
        Long studentId,
        @NotNull(message = "termId is required")
        Long termId
) {
}
