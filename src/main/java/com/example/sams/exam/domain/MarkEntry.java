package com.example.sams.exam.domain;

import com.example.sams.common.entity.BaseEntity;
import com.example.sams.user.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mark_entries")
public class MarkEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marks_obtained", nullable = false, precision = 6, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "percentage_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageScore;

    @Column(name = "weighted_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "letter_grade", nullable = false, length = 5)
    private String letterGrade;

    @Column(name = "grade_points", nullable = false, precision = 3, scale = 2)
    private BigDecimal gradePoints;

    @Column(length = 255)
    private String remarks;
}
