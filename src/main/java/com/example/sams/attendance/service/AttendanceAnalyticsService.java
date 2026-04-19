package com.example.sams.attendance.service;

import com.example.sams.attendance.domain.AttendanceStatus;
import com.example.sams.attendance.repository.AttendanceRecordRepository;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAnalyticsService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AttendanceAnalyticsService(
            EnrollmentRepository enrollmentRepository,
            AttendanceRecordRepository attendanceRecordRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceShortageCandidate> findShortageCandidates(BigDecimal minimumPercentage) {
        return enrollmentRepository.findAllByStatus(EnrollmentStatus.ENROLLED).stream()
                .map(this::buildSummary)
                .filter(summary -> summary.totalSessions() > 0)
                .filter(summary -> summary.attendancePercentage().compareTo(minimumPercentage) < 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceShortageCandidate buildSummary(Enrollment enrollment) {
        Long offeringId = enrollment.getCourseOffering().getId();
        Long studentId = enrollment.getStudent().getId();
        long totalSessions = attendanceRecordRepository.countBySessionCourseOfferingIdAndStudentId(offeringId, studentId);
        long presentSessions = attendanceRecordRepository.countBySessionCourseOfferingIdAndStudentIdAndStatus(
                offeringId,
                studentId,
                AttendanceStatus.PRESENT
        );

        BigDecimal percentage = totalSessions == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(presentSessions)
                        .multiply(new BigDecimal("100.00"))
                        .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);

        return new AttendanceShortageCandidate(
                enrollment.getStudent().getUser().getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourseOffering().getId(),
                enrollment.getCourseOffering().getSubject().getCode(),
                enrollment.getCourseOffering().getSubject().getName(),
                totalSessions,
                presentSessions,
                percentage
        );
    }

    public record AttendanceShortageCandidate(
            Long userId,
            Long studentId,
            Long courseOfferingId,
            String subjectCode,
            String subjectName,
            long totalSessions,
            long presentSessions,
            BigDecimal attendancePercentage
    ) {
    }
}
