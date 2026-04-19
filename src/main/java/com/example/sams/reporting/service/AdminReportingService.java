package com.example.sams.reporting.service;

import com.example.sams.common.exception.ConflictException;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.StudentResultHistoryResponse;
import com.example.sams.exam.dto.StudentResultSummaryResponse;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.exam.service.ResultCalculationService;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.fee.service.FeePolicyService;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AdminStudentAnalyticsSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.dto.PublishedResultSummaryResponse;
import com.example.sams.reporting.dto.StudentAcademicSnapshotResponse;
import com.example.sams.reporting.dto.StudentDistributionReportResponse;
import com.example.sams.reporting.dto.TeacherWorkloadReportResponse;
import com.example.sams.reporting.projection.AdminDashboardMetricsProjection;
import com.example.sams.reporting.projection.AttendanceShortageReportProjection;
import com.example.sams.reporting.projection.CountBreakdownProjection;
import com.example.sams.reporting.projection.StudentDistributionProjection;
import com.example.sams.reporting.projection.TeacherWorkloadProjection;
import com.example.sams.reporting.repository.AdminReportingRepository;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportingService {

    private final StudentRepository studentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MarkEntryRepository markEntryRepository;
    private final ResultCalculationService resultCalculationService;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeePolicyService feePolicyService;
    private final AdminReportingRepository adminReportingRepository;
    private final BigDecimal minimumAttendancePercentage;

    public AdminReportingService(
            StudentRepository studentRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            MarkEntryRepository markEntryRepository,
            ResultCalculationService resultCalculationService,
            SemesterFeeRepository semesterFeeRepository,
            FeePolicyService feePolicyService,
            AdminReportingRepository adminReportingRepository,
            @Value("${sams.scheduler.attendance-shortage.minimum-percentage:75}") BigDecimal minimumAttendancePercentage
    ) {
        this.studentRepository = studentRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.markEntryRepository = markEntryRepository;
        this.resultCalculationService = resultCalculationService;
        this.semesterFeeRepository = semesterFeeRepository;
        this.feePolicyService = feePolicyService;
        this.adminReportingRepository = adminReportingRepository;
        this.minimumAttendancePercentage = minimumAttendancePercentage;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getDashboardSummary() {
        AdminDashboardMetricsProjection metrics = adminReportingRepository.fetchDashboardMetrics(minimumAttendancePercentage);
        return new AdminDashboardSummaryResponse(
                metrics.getTotalStudents(),
                metrics.getTotalTeachers(),
                metrics.getTotalOfferings(),
                metrics.getActiveEnrollments(),
                metrics.getPublishedResults(),
                metrics.getStudentsWithOutstandingDues(),
                metrics.getLowAttendanceCases()
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
        SortRequest sort = resolveSort(pageable, "studentId");
        return enrollmentRepository.findAttendanceShortageReports(
                        termId,
                        programId,
                        sectionId,
                        normalize(query),
                        minimumAttendancePercentage,
                        sort.property(),
                        sort.direction(),
                        pageRequestWithoutSort(pageable)
                )
                .map(this::toAttendanceShortageResponse);
    }

    @Transactional(readOnly = true)
    public PublishedResultSummaryResponse getPublishedResultSummary(Long termId, Long courseOfferingId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findAdminPublishedResultEnrollments(null, termId, courseOfferingId, Pageable.unpaged())
                .getContent();

        Map<ResultRowKey, List<MarkEntry>> markEntriesByEnrollment = loadPublishedMarkEntriesByEnrollment(enrollments);
        List<StudentResultHistoryResponse> courseResults = enrollments.stream()
                .map(enrollment -> toCourseResult(enrollment, markEntriesByEnrollment))
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
                            results.size(),
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

    @Transactional(readOnly = true)
    public Page<StudentDistributionReportResponse> getStudentDistribution(
            Long departmentId,
            Long programId,
            Long termId,
            Long sectionId,
            String academicStatus,
            Pageable pageable
    ) {
        AcademicStatus parsedStatus = parseAcademicStatus(academicStatus);
        SortRequest sort = resolveSort(pageable, "studentCount");
        return studentRepository.findStudentDistributionRows(
                        departmentId,
                        programId,
                        termId,
                        sectionId,
                        parsedStatus == null ? null : parsedStatus.name(),
                        sort.property(),
                        sort.direction(),
                        pageRequestWithoutSort(pageable)
                )
                .map(this::toStudentDistributionResponse);
    }

    @Transactional(readOnly = true)
    public AdminStudentAnalyticsSummaryResponse getStudentAnalyticsSummary() {
        return new AdminStudentAnalyticsSummaryResponse(
                studentRepository.count(),
                studentRepository.countByAcademicStatus(AcademicStatus.ACTIVE),
                studentRepository.countByAcademicStatus(AcademicStatus.ON_HOLD),
                studentRepository.countByAcademicStatus(AcademicStatus.GRADUATED),
                studentRepository.countByAcademicStatus(AcademicStatus.DROPPED),
                studentRepository.countBySectionIsNull(),
                studentRepository.countByCurrentTermIsNull(),
                toCountBreakdowns(studentRepository.findDepartmentBreakdown()),
                toCountBreakdowns(studentRepository.findProgramBreakdown()),
                toCountBreakdowns(studentRepository.findTermBreakdown())
        );
    }

    @Transactional(readOnly = true)
    public Page<TeacherWorkloadReportResponse> getTeacherWorkloads(Long departmentId, Long termId, String query, Pageable pageable) {
        SortRequest sort = resolveSort(pageable, "employeeCode");
        return courseOfferingRepository.findTeacherWorkloadRows(
                        departmentId,
                        termId,
                        normalize(query),
                        sort.property(),
                        sort.direction(),
                        pageRequestWithoutSort(pageable)
                )
                .map(this::toTeacherWorkloadResponse);
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

    private AttendanceShortageReportResponse toAttendanceShortageResponse(AttendanceShortageReportProjection row) {
        return new AttendanceShortageReportResponse(
                row.getStudentId(),
                row.getStudentCode(),
                row.getStudentUsername(),
                row.getCourseOfferingId(),
                row.getSubjectCode(),
                row.getSubjectName(),
                row.getTotalSessions(),
                row.getPresentSessions(),
                row.getAttendancePercentage()
        );
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

    private StudentResultHistoryResponse toCourseResult(Enrollment enrollment, Map<ResultRowKey, List<MarkEntry>> markEntriesByEnrollment) {
        return resultCalculationService.buildCourseResult(
                enrollment,
                markEntriesByEnrollment.getOrDefault(
                        new ResultRowKey(enrollment.getStudent().getId(), enrollment.getCourseOffering().getId()),
                        List.of()
                )
        );
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

    private TeacherWorkloadReportResponse toTeacherWorkloadResponse(TeacherWorkloadProjection projection) {
        return new TeacherWorkloadReportResponse(
                projection.getTeacherId(),
                projection.getEmployeeCode(),
                projection.getTeacherUsername(),
                projection.getTeacherEmail(),
                projection.getDepartmentName(),
                projection.getDesignation(),
                projection.getTotalOfferings(),
                projection.getOpenOfferings(),
                projection.getClosedOfferings(),
                projection.getArchivedOfferings(),
                projection.getTotalAssignedStudents(),
                projection.getPublishedExamCount(),
                projection.getAverageCapacityUtilization()
        );
    }

    private StudentDistributionReportResponse toStudentDistributionResponse(StudentDistributionProjection row) {
        return new StudentDistributionReportResponse(
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getProgramId(),
                row.getProgramName(),
                row.getTermId(),
                row.getTermName(),
                row.getAcademicYear(),
                row.getSectionId(),
                row.getSectionName(),
                row.getAcademicStatus(),
                row.getStudentCount()
        );
    }

    private List<AdminStudentAnalyticsSummaryResponse.CountBreakdown> toCountBreakdowns(List<CountBreakdownProjection> rows) {
        return rows.stream()
                .map(row -> new AdminStudentAnalyticsSummaryResponse.CountBreakdown(
                        row.getId(),
                        row.getName(),
                        row.getStudentCount()
                ))
                .sorted(Comparator.comparingLong(AdminStudentAnalyticsSummaryResponse.CountBreakdown::studentCount).reversed()
                        .thenComparing(AdminStudentAnalyticsSummaryResponse.CountBreakdown::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private AcademicStatus parseAcademicStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return AcademicStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid academicStatus. Allowed values: ACTIVE, ON_HOLD, GRADUATED, DROPPED");
        }
    }

    private Map<ResultRowKey, List<MarkEntry>> loadPublishedMarkEntriesByEnrollment(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return Map.of();
        }

        List<Long> studentIds = enrollments.stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .distinct()
                .toList();
        List<Long> offeringIds = enrollments.stream()
                .map(enrollment -> enrollment.getCourseOffering().getId())
                .distinct()
                .toList();

        return markEntryRepository.findAllByStudentIdInAndExamCourseOfferingIdInAndExamPublishedTrue(studentIds, offeringIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(markEntry -> new ResultRowKey(
                        markEntry.getStudent().getId(),
                        markEntry.getExam().getCourseOffering().getId()
                )));
    }

    private Pageable pageRequestWithoutSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private SortRequest resolveSort(Pageable pageable, String defaultProperty) {
        return pageable.getSort().stream()
                .findFirst()
                .map(order -> new SortRequest(order.getProperty(), order.isDescending() ? "desc" : "asc"))
                .orElseGet(() -> new SortRequest(defaultProperty, "asc"));
    }

    private record SortRequest(String property, String direction) {
    }

    private record ResultRowKey(Long studentId, Long courseOfferingId) {
    }
}
