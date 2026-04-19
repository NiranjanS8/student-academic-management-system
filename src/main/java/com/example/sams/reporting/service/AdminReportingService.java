package com.example.sams.reporting.service;

import com.example.sams.attendance.service.AttendanceAnalyticsService;
import com.example.sams.common.api.PageResponse;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.fee.service.FeePolicyService;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportingService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeePolicyService feePolicyService;
    private final AttendanceAnalyticsService attendanceAnalyticsService;
    private final BigDecimal minimumAttendancePercentage;

    public AdminReportingService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            ExamRepository examRepository,
            SemesterFeeRepository semesterFeeRepository,
            FeePolicyService feePolicyService,
            AttendanceAnalyticsService attendanceAnalyticsService,
            @Value("${sams.scheduler.attendance-shortage.minimum-percentage:75}") BigDecimal minimumAttendancePercentage
    ) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examRepository = examRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.feePolicyService = feePolicyService;
        this.attendanceAnalyticsService = attendanceAnalyticsService;
        this.minimumAttendancePercentage = minimumAttendancePercentage;
    }

    @Transactional
    public AdminDashboardSummaryResponse getDashboardSummary() {
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalOfferings = courseOfferingRepository.count();
        long activeEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.ENROLLED);
        long publishedResults = examRepository.countByPublishedTrue();
        long studentsWithOutstandingDues = semesterFeeRepository.searchDefaulters(null, null, null, null, Pageable.unpaged())
                .map(feePolicyService::synchronizeFeeState)
                .stream()
                .map(semesterFee -> semesterFee.getStudent().getId())
                .distinct()
                .count();
        long lowAttendanceCases = attendanceAnalyticsService.findShortageCandidates(minimumAttendancePercentage).size();

        return new AdminDashboardSummaryResponse(
                totalStudents,
                totalTeachers,
                totalOfferings,
                activeEnrollments,
                publishedResults,
                studentsWithOutstandingDues,
                lowAttendanceCases
        );
    }

    @Transactional
    public Page<FeeDefaulterReportResponse> getFeeDefaulters(Long termId, Long programId, Long sectionId, String query, Pageable pageable) {
        return semesterFeeRepository.searchDefaulters(termId, programId, sectionId, normalize(query), pageable)
                .map(feePolicyService::synchronizeFeeState)
                .map(this::toFeeDefaulterResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceShortageReportResponse> getAttendanceShortages(
            Long termId,
            Long programId,
            Long sectionId,
            String query,
            Pageable pageable
    ) {
        List<AttendanceShortageReportResponse> content = enrollmentRepository.findAllByStatus(EnrollmentStatus.ENROLLED).stream()
                .filter(enrollment -> termId == null || enrollment.getCourseOffering().getTerm().getId().equals(termId))
                .filter(enrollment -> programId == null || enrollment.getStudent().getProgram().getId().equals(programId))
                .filter(enrollment -> sectionId == null || (enrollment.getStudent().getSection() != null
                        && enrollment.getStudent().getSection().getId().equals(sectionId)))
                .filter(enrollment -> matchesQuery(enrollment, query))
                .map(attendanceAnalyticsService::buildSummary)
                .filter(summary -> summary.totalSessions() > 0)
                .filter(summary -> summary.attendancePercentage().compareTo(minimumAttendancePercentage) < 0)
                .map(summary -> new AttendanceShortageReportResponse(
                        summary.studentId(),
                        enrollmentRepository.findByStudentIdAndCourseOfferingId(summary.studentId(), summary.courseOfferingId())
                                .map(Enrollment::getStudent)
                                .orElseThrow()
                                .getStudentCode(),
                        enrollmentRepository.findByStudentIdAndCourseOfferingId(summary.studentId(), summary.courseOfferingId())
                                .map(enrollment -> enrollment.getStudent().getUser().getUsername())
                                .orElseThrow(),
                        summary.courseOfferingId(),
                        summary.subjectCode(),
                        summary.subjectName(),
                        summary.totalSessions(),
                        summary.presentSessions(),
                        summary.attendancePercentage()
                ))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), content.size());
        List<AttendanceShortageReportResponse> pageContent = start >= content.size() ? List.of() : content.subList(start, end);
        return new PageImpl<>(pageContent, pageable, content.size());
    }

    private FeeDefaulterReportResponse toFeeDefaulterResponse(SemesterFee semesterFee) {
        BigDecimal outstandingAmount = semesterFee.getTotalPayable()
                .subtract(semesterFee.getPaidAmount())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new FeeDefaulterReportResponse(
                semesterFee.getId(),
                semesterFee.getStudent().getId(),
                semesterFee.getStudent().getStudentCode(),
                semesterFee.getStudent().getUser().getUsername(),
                semesterFee.getTerm().getId(),
                semesterFee.getTerm().getName(),
                semesterFee.getTerm().getAcademicYear(),
                semesterFee.getStatus().name(),
                semesterFee.getTotalPayable(),
                semesterFee.getPaidAmount(),
                outstandingAmount,
                semesterFee.getDueDate()
        );
    }

    private boolean matchesQuery(Enrollment enrollment, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase();
        return enrollment.getStudent().getStudentCode().toLowerCase().contains(normalized)
                || enrollment.getStudent().getUser().getUsername().toLowerCase().contains(normalized)
                || enrollment.getStudent().getUser().getEmail().toLowerCase().contains(normalized)
                || enrollment.getCourseOffering().getSubject().getCode().toLowerCase().contains(normalized)
                || enrollment.getCourseOffering().getSubject().getName().toLowerCase().contains(normalized);
    }

    private String normalize(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }
}
