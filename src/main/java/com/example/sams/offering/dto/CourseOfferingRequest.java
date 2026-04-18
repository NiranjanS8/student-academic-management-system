package com.example.sams.offering.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CourseOfferingRequest(
        @NotNull(message = "subjectId is required")
        Long subjectId,
        @NotNull(message = "termId is required")
        Long termId,
        @NotNull(message = "sectionId is required")
        Long sectionId,
        @NotNull(message = "teacherId is required")
        Long teacherId,
        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1")
        Integer capacity,
        Instant enrollmentOpenAt,
        Instant enrollmentCloseAt,
        @NotNull(message = "status is required")
        String status
) {
}
