package com.example.sams.exam.service;

import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResultCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final GradeCalculationService gradeCalculationService;

    public ResultCalculationService(GradeCalculationService gradeCalculationService) {
        this.gradeCalculationService = gradeCalculationService;
    }

    public StudentResultHistoryResponse buildCourseResult(Enrollment enrollment, List<MarkEntry> publishedMarkEntries) {
        List<MarkEntry> sortedEntries = publishedMarkEntries.stream()
                .sorted(Comparator
                        .comparing((MarkEntry entry) -> entry.getExam().getPublishedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(entry -> entry.getExam().getScheduledAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(entry -> entry.getExam().getId()))
                .toList();

        BigDecimal publishedWeightage = sortedEntries.stream()
                .map(entry -> entry.getExam().getWeightage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeightedScore = sortedEntries.stream()
                .map(MarkEntry::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        boolean finalResult = publishedWeightage.compareTo(HUNDRED) >= 0;
        String finalLetterGrade = null;
        BigDecimal finalGradePoints = null;

        if (finalResult) {
            GradeCalculationService.GradeSnapshot finalGrade = gradeCalculationService.resolveGradeFromPercentage(
                    totalWeightedScore.min(HUNDRED)
            );
            finalLetterGrade = finalGrade.letterGrade();
            finalGradePoints = finalGrade.gradePoints();
        }

        return new StudentResultHistoryResponse(
                enrollment.getCourseOffering().getId(),
                enrollment.getCourseOffering().getTerm().getId(),
                enrollment.getCourseOffering().getTerm().getName(),
                enrollment.getCourseOffering().getTerm().getAcademicYear(),
                enrollment.getCourseOffering().getSubject().getCode(),
                enrollment.getCourseOffering().getSubject().getName(),
                enrollment.getCourseOffering().getSubject().getCredits(),
                publishedWeightage.setScale(2, RoundingMode.HALF_UP),
                totalWeightedScore,
                finalResult ? "FINAL" : "PARTIAL",
                finalLetterGrade,
                finalGradePoints,
                sortedEntries.stream()
                        .map(entry -> new StudentResultHistoryResponse.PublishedExamResult(
                                entry.getExam().getId(),
                                entry.getExam().getTitle(),
                                entry.getExam().getExamType(),
                                entry.getExam().getMaxMarks(),
                                entry.getExam().getWeightage(),
                                entry.getMarksObtained(),
                                entry.getPercentageScore(),
                                entry.getWeightedScore(),
                                entry.getLetterGrade(),
                                entry.getGradePoints(),
                                entry.getExam().getPublishedAt()
                        ))
                        .toList()
        );
    }

    public StudentResultSummaryResponse buildSummary(List<StudentResultHistoryResponse> courseResults) {
        Map<Long, List<StudentResultHistoryResponse>> resultsByTerm = new LinkedHashMap<>();
        for (StudentResultHistoryResponse result : courseResults.stream()
                .sorted(Comparator.comparing(StudentResultHistoryResponse::termId))
                .toList()) {
            resultsByTerm.computeIfAbsent(result.termId(), unused -> new ArrayList<>()).add(result);
        }

        List<StudentResultSummaryResponse.TermSummary> termSummaries = new ArrayList<>();
        BigDecimal cumulativeCredits = BigDecimal.ZERO;
        BigDecimal cumulativeGradePoints = BigDecimal.ZERO;

        for (List<StudentResultHistoryResponse> termResults : resultsByTerm.values()) {
            List<StudentResultHistoryResponse> completedResults = termResults.stream()
                    .filter(result -> "FINAL".equals(result.resultStatus()))
                    .filter(result -> result.finalGradePoints() != null)
                    .toList();

            BigDecimal completedCredits = completedResults.stream()
                    .map(StudentResultHistoryResponse::credits)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalGradePoints = completedResults.stream()
                    .map(result -> result.finalGradePoints().multiply(result.credits()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal gpa = completedCredits.compareTo(BigDecimal.ZERO) > 0
                    ? totalGradePoints.divide(completedCredits, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            StudentResultHistoryResponse anchor = termResults.get(0);
            termSummaries.add(new StudentResultSummaryResponse.TermSummary(
                    anchor.termId(),
                    anchor.termName(),
                    anchor.academicYear(),
                    gpa,
                    completedCredits.setScale(2, RoundingMode.HALF_UP),
                    completedResults.size()
            ));

            cumulativeCredits = cumulativeCredits.add(completedCredits);
            cumulativeGradePoints = cumulativeGradePoints.add(totalGradePoints);
        }

        BigDecimal cgpa = cumulativeCredits.compareTo(BigDecimal.ZERO) > 0
                ? cumulativeGradePoints.divide(cumulativeCredits, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new StudentResultSummaryResponse(
                cgpa,
                cumulativeCredits.setScale(2, RoundingMode.HALF_UP),
                termSummaries
        );
    }
}
