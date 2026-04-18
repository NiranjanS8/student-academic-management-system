package com.example.sams.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MarkEntryRequest(
        @NotNull(message = "studentId is required")
        Long studentId,
        @NotNull(message = "marksObtained is required")
        @DecimalMin(value = "0.00", message = "marksObtained must be at least 0")
        BigDecimal marksObtained,
        String remarks
) {
}
