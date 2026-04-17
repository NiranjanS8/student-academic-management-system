package com.example.sams.user.service;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.auth.service.AuthService;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.domain.User;
import com.example.sams.user.dto.CreateStudentRequest;
import com.example.sams.user.dto.CreateTeacherRequest;
import com.example.sams.user.dto.UserProvisionResponse;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicTermRepository academicTermRepository;
    private final SectionRepository sectionRepository;
    private final AuthService authService;

    public UserProvisioningService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            AcademicTermRepository academicTermRepository,
            SectionRepository sectionRepository,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.academicTermRepository = academicTermRepository;
        this.sectionRepository = sectionRepository;
        this.authService = authService;
    }

    @Transactional
    public UserProvisionResponse createTeacher(CreateTeacherRequest request) {
        ensureUserUniqueness(request.username(), request.email());
        if (teacherRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new ConflictException("Teacher with employeeCode already exists");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        User user = buildBaseUser(request.username(), request.email(), request.password(), Role.TEACHER);
        userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(request.employeeCode());
        teacher.setDepartment(department);
        teacher.setDesignation(request.designation());
        teacherRepository.save(teacher);

        return new UserProvisionResponse(
                user.getId(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                teacher.getId(),
                teacher.getEmployeeCode(),
                "Teacher account created successfully"
        );
    }

    @Transactional
    public UserProvisionResponse createStudent(CreateStudentRequest request) {
        ensureUserUniqueness(request.username(), request.email());
        if (studentRepository.existsByStudentCode(request.studentCode())) {
            throw new ConflictException("Student with studentCode already exists");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        AcademicTerm currentTerm = request.currentTermId() == null ? null : academicTermRepository.findById(request.currentTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic term not found"));
        Section section = request.sectionId() == null ? null : sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        User user = buildBaseUser(request.username(), request.email(), request.password(), Role.STUDENT);
        userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(request.studentCode());
        student.setDepartment(department);
        student.setProgram(program);
        student.setCurrentTerm(currentTerm);
        student.setSection(section);
        student.setAcademicStatus(parseAcademicStatus(request.academicStatus()));
        student.setAdmissionDate(request.admissionDate());
        studentRepository.save(student);

        return new UserProvisionResponse(
                user.getId(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                student.getId(),
                student.getStudentCode(),
                "Student account created successfully"
        );
    }

    private User buildBaseUser(String username, String email, String password, Role role) {
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(authService.encodePassword(password));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return user;
    }

    private void ensureUserUniqueness(String username, String email) {
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new ConflictException("User with username already exists");
        }
        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new ConflictException("User with email already exists");
        }
    }

    private AcademicStatus parseAcademicStatus(String rawStatus) {
        try {
            return AcademicStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid academicStatus. Allowed values: ACTIVE, ON_HOLD, GRADUATED, DROPPED");
        }
    }
}
