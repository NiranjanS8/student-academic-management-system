package com.example.sams.reporting.service;

import com.example.sams.attendance.service.AttendanceAnalyticsService;
import com.example.sams.common.api.PageResponse;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.exam.service.ResultCalculationService;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.fee.service.FeePolicyService;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.dto.PublishedResultSummaryResponse;
import com.example.sams.reporting.dto.StudentAcademicSnapshotResponse;
import com.example.sams.user.domain.Student;
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
    private final MarkEntryRepository markEntryRepository;
    private final ResultCalculationService resultCalculationService;
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
            MarkEntryRepository markEntryRepository,
            ResultCalculationService resultCalculationService,
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
        this.markEntryRepository = markEntryRepository;
        this.resultCalculationService = resultCalculationService;
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

    @Transactional(readOnly = true)
    public PublishedResultSummaryResponse getPublishedResultSummary(Long termId, Long courseOfferingId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findAdminPublishedResultEnrollments(null, termId, courseOfferingId, Pageable.unpaged())
                .getContent();

        List<StudentResultHistoryResponse> courseResults = enrollments.stream()
                .map(this::toCourseResult)
                .toList();

        long finalResultsCount = courseResults.stream()
                .filter(result -> "FINAL".equals(result.resultStatus()))
                .count();
        long partialResultsCount = courseResults.size() - finalResultsCount;
        BigDecimal averageWeightedScore = averageWeightedScore(courseResults);

        List<PublishedResultSummaryResponse.OfferingResultSummary> offerings = courseResults.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        StudentResultHistoryResponse::courseOfferingId,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ))
                .values()
                .stream()
                .map(results -> {
                    StudentResultHistoryResponse anchor = results.get(0);
                    long publishedExamCount = results.stream()
                            .flatMap(result -> result.publishedExams().stream())
                            .map(StudentResultHistoryResponse.PublishedExamResult::examId)
                            .distinct()
                            .count();
                    return new PublishedResultSummaryResponse.OfferingResultSummary(
                            anchor.courseOfferingId(),
                            anchor.termId(),
                            anchor.termName(),
                            anchor.academicYear(),
                            anchor.subjectCode(),
                            anchor.subjectName(),
                            publishedExamCount,
                            enrollments.stream().filter(enrollment -> enrollment.getCourseOffering().getId().equals(anchor.courseOfferingId())).count(),
                            results.size(),
                            results.stream().filter(result -> "FINAL".equals(result.resultStatus())).count(),
                            averageWeightedScore(results)
                    );
                })
                .toList();

        return new PublishedResultSummaryResponse(
                offerings.stream().mapToLong(PublishedResultSummaryResponse.OfferingResultSummary::publishedExamCount).sum(),
                offerings.size(),
                finalResultsCount,
                partialResultsCount,
                averageWeightedScore,
                offerings
        );
    }

    @Transactional(readOnly = true)
    public StudentAcademicSnapshotResponse getStudentAcademicSnapshot(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new com.example.sams.common.exception.ResourceNotFoundException("Student not found"));

        List<StudentResultHistoryResponse> publishedResults = enrollmentRepository
                .findAdminPublishedResultEnrollments(studentId, null, null, Pageable.unpaged())
                .stream()
                .map(this::toCourseResult)
                .toList();

        StudentResultSummaryResponse summary = resultCalculationService.buildSummary(publishedResults);

        return new StudentAcademicSnapshotResponse(
                student.getId(),
                student.getStudentCode(),
                student.getUser().getUsername(),
                student.getProgram().getName(),
                student.getSection() == null ? null : student.getSection().getName(),
                student.getCurrentTerm() == null ? null : student.getCurrentTerm().getName(),
                summary,
                publishedResults
        );
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

    private StudentResultHistoryResponse toCourseResult(Enrollment enrollment) {
        List<MarkEntry> publishedMarkEntries = markEntryRepository.findAllByStudentIdAndExamCourseOfferingIdAndExamPublishedTrue(
                enrollment.getStudent().getId(),
                enrollment.getCourseOffering().getId()
        );
        return resultCalculationService.buildCourseResult(enrollment, publishedMarkEntries);
    }

    private BigDecimal averageWeightedScore(List<StudentResultHistoryResponse> results) {
        if (results.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return results.stream()
                .map(StudentResultHistoryResponse::totalWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(results.size()), 2, RoundingMode.HALF_UP);
    }
}
