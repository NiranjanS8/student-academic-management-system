package com.example.sams.enrollment.dto;

import com.example.sams.offering.dto.CourseOfferingResponse;
import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        String status,
        Instant enrolledAt,
        Instant droppedAt,
        CourseOfferingResponse courseOffering
) {
}
