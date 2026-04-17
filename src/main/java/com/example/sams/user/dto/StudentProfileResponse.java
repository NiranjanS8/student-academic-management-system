package com.example.sams.user.dto;

import com.example.sams.user.domain.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;

public record StudentProfileResponse(
        Long studentId,
        Long userId,
        String username,
        String email,
        AccountStatus accountStatus,
        String studentCode,
        DepartmentSummary department,
        ProgramSummary program,
        AcademicTermSummary currentTerm,
        SectionSummary section,
        String academicStatus,
        LocalDate admissionDate,
        Instant createdAt,
        Instant updatedAt
) {
    public record DepartmentSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record ProgramSummary(
            Long id,
            String code,
            String name
    ) {
    }

    public record AcademicTermSummary(
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
}
