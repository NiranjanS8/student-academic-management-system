package com.example.sams.offering.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.Subject;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.academic.repository.SubjectRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
import com.example.sams.offering.dto.CourseOfferingRequest;
import com.example.sams.offering.dto.CourseOfferingResponse;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.repository.TeacherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseOfferingAdministrationService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SectionRepository sectionRepository;
    private final TeacherRepository teacherRepository;
    private final CourseOfferingResponseMapper courseOfferingResponseMapper;

    public CourseOfferingAdministrationService(
            CourseOfferingRepository courseOfferingRepository,
            SubjectRepository subjectRepository,
            AcademicTermRepository academicTermRepository,
            SectionRepository sectionRepository,
            TeacherRepository teacherRepository,
            CourseOfferingResponseMapper courseOfferingResponseMapper
    ) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.subjectRepository = subjectRepository;
        this.academicTermRepository = academicTermRepository;
        this.sectionRepository = sectionRepository;
        this.teacherRepository = teacherRepository;
        this.courseOfferingResponseMapper = courseOfferingResponseMapper;
    }

    @Transactional
    public CourseOfferingResponse createCourseOffering(CourseOfferingRequest request) {
        Subject subject = getSubject(request.subjectId());
        AcademicTerm term = getTerm(request.termId());
        Section section = getSection(request.sectionId());
        Teacher teacher = getTeacher(request.teacherId());
        CourseOfferingStatus status = parseStatus(request.status());

        validateOfferingIntegrity(subject, term, section, teacher, null);
        validateOfferingWindow(request.enrollmentOpenAt(), request.enrollmentCloseAt());
        validateSchedule(request.scheduleStartTime(), request.scheduleEndTime());

        CourseOffering offering = new CourseOffering();
        offering.setSubject(subject);
        offering.setTerm(term);
        offering.setSection(section);
        offering.setTeacher(teacher);
        offering.setCapacity(request.capacity());
        offering.setRoomCode(normalize(request.roomCode()));
        offering.setScheduleDays(normalize(request.scheduleDays()));
        offering.setScheduleStartTime(request.scheduleStartTime());
        offering.setScheduleEndTime(request.scheduleEndTime());
        offering.setEnrollmentOpenAt(request.enrollmentOpenAt());
        offering.setEnrollmentCloseAt(request.enrollmentCloseAt());
        offering.setStatus(status);
        courseOfferingRepository.save(offering);

        return toResponse(offering);
    }

    @Transactional
    public CourseOfferingResponse updateCourseOffering(Long offeringId, CourseOfferingRequest request) {
        CourseOffering offering = getOffering(offeringId);
        Subject subject = getSubject(request.subjectId());
        AcademicTerm term = getTerm(request.termId());
        Section section = getSection(request.sectionId());
        Teacher teacher = getTeacher(request.teacherId());
        CourseOfferingStatus status = parseStatus(request.status());

        validateOfferingIntegrity(subject, term, section, teacher, offeringId);
        validateOfferingWindow(request.enrollmentOpenAt(), request.enrollmentCloseAt());
        validateSchedule(request.scheduleStartTime(), request.scheduleEndTime());

        offering.setSubject(subject);
        offering.setTerm(term);
        offering.setSection(section);
        offering.setTeacher(teacher);
        offering.setCapacity(request.capacity());
        offering.setRoomCode(normalize(request.roomCode()));
        offering.setScheduleDays(normalize(request.scheduleDays()));
        offering.setScheduleStartTime(request.scheduleStartTime());
        offering.setScheduleEndTime(request.scheduleEndTime());
        offering.setEnrollmentOpenAt(request.enrollmentOpenAt());
        offering.setEnrollmentCloseAt(request.enrollmentCloseAt());
        offering.setStatus(status);

        return toResponse(offering);
    }

    @Transactional(readOnly = true)
    public CourseOfferingResponse getCourseOfferingById(Long offeringId) {
        return toResponse(getOffering(offeringId));
    }

    @Transactional(readOnly = true)
    public Page<CourseOfferingResponse> listCourseOfferings(
            Long termId,
            Long sectionId,
            Long teacherId,
            Long subjectId,
            String status,
            Pageable pageable
    ) {
        CourseOfferingStatus parsedStatus = (status == null || status.isBlank()) ? null : parseStatus(status);
        return courseOfferingRepository.search(termId, sectionId, teacherId, subjectId, parsedStatus, pageable)
                .map(this::toResponse);
    }

    private CourseOffering getOffering(Long offeringId) {
        return courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));
    }

    private Subject getSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }

    private AcademicTerm getTerm(Long termId) {
        return academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    private Section getSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private Teacher getTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private void validateOfferingWindow(java.time.Instant openAt, java.time.Instant closeAt) {
        if (openAt != null && closeAt != null && closeAt.isBefore(openAt)) {
            throw new ConflictException("enrollmentCloseAt cannot be before enrollmentOpenAt");
        }
    }

    private void validateOfferingIntegrity(
            Subject subject,
            AcademicTerm term,
            Section section,
            Teacher teacher,
            Long existingOfferingId
    ) {
        boolean duplicateExists = existingOfferingId == null
                ? courseOfferingRepository.existsBySubjectIdAndTermIdAndSectionId(subject.getId(), term.getId(), section.getId())
                : courseOfferingRepository.existsBySubjectIdAndTermIdAndSectionIdAndIdNot(
                        subject.getId(),
                        term.getId(),
                        section.getId(),
                        existingOfferingId
                );
        if (duplicateExists) {
            throw new ConflictException("An offering already exists for this subject, term, and section");
        }

        if (!teacher.getDepartment().getId().equals(subject.getDepartment().getId())) {
            throw new ConflictException("Teacher department must match subject department");
        }

        if (section.getCurrentTerm() != null && !section.getCurrentTerm().getId().equals(term.getId())) {
            throw new ConflictException("Section current term must match the offering term");
        }
    }

    private void validateSchedule(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if ((startTime == null) != (endTime == null)) {
            throw new ConflictException("scheduleStartTime and scheduleEndTime must be provided together");
        }
        if (startTime != null && !endTime.isAfter(startTime)) {
            throw new ConflictException("scheduleEndTime must be after scheduleStartTime");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CourseOfferingStatus parseStatus(String rawStatus) {
        try {
            return CourseOfferingStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid status. Allowed values: DRAFT, OPEN, CLOSED, ARCHIVED");
        }
    }

    private CourseOfferingResponse toResponse(CourseOffering offering) {
        return courseOfferingResponseMapper.toResponse(offering);
    }
}
