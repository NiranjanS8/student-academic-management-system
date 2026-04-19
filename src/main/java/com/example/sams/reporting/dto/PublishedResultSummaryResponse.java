package com.example.sams.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublishedResultSummaryResponse(
        long publishedExamCount,
        long offeringsWithPublishedResults,
        long finalResultsCount,
        long partialResultsCount,
        BigDecimal averageWeightedScore,
        List<OfferingResultSummary> offerings
) {
    public record OfferingResultSummary(
            Long courseOfferingId,
            Long termId,
            String termName,
            String academicYear,
            String subjectCode,
            String subjectName,
            long publishedExamCount,
            long enrolledStudents,
            long studentsWithPublishedResults,
            long finalResultsCount,
            BigDecimal averageWeightedScore
    ) {
    }
}
