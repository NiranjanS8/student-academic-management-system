package com.example.sams.exam.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class GradeCalculationService {

    public GradeSnapshot calculate(BigDecimal marksObtained, BigDecimal maxMarks, BigDecimal examWeightage) {
        BigDecimal percentageScore = marksObtained
                .multiply(new BigDecimal("100"))
                .divide(maxMarks, 2, RoundingMode.HALF_UP);
        BigDecimal weightedScore = percentageScore
                .multiply(examWeightage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        GradeBand gradeBand = resolveGradeBand(percentageScore);
        return new GradeSnapshot(
                percentageScore,
                weightedScore,
                gradeBand.letterGrade(),
                gradeBand.gradePoints()
        );
    }

    public GradeSnapshot resolveGradeFromPercentage(BigDecimal percentageScore) {
        GradeBand gradeBand = resolveGradeBand(percentageScore);
        return new GradeSnapshot(
                percentageScore,
                percentageScore,
                gradeBand.letterGrade(),
                gradeBand.gradePoints()
        );
    }

    private GradeBand resolveGradeBand(BigDecimal percentageScore) {
        if (percentageScore.compareTo(new BigDecimal("90.00")) >= 0) {
            return new GradeBand("A+", new BigDecimal("4.00"));
        }
        if (percentageScore.compareTo(new BigDecimal("80.00")) >= 0) {
            return new GradeBand("A", new BigDecimal("3.70"));
        }
        if (percentageScore.compareTo(new BigDecimal("70.00")) >= 0) {
            return new GradeBand("B+", new BigDecimal("3.30"));
        }
        if (percentageScore.compareTo(new BigDecimal("60.00")) >= 0) {
            return new GradeBand("B", new BigDecimal("3.00"));
        }
        if (percentageScore.compareTo(new BigDecimal("50.00")) >= 0) {
            return new GradeBand("C", new BigDecimal("2.00"));
        }
        if (percentageScore.compareTo(new BigDecimal("40.00")) >= 0) {
            return new GradeBand("D", new BigDecimal("1.00"));
        }
        return new GradeBand("F", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    public record GradeSnapshot(
            BigDecimal percentageScore,
            BigDecimal weightedScore,
            String letterGrade,
            BigDecimal gradePoints
    ) {
    }

    private record GradeBand(String letterGrade, BigDecimal gradePoints) {
    }
}
