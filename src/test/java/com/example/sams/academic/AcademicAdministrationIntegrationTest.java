package com.example.sams.academic;

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
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.user.domain.AccountStatus;
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
class AcademicAdministrationIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private Department department;
    private Program program;
    private AcademicTerm term;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        studentRepository.deleteAll();
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

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
    }

    @Test
    void adminCanManageDepartmentsAndPrograms() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/departments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "EEE",
                                  "name": "Electrical Engineering"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("EEE"))
                .andExpect(jsonPath("$.data.name").value("Electrical Engineering"));

        Department createdDepartment = departmentRepository.findByCodeIgnoreCase("EEE").orElseThrow();

        mockMvc.perform(put("/api/v1/admin/academic/departments/{departmentId}", createdDepartment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ECE",
                                  "name": "Electronics Engineering"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ECE"))
                .andExpect(jsonPath("$.data.name").value("Electronics Engineering"));

        mockMvc.perform(post("/api/v1/admin/academic/programs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BTECH-ECE",
                                  "name": "BTech ECE",
                                  "departmentId": %d
                                }
                                """.formatted(createdDepartment.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("BTECH-ECE"))
                .andExpect(jsonPath("$.data.department.code").value("ECE"));

        mockMvc.perform(get("/api/v1/admin/academic/departments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/admin/academic/programs")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("departmentId", String.valueOf(createdDepartment.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].department.id").value(createdDepartment.getId()));
    }

    @Test
    void adminCanManageAcademicTermsAndSections() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/terms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Semester 2",
                                  "academicYear": "2026-2027",
                                  "startDate": "2026-12-01",
                                  "endDate": "2027-04-30",
                                  "status": "planned"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Semester 2"))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));

        AcademicTerm createdTerm = academicTermRepository.findByNameIgnoreCaseAndAcademicYear("Semester 2", "2026-2027")
                .orElseThrow();

        mockMvc.perform(post("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "B",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), createdTerm.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("B"))
                .andExpect(jsonPath("$.data.program.code").value("BTECH-CSE"))
                .andExpect(jsonPath("$.data.currentTerm.name").value("Semester 2"));

        Section createdSection = sectionRepository.findAll().stream()
                .filter(section -> "B".equals(section.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(put("/api/v1/admin/academic/sections/{sectionId}", createdSection.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "B1",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), createdTerm.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("B1"));

        mockMvc.perform(get("/api/v1/admin/academic/terms")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("programId", String.valueOf(program.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        assertThat(sectionRepository.findById(createdSection.getId())).isPresent();
    }

    @Test
    void adminCannotCreateAcademicTermWithInvalidDates() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/terms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Broken Term",
                                  "academicYear": "2026-2027",
                                  "startDate": "2027-05-01",
                                  "endDate": "2027-04-01",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Academic term endDate cannot be before startDate"));
    }

    @Test
    void adminCanManageSubjectsAndPrerequisites() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DBMS101",
                                  "name": "Database Management Systems",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DBMS101"))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DSA100",
                                  "name": "Data Structures",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DSA100"));

        Long dbmsId = subjectRepository.findByCodeIgnoreCase("DBMS101").orElseThrow().getId();
        Long dsaId = subjectRepository.findByCodeIgnoreCase("DSA100").orElseThrow().getId();

        mockMvc.perform(put("/api/v1/admin/academic/subjects/{subjectId}", dbmsId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DBMS201",
                                  "name": "Advanced DBMS",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": false
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DBMS201"))
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(post("/api/v1/admin/academic/subjects/{subjectId}/prerequisites", dbmsId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prerequisiteSubjectId": %d
                                }
                                """.formatted(dsaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prerequisites[0].code").value("DSA100"));

        mockMvc.perform(get("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("departmentId", String.valueOf(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/admin/academic/subjects/{subjectId}", dbmsId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prerequisites[0].code").value("DSA100"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/admin/academic/subjects/{subjectId}/prerequisites/{prerequisiteSubjectId}",
                        dbmsId,
                        dsaId
                ).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prerequisites").isEmpty());
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
