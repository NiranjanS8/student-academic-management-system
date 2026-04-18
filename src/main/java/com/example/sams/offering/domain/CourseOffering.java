package com.example.sams.offering.domain;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.Subject;
import com.example.sams.common.entity.BaseEntity;
import com.example.sams.user.domain.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "course_offerings")
public class CourseOffering extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "room_code", length = 50)
    private String roomCode;

    @Column(name = "schedule_days", length = 50)
    private String scheduleDays;

    @Column(name = "schedule_start_time")
    private LocalTime scheduleStartTime;

    @Column(name = "schedule_end_time")
    private LocalTime scheduleEndTime;

    @Column(name = "enrollment_open_at")
    private Instant enrollmentOpenAt;

    @Column(name = "enrollment_close_at")
    private Instant enrollmentCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseOfferingStatus status;
}
