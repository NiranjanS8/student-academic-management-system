package com.example.sams.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FeeStructureRequest(
        @NotNull(message = "programId is required")
        Long programId,
        @NotNull(message = "termId is required")
        Long termId,
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,
        @NotBlank(message = "feeCategory is required")
        @Size(max = 50, message = "feeCategory must be at most 50 characters")
        String feeCategory,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,
        @NotNull(message = "dueDaysFromTermStart is required")
        @Min(value = 0, message = "dueDaysFromTermStart cannot be negative")
        @Max(value = 365, message = "dueDaysFromTermStart must be at most 365")
        Integer dueDaysFromTermStart,
        @Size(max = 255, message = "description must be at most 255 characters")
        String description,
        @NotNull(message = "active is required")
        Boolean active
) {
}
