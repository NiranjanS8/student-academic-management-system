package com.example.sams.user.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.User;
import com.example.sams.user.dto.StudentProfileResponse;
import com.example.sams.user.dto.UpdateStudentRequest;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAdministrationService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SectionRepository sectionRepository;

    public StudentAdministrationService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            AcademicTermRepository academicTermRepository,
            SectionRepository sectionRepository
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.academicTermRepository = academicTermRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentById(Long studentId) {
        return toStudentProfileResponse(getStudent(studentId));
    }

    @Transactional(readOnly = true)
    public Page<StudentProfileResponse> listStudents(
            Long departmentId,
            Long programId,
            Long sectionId,
            String query,
            Pageable pageable
    ) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return studentRepository.search(departmentId, programId, sectionId, normalizedQuery, pageable)
                .map(this::toStudentProfileResponse);
    }

    @Transactional
    public StudentProfileResponse updateStudent(Long studentId, UpdateStudentRequest request) {
        Student student = getStudent(studentId);
        User user = student.getUser();
        Department department = getDepartment(request.departmentId());
        Program program = getProgram(request.programId());
        AcademicTerm currentTerm = resolveAcademicTerm(request.currentTermId());
        Section section = resolveSection(request.sectionId());

        ensureStudentUniqueness(student, request);

        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setAccountStatus(parseAccountStatus(request.accountStatus()));

        student.setStudentCode(request.studentCode().trim());
        student.setDepartment(department);
        student.setProgram(program);
        student.setCurrentTerm(currentTerm);
        student.setSection(section);
        student.setAcademicStatus(parseAcademicStatus(request.academicStatus()));
        student.setAdmissionDate(request.admissionDate());

        return toStudentProfileResponse(student);
    }

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private Program getProgram(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private AcademicTerm resolveAcademicTerm(Long termId) {
        return termId == null ? null : academicTermRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
    }

    private Section resolveSection(Long sectionId) {
        return sectionId == null ? null : sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private void ensureStudentUniqueness(Student student, UpdateStudentRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        String studentCode = request.studentCode().trim();

        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(student.getUser().getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("User with username already exists");
                });

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(student.getUser().getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("User with email already exists");
                });

        studentRepository.findAll().stream()
                .filter(existing -> existing.getStudentCode().equalsIgnoreCase(studentCode))
                .filter(existing -> !existing.getId().equals(student.getId()))
                .findAny()
                .ifPresent(existing -> {
                    throw new ConflictException("Student with studentCode already exists");
                });
    }

    private AccountStatus parseAccountStatus(String rawStatus) {
        try {
            return AccountStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid accountStatus. Allowed values: ACTIVE, SUSPENDED, LOCKED, DISABLED");
        }
    }

    private AcademicStatus parseAcademicStatus(String rawStatus) {
        try {
            return AcademicStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid academicStatus. Allowed values: ACTIVE, ON_HOLD, GRADUATED, DROPPED");
        }
    }

    private StudentProfileResponse toStudentProfileResponse(Student student) {
        User user = student.getUser();
        Department department = student.getDepartment();
        Program program = student.getProgram();
        AcademicTerm currentTerm = student.getCurrentTerm();
        Section section = student.getSection();

        return new StudentProfileResponse(
                student.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAccountStatus(),
                student.getStudentCode(),
                new StudentProfileResponse.DepartmentSummary(
                        department.getId(),
                        department.getCode(),
                        department.getName()
                ),
                new StudentProfileResponse.ProgramSummary(
                        program.getId(),
                        program.getCode(),
                        program.getName()
                ),
                currentTerm == null ? null : new StudentProfileResponse.AcademicTermSummary(
                        currentTerm.getId(),
                        currentTerm.getName(),
                        currentTerm.getAcademicYear(),
                        currentTerm.getStatus()
                ),
                section == null ? null : new StudentProfileResponse.SectionSummary(
                        section.getId(),
                        section.getName()
                ),
                student.getAcademicStatus().name(),
                student.getAdmissionDate(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
