package com.example.sams.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StudentResultHistoryResponse(
        Long courseOfferingId,
        Long termId,
        String termName,
        String academicYear,
        String subjectCode,
        String subjectName,
        BigDecimal credits,
        BigDecimal publishedWeightage,
        BigDecimal totalWeightedScore,
        String resultStatus,
        String finalLetterGrade,
        BigDecimal finalGradePoints,
        List<PublishedExamResult> publishedExams
) {
    public record PublishedExamResult(
            Long examId,
            String title,
            String examType,
            BigDecimal maxMarks,
            BigDecimal weightage,
            BigDecimal marksObtained,
            BigDecimal percentageScore,
            BigDecimal weightedScore,
            String letterGrade,
            BigDecimal gradePoints,
            Instant publishedAt
    ) {
    }
}
