package com.example.sams.exam;

import com.example.sams.exam.service.GradeCalculationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradeCalculationServiceTest {

    private final GradeCalculationService gradeCalculationService = new GradeCalculationService();

    @Test
    void calculateReturnsExpectedWeightedScoreAndGradeForStrongPerformance() {
        GradeCalculationService.GradeSnapshot gradeSnapshot = gradeCalculationService.calculate(
                new BigDecimal("84.50"),
                new BigDecimal("100.00"),
                new BigDecimal("30.00")
        );

        assertThat(gradeSnapshot.percentageScore()).isEqualByComparingTo("84.50");
        assertThat(gradeSnapshot.weightedScore()).isEqualByComparingTo("25.35");
        assertThat(gradeSnapshot.letterGrade()).isEqualTo("A");
        assertThat(gradeSnapshot.gradePoints()).isEqualByComparingTo("3.70");
    }

    @Test
    void calculateMapsBoundaryBandsConsistently() {
        assertThat(gradeCalculationService.calculate(
                new BigDecimal("90.00"),
                new BigDecimal("100.00"),
                new BigDecimal("40.00")
        ).letterGrade()).isEqualTo("A+");

        assertThat(gradeCalculationService.calculate(
                new BigDecimal("70.00"),
                new BigDecimal("100.00"),
                new BigDecimal("40.00")
        ).letterGrade()).isEqualTo("B+");

        GradeCalculationService.GradeSnapshot failingGrade = gradeCalculationService.calculate(
                new BigDecimal("39.99"),
                new BigDecimal("100.00"),
                new BigDecimal("40.00")
        );
        assertThat(failingGrade.letterGrade()).isEqualTo("F");
        assertThat(failingGrade.gradePoints()).isEqualByComparingTo("0.00");
        assertThat(failingGrade.weightedScore()).isEqualByComparingTo("16.00");
    }
}
