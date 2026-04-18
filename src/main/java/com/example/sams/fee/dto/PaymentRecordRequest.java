package com.example.sams.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRecordRequest(
        @NotBlank(message = "paymentReference is required")
        @Size(max = 100, message = "paymentReference must be at most 100 characters")
        String paymentReference,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,
        @NotBlank(message = "paymentMethod is required")
        @Size(max = 30, message = "paymentMethod must be at most 30 characters")
        String paymentMethod,
        @NotNull(message = "paidAt is required")
        Instant paidAt,
        @Size(max = 255, message = "remarks must be at most 255 characters")
        String remarks
) {
}
