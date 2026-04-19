package com.example.sams.attendance.service;

import com.example.sams.attendance.domain.AttendanceRecord;
import com.example.sams.attendance.domain.AttendanceSession;
import com.example.sams.attendance.domain.AttendanceStatus;
import com.example.sams.attendance.dto.AttendanceEligibleStudentResponse;
import com.example.sams.attendance.dto.AttendanceRecordRequest;
import com.example.sams.attendance.dto.AttendanceSessionRequest;
import com.example.sams.attendance.dto.AttendanceSessionResponse;
import com.example.sams.attendance.dto.AttendanceSessionUpdateRequest;
import com.example.sams.attendance.repository.AttendanceRecordRepository;
import com.example.sams.attendance.repository.AttendanceSessionRepository;
import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.audit.domain.AuditActionType;
import com.example.sams.audit.service.AuditLogService;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.Student;
import com.example.sams.user.service.AppUserDetails;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherAttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceResponseMapper attendanceResponseMapper;
    private final AuditLogService auditLogService;

    public TeacherAttendanceService(
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            AttendanceResponseMapper attendanceResponseMapper,
            AuditLogService auditLogService
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceResponseMapper = attendanceResponseMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AttendanceSessionResponse createAttendanceSession(AttendanceSessionRequest request) {
        CourseOffering offering = getAssignedOffering(request.courseOfferingId());
        attendanceSessionRepository.findByCourseOfferingIdAndSessionDate(offering.getId(), request.sessionDate()).ifPresent(existing -> {
            throw new ConflictException("Attendance session already exists for this offering and date");
        });

        AttendanceSession session = new AttendanceSession();
        session.setCourseOffering(offering);
        session.setSessionDate(request.sessionDate());
        attendanceSessionRepository.save(session);

        upsertAttendanceRecords(session, request.records(), false);
        auditLogService.log(
                AuditActionType.ATTENDANCE_SESSION_CREATED,
                "ATTENDANCE_SESSION",
                session.getId(),
                "Created attendance session for %s on %s with %s records".formatted(
                        session.getCourseOffering().getSubject().getCode(),
                        session.getSessionDate(),
                        request.records().size()
                )
        );
        return buildSessionResponse(session);
    }

    @Transactional
    public AttendanceSessionResponse updateAttendanceSession(Long sessionId, AttendanceSessionUpdateRequest request) {
        AttendanceSession session = getAssignedSession(sessionId);
        upsertAttendanceRecords(session, request.records(), true);
        auditLogService.log(
                AuditActionType.ATTENDANCE_SESSION_UPDATED,
                "ATTENDANCE_SESSION",
                session.getId(),
                "Updated attendance session for %s on %s with %s records".formatted(
                        session.getCourseOffering().getSubject().getCode(),
                        session.getSessionDate(),
                        request.records().size()
                )
        );
        return buildSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceSessionResponse> listAssignedSessions(Long offeringId, Pageable pageable) {
        Page<AttendanceSession> sessions = offeringId == null
                ? attendanceSessionRepository.findAllByCourseOfferingTeacherUserId(getAuthenticatedUserId(), pageable)
                : attendanceSessionRepository.findAllByCourseOfferingIdAndCourseOfferingTeacherUserId(offeringId, getAuthenticatedUserId(), pageable);
        return sessions.map(this::buildSessionResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceSessionResponse getAssignedSessionById(Long sessionId) {
        return buildSessionResponse(getAssignedSession(sessionId));
    }

    @Transactional(readOnly = true)
    public List<AttendanceEligibleStudentResponse> listEligibleStudents(Long offeringId) {
        getAssignedOffering(offeringId);
        return enrollmentRepository.findAllByCourseOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED).stream()
                .map(Enrollment::getStudent)
                .map(attendanceResponseMapper::toEligibleStudentResponse)
                .toList();
    }

    private void upsertAttendanceRecords(AttendanceSession session, List<AttendanceRecordRequest> requests, boolean allowUpdate) {
        validateUniqueStudents(requests);

        Map<Long, Student> eligibleStudents = enrollmentRepository.findAllByCourseOfferingIdAndStatus(
                        session.getCourseOffering().getId(),
                        EnrollmentStatus.ENROLLED
                ).stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        if (eligibleStudents.isEmpty()) {
            throw new ConflictException("Attendance can only be marked when active enrollments exist");
        }

        for (AttendanceRecordRequest request : requests) {
            Student student = eligibleStudents.get(request.studentId());
            if (student == null) {
                throw new ConflictException("Attendance can only be marked for actively enrolled students");
            }

            AttendanceRecord record = attendanceRecordRepository.findBySessionIdAndStudentId(session.getId(), student.getId())
                    .orElseGet(() -> {
                        if (allowUpdate) {
                            AttendanceRecord newRecord = new AttendanceRecord();
                            newRecord.setSession(session);
                            newRecord.setStudent(student);
                            return newRecord;
                        }
                        AttendanceRecord newRecord = new AttendanceRecord();
                        newRecord.setSession(session);
                        newRecord.setStudent(student);
                        return newRecord;
                    });

            if (!allowUpdate && record.getId() != null) {
                throw new ConflictException("Attendance already exists for one or more students in this session");
            }

            record.setStatus(parseStatus(request.status()));
            record.setMarkedAt(Instant.now());
            attendanceRecordRepository.save(record);
        }
    }

    private AttendanceSessionResponse buildSessionResponse(AttendanceSession session) {
        return attendanceResponseMapper.toSessionResponse(
                session,
                attendanceRecordRepository.findAllBySessionIdOrderByStudentStudentCodeAsc(session.getId())
        );
    }

    private CourseOffering getAssignedOffering(Long offeringId) {
        return courseOfferingRepository.findByIdAndTeacherUserId(offeringId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));
    }

    private AttendanceSession getAssignedSession(Long sessionId) {
        return attendanceSessionRepository.findByIdAndCourseOfferingTeacherUserId(sessionId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found"));
    }

    private void validateUniqueStudents(List<AttendanceRecordRequest> requests) {
        long distinctCount = requests.stream().map(AttendanceRecordRequest::studentId).distinct().count();
        if (distinctCount != requests.size()) {
            throw new ConflictException("Duplicate student entries are not allowed in attendance records");
        }
    }

    private AttendanceStatus parseStatus(String rawStatus) {
        try {
            return AttendanceStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: PRESENT, ABSENT");
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }
}
