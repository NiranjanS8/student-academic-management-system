package com.example.sams.auth;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.SubjectPrerequisite;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.academic.repository.SubjectPrerequisiteRepository;
import com.example.sams.academic.repository.SubjectRepository;
import com.example.sams.auth.domain.RefreshToken;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private AcademicTermRepository academicTermRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectPrerequisiteRepository subjectPrerequisiteRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private MarkEntryRepository markEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        markEntryRepository.deleteAll();
        examRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        courseOfferingRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
        subjectPrerequisiteRepository.deleteAll();
        subjectRepository.deleteAll();
        sectionRepository.deleteAll();
        programRepository.deleteAll();
        departmentRepository.deleteAll();
        academicTermRepository.deleteAll();

        department = new Department();
        department.setCode("CSE");
        department.setName("Computer Science");
        departmentRepository.save(department);

        program = new Program();
        program.setCode("BTECH-CSE");
        program.setName("BTech CSE");
        program.setDepartment(department);
        programRepository.save(program);

        term = new AcademicTerm();
        term.setName("Semester 1");
        term.setAcademicYear("2026-2027");
        term.setStartDate(LocalDate.of(2026, 6, 1));
        term.setEndDate(LocalDate.of(2026, 11, 30));
        term.setStatus("ACTIVE");
        academicTermRepository.save(term);

        section = new Section();
        section.setProgram(program);
        section.setName("A");
        section.setCurrentTerm(term);
        sectionRepository.save(section);

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
    }

    @Test
    void loginReturnsTokensAndPersistsRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void adminCanCreateTeacherAndStudentAccounts() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher1",
                                  "email": "teacher1@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-001",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("TEACHER"))
                .andExpect(jsonPath("$.data.profileCode").value("EMP-001"));

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student1",
                                  "email": "student1@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-001",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.profileCode").value("STU-001"));

        assertThat(teacherRepository.existsByEmployeeCode("EMP-001")).isTrue();
        assertThat(studentRepository.existsByStudentCode("STU-001")).isTrue();
    }

    @Test
    void currentUserReturnsAuthenticatedAdminProfile() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    void adminCanViewListAndUpdateTeacherProfiles() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher2",
                                  "email": "teacher2@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-002",
                                  "departmentId": %d,
                                  "designation": "Lecturer"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-002".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/admin/users/teachers/{teacherId}", teacherId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-002"))
                .andExpect(jsonPath("$.data.designation").value("Lecturer"));

        mockMvc.perform(get("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("departmentId", String.valueOf(department.getId()))
                        .param("query", "teacher2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP-002"));

        mockMvc.perform(put("/api/v1/admin/users/teachers/{teacherId}", teacherId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher2-updated",
                                  "email": "teacher2-updated@sams.local",
                                  "employeeCode": "EMP-002A",
                                  "departmentId": %d,
                                  "designation": "Senior Lecturer",
                                  "accountStatus": "SUSPENDED"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("teacher2-updated"))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-002A"))
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.designation").value("Senior Lecturer"));
    }

    @Test
    void adminCanViewListAndUpdateStudentProfiles() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student2",
                                  "email": "student2@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-002",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long studentId = studentRepository.findAll().stream()
                .filter(student -> "STU-002".equals(student.getStudentCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/admin/users/students/{studentId}", studentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCode").value("STU-002"))
                .andExpect(jsonPath("$.data.academicStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("departmentId", String.valueOf(department.getId()))
                        .param("programId", String.valueOf(program.getId()))
                        .param("sectionId", String.valueOf(section.getId()))
                        .param("query", "student2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].studentCode").value("STU-002"));

        mockMvc.perform(put("/api/v1/admin/users/students/{studentId}", studentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student2-updated",
                                  "email": "student2-updated@sams.local",
                                  "studentCode": "STU-002A",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ON_HOLD",
                                  "accountStatus": "SUSPENDED",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("student2-updated"))
                .andExpect(jsonPath("$.data.studentCode").value("STU-002A"))
                .andExpect(jsonPath("$.data.academicStatus").value("ON_HOLD"))
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));
    }

    @Test
    void studentCanViewAvailableOfferingsForOwnSectionAndTerm() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CN501",
                                  "name": "Cloud Networks",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DS502",
                                  "name": "Distributed Systems",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-student-view",
                                  "email": "teacher-student-view@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-STU-VIEW-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-offerings",
                                  "email": "student-offerings@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-OFFER-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectOneId = subjectRepository.findByCodeIgnoreCase("CN501").orElseThrow().getId();
        Long subjectTwoId = subjectRepository.findByCodeIgnoreCase("DS502").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-STU-VIEW-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 50,
                                  "roomCode": "C-101",
                                  "scheduleDays": "MON,FRI",
                                  "scheduleStartTime": "08:30:00",
                                  "scheduleEndTime": "10:00:00",
                                  "enrollmentOpenAt": "2026-01-01T00:00:00Z",
                                  "enrollmentCloseAt": "2027-01-01T00:00:00Z",
                                  "status": "OPEN"
                                }
                                """.formatted(subjectOneId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 45,
                                  "status": "CLOSED"
                                }
                                """.formatted(subjectTwoId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        String studentAccessToken = extractAccessToken("student-offerings", "Student@123");

        mockMvc.perform(get("/api/v1/student/offerings")
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].subject.code").value("CN501"))
                .andExpect(jsonPath("$.data.content[0].roomCode").value("C-101"))
                .andExpect(jsonPath("$.data.content[0].scheduleDays").value("MON,FRI"))
                .andExpect(jsonPath("$.data.content[0].enrollmentCurrentlyOpen").value(true));

        Long visibleOfferingId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .filter(offering -> offering.getStatus().name().equals("OPEN"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/student/offerings/{offeringId}", visibleOfferingId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(visibleOfferingId))
                .andExpect(jsonPath("$.data.subject.code").value("CN501"));
    }

    @Test
    void studentCannotViewOfferingOutsideOwnSectionOrTerm() throws Exception {
        String adminAccessToken = extractAccessToken();

        Section otherSection = new Section();
        otherSection.setProgram(program);
        otherSection.setName("B");
        otherSection.setCurrentTerm(term);
        sectionRepository.save(otherSection);

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OS503",
                                  "name": "Advanced Operating Systems",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-hidden-offering",
                                  "email": "teacher-hidden-offering@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-HIDE-01",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-hidden-check",
                                  "email": "student-hidden-check@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-HIDE-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("OS503").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-HIDE-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), otherSection.getId(), teacherId)))
                .andExpect(status().isOk());

        Long hiddenOfferingId = courseOfferingRepository.findAllBySectionId(otherSection.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentAccessToken = extractAccessToken("student-hidden-check", "Student@123");

        mockMvc.perform(get("/api/v1/student/offerings/{offeringId}", hiddenOfferingId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course offering not found"));
    }

    @Test
    void studentCanEnrollDropAndViewEnrollmentHistory() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE601",
                                  "name": "Secure Engineering",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-enrollment",
                                  "email": "teacher-enrollment@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-ENROLL-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-enrollment",
                                  "email": "student-enrollment@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-ENROLL-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("SE601").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-ENROLL-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 50,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentAccessToken = extractAccessToken("student-enrollment", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.courseOffering.id").value(offeringId))
                .andExpect(jsonPath("$.data.courseOffering.subject.code").value("SE601"));

        Long enrollmentId = enrollmentRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(enrollmentId))
                .andExpect(jsonPath("$.data.content[0].status").value("ENROLLED"));

        mockMvc.perform(get("/api/v1/student/enrollments/history")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .param("termId", String.valueOf(term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ENROLLED"));

        mockMvc.perform(post("/api/v1/student/enrollments/{enrollmentId}/drop", enrollmentId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"))
                .andExpect(jsonPath("$.data.droppedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        mockMvc.perform(get("/api/v1/student/enrollments/history")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .param("status", "DROPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(enrollmentId))
                .andExpect(jsonPath("$.data.content[0].status").value("DROPPED"));
    }

    @Test
    void studentCannotEnrollInOfferingOutsideOwnSectionOrTerm() throws Exception {
        String adminAccessToken = extractAccessToken();

        Section otherSection = new Section();
        otherSection.setProgram(program);
        otherSection.setName("C");
        otherSection.setCurrentTerm(term);
        sectionRepository.save(otherSection);

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OS602",
                                  "name": "Operating Systems Lab",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-enrollment-hidden",
                                  "email": "teacher-enrollment-hidden@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-ENROLL-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-hidden-enrollment",
                                  "email": "student-hidden-enrollment@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-ENROLL-02",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("OS602").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-ENROLL-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 30,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), otherSection.getId(), teacherId)))
                .andExpect(status().isOk());

        Long hiddenOfferingId = courseOfferingRepository.findAllBySectionId(otherSection.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentAccessToken = extractAccessToken("student-hidden-enrollment", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(hiddenOfferingId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course offering not found"));
    }

    @Test
    void studentCannotCreateDuplicateActiveEnrollmentAndCanReEnrollAfterDrop() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CN603",
                                  "name": "Campus Networks",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-reenroll",
                                  "email": "teacher-reenroll@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-REENROLL-01",
                                  "departmentId": %d,
                                  "designation": "Lecturer"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-reenroll",
                                  "email": "student-reenroll@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-REENROLL-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("CN603").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-REENROLL-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 45,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentAccessToken = extractAccessToken("student-reenroll", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENROLLED"));

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student is already actively enrolled in this offering"));

        Long enrollmentId = enrollmentRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/student/enrollments/{enrollmentId}/drop", enrollmentId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"));

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(enrollmentId))
                .andExpect(jsonPath("$.data.status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.droppedAt").doesNotExist());
    }

    @Test
    void studentCannotDropAlreadyDroppedEnrollment() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DS604",
                                  "name": "Data Security",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-drop-check",
                                  "email": "teacher-drop-check@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-DROP-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-drop-check",
                                  "email": "student-drop-check@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-DROP-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId()
                        )))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("DS604").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-DROP-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentAccessToken = extractAccessToken("student-drop-check", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk());

        Long enrollmentId = enrollmentRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/student/enrollments/{enrollmentId}/drop", enrollmentId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DROPPED"));

        mockMvc.perform(post("/api/v1/student/enrollments/{enrollmentId}/drop", enrollmentId)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enrollment is already dropped"));
    }

    @Test
    void studentCannotEnrollWhenOfferingCapacityIsReached() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CAP605",
                                  "name": "Capacity Systems",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-capacity",
                                  "email": "teacher-capacity@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-CAP-01",
                                  "departmentId": %d,
                                  "designation": "Lecturer"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-capacity-1",
                                  "email": "student-capacity-1@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-CAP-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-capacity-2",
                                  "email": "student-capacity-2@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-CAP-02",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("CAP605").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-CAP-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 1,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentOneToken = extractAccessToken("student-capacity-1", "Student@123");
        String studentTwoToken = extractAccessToken("student-capacity-2", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentTwoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Offering capacity has been reached"));
    }

    @Test
    void studentCannotEnrollBeforeEnrollmentWindowOpens() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "WIN606",
                                  "name": "Window Systems",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-window",
                                  "email": "teacher-window@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-WIN-01",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-window",
                                  "email": "student-window@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-WIN-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("WIN606").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-WIN-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "enrollmentOpenAt": "2099-01-01T00:00:00Z",
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentToken = extractAccessToken("student-window", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enrollment has not opened for this offering yet"));
    }

    @Test
    void studentCannotEnrollAfterEnrollmentWindowClosesOrWhenOfferingIsNotOpen() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CLS607",
                                  "name": "Closed Systems",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DRF608",
                                  "name": "Draft Systems",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-window-closed",
                                  "email": "teacher-window-closed@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-WIN-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-window-closed",
                                  "email": "student-window-closed@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-WIN-02",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long closedSubjectId = subjectRepository.findByCodeIgnoreCase("CLS607").orElseThrow().getId();
        Long draftSubjectId = subjectRepository.findByCodeIgnoreCase("DRF608").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-WIN-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "enrollmentCloseAt": "2000-01-01T00:00:00Z",
                                  "status": "OPEN"
                                }
                                """.formatted(closedSubjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "DRAFT"
                                }
                                """.formatted(draftSubjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        java.util.List<Long> offeringIds = courseOfferingRepository.findAllBySectionId(
                        section.getId(),
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                10,
                                org.springframework.data.domain.Sort.by("id").ascending()
                        )
                )
                .stream()
                .map(offering -> offering.getId())
                .toList();
        Long closedOfferingId = offeringIds.get(0);
        Long draftOfferingId = offeringIds.get(1);

        String studentToken = extractAccessToken("student-window-closed", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(closedOfferingId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enrollment window has closed for this offering"));

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(draftOfferingId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enrollment is only allowed for OPEN offerings"));
    }

    @Test
    void studentCannotEnrollWhenPrerequisitesAreMissing() throws Exception {
        String adminAccessToken = extractAccessToken();

        AcademicTerm previousTerm = new AcademicTerm();
        previousTerm.setName("Semester 0");
        previousTerm.setAcademicYear("2025-2026");
        previousTerm.setStartDate(LocalDate.of(2025, 1, 1));
        previousTerm.setEndDate(LocalDate.of(2025, 5, 30));
        previousTerm.setStatus("COMPLETED");
        academicTermRepository.save(previousTerm);

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DSA701",
                                  "name": "Data Structures Advanced",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ALG702",
                                  "name": "Algorithms",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-prereq-missing",
                                  "email": "teacher-prereq-missing@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-PREREQ-01",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-prereq-missing",
                                  "email": "student-prereq-missing@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-PREREQ-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long prerequisiteSubjectId = subjectRepository.findByCodeIgnoreCase("DSA701").orElseThrow().getId();
        Long targetSubjectId = subjectRepository.findByCodeIgnoreCase("ALG702").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-PREREQ-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        SubjectPrerequisite subjectPrerequisite = new SubjectPrerequisite();
        subjectPrerequisite.setSubject(subjectRepository.findById(targetSubjectId).orElseThrow());
        subjectPrerequisite.setPrerequisiteSubject(subjectRepository.findById(prerequisiteSubjectId).orElseThrow());
        subjectPrerequisiteRepository.save(subjectPrerequisite);

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(targetSubjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long targetOfferingId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentToken = extractAccessToken("student-prereq-missing", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(targetOfferingId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Missing prerequisite subjects: DSA701"));
    }

    @Test
    void studentCanEnrollWhenPrerequisiteWasSatisfiedInEarlierCompletedTerm() throws Exception {
        String adminAccessToken = extractAccessToken();

        AcademicTerm previousTerm = new AcademicTerm();
        previousTerm.setName("Semester 0");
        previousTerm.setAcademicYear("2025-2026");
        previousTerm.setStartDate(LocalDate.of(2025, 1, 1));
        previousTerm.setEndDate(LocalDate.of(2025, 5, 30));
        previousTerm.setStatus("COMPLETED");
        academicTermRepository.save(previousTerm);

        Section previousSection = new Section();
        previousSection.setProgram(program);
        previousSection.setName("PREV");
        previousSection.setCurrentTerm(previousTerm);
        sectionRepository.save(previousSection);

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DB701",
                                  "name": "Database Foundations",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DB702",
                                  "name": "Database Advanced",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-prereq-pass",
                                  "email": "teacher-prereq-pass@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-PREREQ-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-prereq-pass",
                                  "email": "student-prereq-pass@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-PREREQ-02",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long prerequisiteSubjectId = subjectRepository.findByCodeIgnoreCase("DB701").orElseThrow().getId();
        Long targetSubjectId = subjectRepository.findByCodeIgnoreCase("DB702").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-PREREQ-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        SubjectPrerequisite subjectPrerequisite = new SubjectPrerequisite();
        subjectPrerequisite.setSubject(subjectRepository.findById(targetSubjectId).orElseThrow());
        subjectPrerequisite.setPrerequisiteSubject(subjectRepository.findById(prerequisiteSubjectId).orElseThrow());
        subjectPrerequisiteRepository.save(subjectPrerequisite);

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(prerequisiteSubjectId, previousTerm.getId(), previousSection.getId(), teacherId)))
                .andExpect(status().isOk());

        Long prerequisiteOfferingId = courseOfferingRepository.findAllBySectionId(previousSection.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();
        Enrollment prerequisiteEnrollment = new Enrollment();
        prerequisiteEnrollment.setStudent(studentRepository.findByUserId(
                userRepository.findByUsername("student-prereq-pass").orElseThrow().getId()
        ).orElseThrow());
        prerequisiteEnrollment.setCourseOffering(courseOfferingRepository.findById(prerequisiteOfferingId).orElseThrow());
        prerequisiteEnrollment.setStatus(EnrollmentStatus.ENROLLED);
        prerequisiteEnrollment.setEnrolledAt(previousTerm.getEndDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        enrollmentRepository.save(prerequisiteEnrollment);

        String studentToken = extractAccessToken("student-prereq-pass", "Student@123");

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(targetSubjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long targetOfferingId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .map(offering -> offering.getId())
                .max(Long::compareTo)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(targetOfferingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.courseOffering.subject.code").value("DB702"));
    }

    @Test
    void assignedTeacherCanManageExamsAndMarksForEnrolledStudents() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "EX701",
                                  "name": "Advanced Evaluation Systems",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-exam-owner",
                                  "email": "teacher-exam-owner@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-EXAM-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-marked",
                                  "email": "student-marked@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-MARK-01",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("EX701").orElseThrow().getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-EXAM-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long studentId = studentRepository.findAll().stream()
                .filter(student -> "STU-MARK-01".equals(student.getStudentCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), teacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String studentToken = extractAccessToken("student-marked", "Student@123");
        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENROLLED"));

        String teacherToken = extractAccessToken("teacher-exam-owner", "Teacher@123");
        mockMvc.perform(post("/api/v1/teacher/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "title": "Midterm",
                                  "examType": "written",
                                  "maxMarks": 100.00,
                                  "weightage": 30.00,
                                  "scheduledAt": "2026-08-01T09:00:00Z"
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Midterm"))
                .andExpect(jsonPath("$.data.examType").value("WRITTEN"))
                .andExpect(jsonPath("$.data.maxMarks").value(100.0))
                .andExpect(jsonPath("$.data.weightage").value(30.0));

        Long examId = examRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/teacher/exams/{examId}/marks", examId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "marksObtained": 84.50,
                                  "remarks": "Strong paper"
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.id").value(studentId))
                .andExpect(jsonPath("$.data.marksObtained").value(84.5))
                .andExpect(jsonPath("$.data.remarks").value("Strong paper"));

        Long markEntryId = markEntryRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/v1/teacher/exams/{examId}/marks/{markEntryId}", examId, markEntryId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "marksObtained": 88.00,
                                  "remarks": "Updated after recheck"
                                }
                                """.formatted(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marksObtained").value(88.0))
                .andExpect(jsonPath("$.data.remarks").value("Updated after recheck"));

        mockMvc.perform(get("/api/v1/teacher/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("courseOfferingId", String.valueOf(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Midterm"));

        mockMvc.perform(get("/api/v1/teacher/exams/{examId}/marks", examId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].student.id").value(studentId))
                .andExpect(jsonPath("$.data.content[0].marksObtained").value(88.0));
    }

    @Test
    void teacherCannotManageExamOrMarksForUnassignedOfferingOrNonEnrolledStudent() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "EX702",
                                  "name": "Secure Assessment Models",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-exam-assigned",
                                  "email": "teacher-exam-assigned@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-EXAM-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-exam-other",
                                  "email": "teacher-exam-other@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-EXAM-03",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-not-enrolled",
                                  "email": "student-not-enrolled@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-MARK-02",
                                  "departmentId": %d,
                                  "programId": %d,
                                  "currentTermId": %d,
                                  "sectionId": %d,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "2026-04-10"
                                }
                                """.formatted(department.getId(), program.getId(), term.getId(), section.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("EX702").orElseThrow().getId();
        Long assignedTeacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-EXAM-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long studentId = studentRepository.findAll().stream()
                .filter(student -> "STU-MARK-02".equals(student.getStudentCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), section.getId(), assignedTeacherId)))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(section.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String otherTeacherToken = extractAccessToken("teacher-exam-other", "Teacher@123");
        mockMvc.perform(post("/api/v1/teacher/exams")
                        .header("Authorization", "Bearer " + otherTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "title": "Unauthorized Midterm",
                                  "examType": "written",
                                  "maxMarks": 100.00,
                                  "weightage": 25.00,
                                  "scheduledAt": "2026-08-10T09:00:00Z"
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course offering not found"));

        String assignedTeacherToken = extractAccessToken("teacher-exam-assigned", "Teacher@123");
        mockMvc.perform(post("/api/v1/teacher/exams")
                        .header("Authorization", "Bearer " + assignedTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "title": "Final Exam",
                                  "examType": "practical",
                                  "maxMarks": 100.00,
                                  "weightage": 40.00,
                                  "scheduledAt": "2026-09-15T09:00:00Z"
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examType").value("PRACTICAL"));

        Long examId = examRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/teacher/exams/{examId}/marks", examId)
                        .header("Authorization", "Bearer " + assignedTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "marksObtained": 76.00,
                                  "remarks": "Should fail"
                                }
                                """.formatted(studentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Marks can only be entered for actively enrolled students"));
    }

    private String extractAccessToken() throws Exception {
        return extractAccessToken("admin", "Admin@123");
    }

    private String extractAccessToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"accessToken\":\"")[1].split("\"")[0];
    }
}
