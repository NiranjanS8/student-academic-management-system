package com.example.sams.exam.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.Exam;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.dto.ExamRequest;
import com.example.sams.exam.dto.ExamResponse;
import com.example.sams.exam.dto.MarkEntryRequest;
import com.example.sams.exam.dto.MarkEntryResponse;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.notification.service.NotificationService;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.Student;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.service.AppUserDetails;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherExamService {

    private final ExamRepository examRepository;
    private final MarkEntryRepository markEntryRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamResponseMapper examResponseMapper;
    private final MarkEntryResponseMapper markEntryResponseMapper;
    private final GradeCalculationService gradeCalculationService;
    private final NotificationService notificationService;

    public TeacherExamService(
            ExamRepository examRepository,
            MarkEntryRepository markEntryRepository,
            CourseOfferingRepository courseOfferingRepository,
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            ExamResponseMapper examResponseMapper,
            MarkEntryResponseMapper markEntryResponseMapper,
            GradeCalculationService gradeCalculationService,
            NotificationService notificationService
    ) {
        this.examRepository = examRepository;
        this.markEntryRepository = markEntryRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examResponseMapper = examResponseMapper;
        this.markEntryResponseMapper = markEntryResponseMapper;
        this.gradeCalculationService = gradeCalculationService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        CourseOffering offering = courseOfferingRepository.findByIdAndTeacherUserId(request.courseOfferingId(), getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));

        validateExamRequest(request);

        Exam exam = new Exam();
        exam.setCourseOffering(offering);
        exam.setTitle(request.title().trim());
        exam.setExamType(request.examType().trim().toUpperCase());
        exam.setMaxMarks(request.maxMarks());
        exam.setWeightage(request.weightage());
        exam.setScheduledAt(request.scheduledAt());
        exam.setPublished(false);
        examRepository.save(exam);

        return examResponseMapper.toResponse(exam);
    }

    @Transactional(readOnly = true)
    public Page<ExamResponse> listAssignedExams(Long offeringId, Pageable pageable) {
        Page<Exam> exams = offeringId == null
                ? examRepository.findAllByCourseOfferingTeacherUserId(getAuthenticatedUserId(), pageable)
                : examRepository.findAllByCourseOfferingIdAndCourseOfferingTeacherUserId(offeringId, getAuthenticatedUserId(), pageable);
        return exams.map(examResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ExamResponse getAssignedExam(Long examId) {
        return examResponseMapper.toResponse(getAssignedExamEntity(examId));
    }

    @Transactional
    public MarkEntryResponse createMarkEntry(Long examId, MarkEntryRequest request) {
        Exam exam = getAssignedExamEntity(examId);
        Student student = getEligibleStudent(exam.getCourseOffering().getId(), request.studentId());
        validateMarks(request.marksObtained(), exam.getMaxMarks());

        if (markEntryRepository.findByExamIdAndStudentId(examId, student.getId()).isPresent()) {
            throw new ConflictException("Mark entry already exists for this student in the exam");
        }

        MarkEntry markEntry = new MarkEntry();
        markEntry.setExam(exam);
        markEntry.setStudent(student);
        markEntry.setMarksObtained(request.marksObtained());
        applyCalculatedGradeFields(markEntry, exam);
        markEntry.setRemarks(normalize(request.remarks()));
        markEntryRepository.save(markEntry);

        return markEntryResponseMapper.toResponse(markEntry);
    }

    @Transactional
    public MarkEntryResponse updateMarkEntry(Long examId, Long markEntryId, MarkEntryRequest request) {
        Exam exam = getAssignedExamEntity(examId);
        MarkEntry markEntry = markEntryRepository.findByIdAndExamCourseOfferingTeacherUserId(markEntryId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Mark entry not found"));
        if (!markEntry.getExam().getId().equals(exam.getId())) {
            throw new ResourceNotFoundException("Mark entry not found");
        }

        Student student = getEligibleStudent(exam.getCourseOffering().getId(), request.studentId());
        validateMarks(request.marksObtained(), exam.getMaxMarks());

        if (!markEntry.getStudent().getId().equals(student.getId())) {
            markEntryRepository.findByExamIdAndStudentId(examId, student.getId()).ifPresent(existing -> {
                if (!existing.getId().equals(markEntryId)) {
                    throw new ConflictException("Mark entry already exists for this student in the exam");
                }
            });
            markEntry.setStudent(student);
        }

        markEntry.setMarksObtained(request.marksObtained());
        applyCalculatedGradeFields(markEntry, exam);
        markEntry.setRemarks(normalize(request.remarks()));
        return markEntryResponseMapper.toResponse(markEntry);
    }

    @Transactional(readOnly = true)
    public Page<MarkEntryResponse> listMarkEntries(Long examId, Pageable pageable) {
        getAssignedExamEntity(examId);
        return markEntryRepository.findAllByExamIdAndExamCourseOfferingTeacherUserId(examId, getAuthenticatedUserId(), pageable)
                .map(markEntryResponseMapper::toResponse);
    }

    @Transactional
    public ExamResponse publishExam(Long examId) {
        Exam exam = getAssignedExamEntity(examId);
        if (exam.isPublished()) {
            throw new ConflictException("Exam results are already published");
        }

        long activeEnrollmentCount = enrollmentRepository.countByCourseOfferingIdAndStatus(
                exam.getCourseOffering().getId(),
                EnrollmentStatus.ENROLLED
        );
        long enteredMarkCount = markEntryRepository.countByExamId(examId);

        if (activeEnrollmentCount == 0) {
            throw new ConflictException("Cannot publish results without active enrollments");
        }
        if (enteredMarkCount < activeEnrollmentCount) {
            throw new ConflictException("Cannot publish results until marks are entered for all actively enrolled students");
        }

        exam.setPublished(true);
        exam.setPublishedAt(Instant.now());
        notificationService.notifyExamPublished(exam);
        return examResponseMapper.toResponse(exam);
    }

    private Exam getAssignedExamEntity(Long examId) {
        return examRepository.findByIdAndCourseOfferingTeacherUserId(examId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    private Student getEligibleStudent(Long offeringId, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        boolean enrolled = enrollmentRepository.findByStudentIdAndCourseOfferingId(studentId, offeringId)
                .map(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .orElse(false);
        if (!enrolled) {
            throw new ConflictException("Marks can only be entered for actively enrolled students");
        }
        return student;
    }

    private void validateExamRequest(ExamRequest request) {
        if (request.weightage().compareTo(BigDecimal.ZERO) <= 0 || request.weightage().compareTo(new BigDecimal("100.00")) > 0) {
            throw new ConflictException("weightage must be between 0.01 and 100.00");
        }
    }

    private void validateMarks(BigDecimal marksObtained, BigDecimal maxMarks) {
        if (marksObtained.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("marksObtained cannot be negative");
        }
        if (marksObtained.compareTo(maxMarks) > 0) {
            throw new ConflictException("marksObtained cannot exceed maxMarks");
        }
    }

    private void applyCalculatedGradeFields(MarkEntry markEntry, Exam exam) {
        GradeCalculationService.GradeSnapshot gradeSnapshot = gradeCalculationService.calculate(
                markEntry.getMarksObtained(),
                exam.getMaxMarks(),
                exam.getWeightage()
        );
        markEntry.setPercentageScore(gradeSnapshot.percentageScore());
        markEntry.setWeightedScore(gradeSnapshot.weightedScore());
        markEntry.setLetterGrade(gradeSnapshot.letterGrade());
        markEntry.setGradePoints(gradeSnapshot.gradePoints());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }
}
