package com.example.sams.fee;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.repository.AcademicTermRepository;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.academic.repository.ProgramRepository;
import com.example.sams.academic.repository.SectionRepository;
import com.example.sams.academic.repository.SubjectPrerequisiteRepository;
import com.example.sams.academic.repository.SubjectRepository;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.fee.repository.FeeStructureRepository;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.offering.repository.CourseOfferingRepository;
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
class FeeAdministrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private MarkEntryRepository markEntryRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

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
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private SemesterFeeRepository semesterFeeRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department department;
    private Program program;
    private AcademicTerm term;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        paymentRecordRepository.deleteAll();
        semesterFeeRepository.deleteAll();
        feeStructureRepository.deleteAll();
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

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
    }

    @Test
    void adminCanCreateUpdateListAndDeactivateFeeStructures() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Core Tuition Fee",
                                  "feeCategory": "tuition",
                                  "amount": 85000.00,
                                  "dueDaysFromTermStart": 15,
                                  "description": "Main semester tuition",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Core Tuition Fee"))
                .andExpect(jsonPath("$.data.feeCategory").value("TUITION"))
                .andExpect(jsonPath("$.data.program.code").value("BTECH-CSE"))
                .andExpect(jsonPath("$.data.term.name").value("Semester 1"))
                .andExpect(jsonPath("$.data.active").value(true));

        Long feeStructureId = feeStructureRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/v1/admin/fees/structures/{feeStructureId}", feeStructureId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Core Tuition Fee Revised",
                                  "feeCategory": "tuition",
                                  "amount": 90000.00,
                                  "dueDaysFromTermStart": 20,
                                  "description": "Updated tuition plan",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Core Tuition Fee Revised"))
                .andExpect(jsonPath("$.data.amount").value(90000.0))
                .andExpect(jsonPath("$.data.dueDaysFromTermStart").value(20));

        mockMvc.perform(get("/api/v1/admin/fees/structures/{feeStructureId}", feeStructureId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated tuition plan"));

        mockMvc.perform(get("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("programId", String.valueOf(program.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(feeStructureId));

        mockMvc.perform(post("/api/v1/admin/fees/structures/{feeStructureId}/deactivate", feeStructureId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        assertThat(feeStructureRepository.findById(feeStructureId)).get().extracting("active").isEqualTo(false);
    }

    @Test
    void adminCannotCreateDuplicateActiveFeeStructureForSameProgramTermAndCategory() throws Exception {
        String accessToken = extractAccessToken();

        String requestBody = """
                {
                  "programId": %d,
                  "termId": %d,
                  "name": "Semester Tuition",
                  "feeCategory": "tuition",
                  "amount": 78000.00,
                  "dueDaysFromTermStart": 10,
                  "description": "Initial plan",
                  "active": true
                }
                """.formatted(program.getId(), term.getId());

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Semester Tuition Alternate",
                                  "feeCategory": "tuition",
                                  "amount": 81000.00,
                                  "dueDaysFromTermStart": 12,
                                  "description": "Should fail",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An active fee structure already exists for this program, term, and category"));

        Long existingId = feeStructureRepository.findAll().stream().findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/fees/structures/{feeStructureId}/deactivate", existingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Semester Tuition Replacement",
                                  "feeCategory": "tuition",
                                  "amount": 81000.00,
                                  "dueDaysFromTermStart": 12,
                                  "description": "Replacement active structure",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
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
