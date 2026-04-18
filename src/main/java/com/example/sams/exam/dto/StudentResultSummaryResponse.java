package com.example.sams.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentResultSummaryResponse(
        BigDecimal cgpa,
        BigDecimal totalCompletedCredits,
        List<TermSummary> terms
) {
    public record TermSummary(
            Long termId,
            String termName,
            String academicYear,
            BigDecimal gpa,
            BigDecimal completedCredits,
            int completedCourses
    ) {
    }
}
