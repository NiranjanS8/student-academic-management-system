package com.example.sams.auth;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.academic.repository.SubjectPrerequisiteRepository;
import com.example.sams.academic.repository.SubjectRepository;
import com.example.sams.auth.domain.RefreshToken;
import com.example.sams.auth.repository.RefreshTokenRepository;
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
    private PasswordEncoder passwordEncoder;

    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
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

    private String extractAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"accessToken\":\"")[1].split("\"")[0];
    }
}
