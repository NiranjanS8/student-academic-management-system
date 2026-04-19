package com.example.sams.attendance.service;

import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.reporting.projection.AttendanceShortageReportProjection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAnalyticsService {

    private final EnrollmentRepository enrollmentRepository;

    public AttendanceAnalyticsService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AttendanceShortageCandidate> findShortageCandidates(BigDecimal minimumPercentage) {
        return enrollmentRepository.findAttendanceShortageCandidates(minimumPercentage).stream()
                .map(this::toCandidate)
                .toList();
    }

    private AttendanceShortageCandidate toCandidate(AttendanceShortageReportProjection row) {
        return new AttendanceShortageCandidate(
                row.getUserId(),
                row.getStudentId(),
                row.getCourseOfferingId(),
                row.getSubjectCode(),
                row.getSubjectName(),
                row.getTotalSessions(),
                row.getPresentSessions(),
                row.getAttendancePercentage()
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
