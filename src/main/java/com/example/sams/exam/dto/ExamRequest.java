package com.example.sams.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ExamRequest(
        @NotNull(message = "courseOfferingId is required")
        Long courseOfferingId,
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "examType is required")
        String examType,
        @NotNull(message = "maxMarks is required")
        @DecimalMin(value = "0.01", message = "maxMarks must be greater than 0")
        BigDecimal maxMarks,
        @NotNull(message = "weightage is required")
        @DecimalMin(value = "0.01", message = "weightage must be greater than 0")
        @DecimalMax(value = "100.00", message = "weightage must be less than or equal to 100")
        BigDecimal weightage,
        Instant scheduledAt
) {
}
