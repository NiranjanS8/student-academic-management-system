package com.example.sams.reporting.service;

import com.example.sams.attendance.service.AttendanceAnalyticsService;
import com.example.sams.common.api.PageResponse;
import com.example.sams.common.exception.ConflictException;
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
import com.example.sams.reporting.dto.AdminStudentAnalyticsSummaryResponse;
import com.example.sams.reporting.dto.AdminDashboardSummaryResponse;
import com.example.sams.reporting.dto.AttendanceShortageReportResponse;
import com.example.sams.reporting.dto.FeeDefaulterReportResponse;
import com.example.sams.reporting.dto.PublishedResultSummaryResponse;
import com.example.sams.reporting.dto.StudentDistributionReportResponse;
import com.example.sams.reporting.dto.StudentAcademicSnapshotResponse;
import com.example.sams.reporting.dto.TeacherWorkloadReportResponse;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        List<StudentDistributionReportResponse> content = studentRepository.findAll().stream()
                .filter(student -> departmentId == null || student.getDepartment().getId().equals(departmentId))
                .filter(student -> programId == null || student.getProgram().getId().equals(programId))
                .filter(student -> termId == null || (student.getCurrentTerm() != null && student.getCurrentTerm().getId().equals(termId)))
                .filter(student -> sectionId == null || (student.getSection() != null && student.getSection().getId().equals(sectionId)))
                .filter(student -> parsedStatus == null || student.getAcademicStatus() == parsedStatus)
                .collect(java.util.stream.Collectors.groupingBy(
                        student -> new StudentDistributionKey(
                                student.getDepartment().getId(),
                                student.getDepartment().getName(),
                                student.getProgram().getId(),
                                student.getProgram().getName(),
                                student.getCurrentTerm() == null ? null : student.getCurrentTerm().getId(),
                                student.getCurrentTerm() == null ? "Unassigned" : student.getCurrentTerm().getName(),
                                student.getCurrentTerm() == null ? null : student.getCurrentTerm().getAcademicYear(),
                                student.getSection() == null ? null : student.getSection().getId(),
                                student.getSection() == null ? "Unassigned" : student.getSection().getName(),
                                student.getAcademicStatus().name()
                        ),
                        java.util.stream.Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new StudentDistributionReportResponse(
                        entry.getKey().departmentId(),
                        entry.getKey().departmentName(),
                        entry.getKey().programId(),
                        entry.getKey().programName(),
                        entry.getKey().termId(),
                        entry.getKey().termName(),
                        entry.getKey().academicYear(),
                        entry.getKey().sectionId(),
                        entry.getKey().sectionName(),
                        entry.getKey().academicStatus(),
                        entry.getValue()
                ))
                .sorted(studentDistributionComparator(pageable))
                .toList();

        return page(content, pageable);
    }

    @Transactional(readOnly = true)
    public AdminStudentAnalyticsSummaryResponse getStudentAnalyticsSummary() {
        List<Student> students = studentRepository.findAll();

        return new AdminStudentAnalyticsSummaryResponse(
                students.size(),
                students.stream().filter(student -> student.getAcademicStatus() == AcademicStatus.ACTIVE).count(),
                students.stream().filter(student -> student.getAcademicStatus() == AcademicStatus.ON_HOLD).count(),
                students.stream().filter(student -> student.getAcademicStatus() == AcademicStatus.GRADUATED).count(),
                students.stream().filter(student -> student.getAcademicStatus() == AcademicStatus.DROPPED).count(),
                students.stream().filter(student -> student.getSection() == null).count(),
                students.stream().filter(student -> student.getCurrentTerm() == null).count(),
                buildBreakdown(students, student -> student.getDepartment().getId(), student -> student.getDepartment().getName()),
                buildBreakdown(students, student -> student.getProgram().getId(), student -> student.getProgram().getName()),
                buildBreakdown(
                        students.stream().filter(student -> student.getCurrentTerm() != null).toList(),
                        student -> student.getCurrentTerm().getId(),
                        student -> "%s (%s)".formatted(student.getCurrentTerm().getName(), student.getCurrentTerm().getAcademicYear())
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<TeacherWorkloadReportResponse> getTeacherWorkloads(Long departmentId, Long termId, String query, Pageable pageable) {
        List<TeacherWorkloadReportResponse> content = teacherRepository.search(departmentId, normalize(query), Pageable.unpaged())
                .stream()
                .map(teacher -> buildTeacherWorkload(teacher, termId))
                .filter(report -> termId == null || report.totalOfferings() > 0)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), content.size());
        List<TeacherWorkloadReportResponse> pageContent = start >= content.size() ? List.of() : content.subList(start, end);
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

    private TeacherWorkloadReportResponse buildTeacherWorkload(Teacher teacher, Long termId) {
        List<com.example.sams.offering.domain.CourseOffering> offerings = courseOfferingRepository.findAllByTeacherId(teacher.getId()).stream()
                .filter(offering -> termId == null || offering.getTerm().getId().equals(termId))
                .toList();

        long openOfferings = offerings.stream()
                .filter(offering -> offering.getStatus() == com.example.sams.offering.domain.CourseOfferingStatus.OPEN)
                .count();
        long closedOfferings = offerings.stream()
                .filter(offering -> offering.getStatus() == com.example.sams.offering.domain.CourseOfferingStatus.CLOSED)
                .count();
        long archivedOfferings = offerings.stream()
                .filter(offering -> offering.getStatus() == com.example.sams.offering.domain.CourseOfferingStatus.ARCHIVED)
                .count();
        long totalAssignedStudents = offerings.stream()
                .mapToLong(offering -> enrollmentRepository.findAllByCourseOfferingIdAndStatus(offering.getId(), EnrollmentStatus.ENROLLED).size())
                .sum();
        long publishedExamCount = offerings.stream()
                .mapToLong(offering -> examRepository.findAllByCourseOfferingIdAndPublishedTrue(offering.getId()).size())
                .sum();
        int totalCapacity = offerings.stream()
                .map(com.example.sams.offering.domain.CourseOffering::getCapacity)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        BigDecimal averageCapacityUtilization = totalCapacity == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalAssignedStudents)
                        .multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(totalCapacity), 2, RoundingMode.HALF_UP);

        return new TeacherWorkloadReportResponse(
                teacher.getId(),
                teacher.getEmployeeCode(),
                teacher.getUser().getUsername(),
                teacher.getUser().getEmail(),
                teacher.getDepartment().getName(),
                teacher.getDesignation(),
                offerings.size(),
                openOfferings,
                closedOfferings,
                archivedOfferings,
                totalAssignedStudents,
                publishedExamCount,
                averageCapacityUtilization
        );
    }

    private Comparator<StudentDistributionReportResponse> studentDistributionComparator(Pageable pageable) {
        String sortBy = pageable.getSort().stream().findFirst().map(org.springframework.data.domain.Sort.Order::getProperty).orElse("studentCount");
        boolean descending = pageable.getSort().stream().findFirst().map(org.springframework.data.domain.Sort.Order::isDescending).orElse(true);

        Comparator<StudentDistributionReportResponse> comparator = switch (sortBy) {
            case "departmentName" -> Comparator.comparing(StudentDistributionReportResponse::departmentName, String.CASE_INSENSITIVE_ORDER);
            case "programName" -> Comparator.comparing(StudentDistributionReportResponse::programName, String.CASE_INSENSITIVE_ORDER);
            case "termName" -> Comparator.comparing(StudentDistributionReportResponse::termName, String.CASE_INSENSITIVE_ORDER);
            case "sectionName" -> Comparator.comparing(StudentDistributionReportResponse::sectionName, String.CASE_INSENSITIVE_ORDER);
            case "academicStatus" -> Comparator.comparing(StudentDistributionReportResponse::academicStatus, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(StudentDistributionReportResponse::studentCount);
        };

        if (descending) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(StudentDistributionReportResponse::programName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StudentDistributionReportResponse::sectionName, String.CASE_INSENSITIVE_ORDER);
    }

    private Page<StudentDistributionReportResponse> page(List<StudentDistributionReportResponse> content, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), content.size());
        List<StudentDistributionReportResponse> pageContent = start >= content.size() ? List.of() : content.subList(start, end);
        return new PageImpl<>(pageContent, pageable, content.size());
    }

    private List<AdminStudentAnalyticsSummaryResponse.CountBreakdown> buildBreakdown(
            List<Student> students,
            java.util.function.Function<Student, Long> idExtractor,
            java.util.function.Function<Student, String> nameExtractor
    ) {
        return students.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        student -> Map.entry(idExtractor.apply(student), nameExtractor.apply(student)),
                        java.util.stream.Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new AdminStudentAnalyticsSummaryResponse.CountBreakdown(
                        entry.getKey().getKey(),
                        entry.getKey().getValue(),
                        entry.getValue()
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

    private record StudentDistributionKey(
            Long departmentId,
            String departmentName,
            Long programId,
            String programName,
            Long termId,
            String termName,
            String academicYear,
            Long sectionId,
            String sectionName,
            String academicStatus
    ) {
    }
}
