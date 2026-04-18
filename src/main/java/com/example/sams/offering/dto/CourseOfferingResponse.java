package com.example.sams.offering.dto;

import java.time.Instant;

public record CourseOfferingResponse(
        Long id,
        SubjectSummary subject,
        TermSummary term,
        SectionSummary section,
        TeacherSummary teacher,
        Integer capacity,
        Instant enrollmentOpenAt,
        Instant enrollmentCloseAt,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public record SubjectSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record TermSummary(
            Long id,
            String name,
            String academicYear,
            String status
    ) {
    }

    public record SectionSummary(
            Long id,
            String name
    ) {
    }

    public record TeacherSummary(
            Long id,
            String employeeCode,
            String username,
            String email
    ) {
    }
}
