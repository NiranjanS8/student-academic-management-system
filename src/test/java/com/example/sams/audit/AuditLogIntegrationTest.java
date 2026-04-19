package com.example.sams.audit;

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
import com.example.sams.attendance.repository.AttendanceRecordRepository;
import com.example.sams.attendance.repository.AttendanceSessionRepository;
import com.example.sams.audit.repository.AuditLogRepository;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.fee.domain.FeeCategory;
import com.example.sams.fee.domain.FeeStructure;
import com.example.sams.fee.repository.FeeStructureRepository;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private MarkEntryRepository markEntryRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private SemesterFeeRepository semesterFeeRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectPrerequisiteRepository subjectPrerequisiteRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AcademicTermRepository academicTermRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;

    @BeforeEach
    void setUp() {
        clearDatabase();

        department = new Department();
        department.setCode("CSE");
        department.setName("Computer Science");
        department = departmentRepository.save(department);

        program = new Program();
        program.setCode("BTECH-CSE");
        program.setName("BTech CSE");
        program.setDepartment(department);
        program = programRepository.save(program);

        term = new AcademicTerm();
        term.setName("Semester 1");
        term.setAcademicYear("2026-2027");
        term.setStartDate(LocalDate.now());
        term.setEndDate(LocalDate.now().plusMonths(4));
        term.setStatus("ACTIVE");
        term = academicTermRepository.save(term);

        section = new Section();
        section.setProgram(program);
        section.setName("A");
        section.setCurrentTerm(term);
        section = sectionRepository.save(section);

        FeeStructure feeStructure = new FeeStructure();
        feeStructure.setProgram(program);
        feeStructure.setTerm(term);
        feeStructure.setName("Tuition Fee");
        feeStructure.setFeeCategory(FeeCategory.TUITION);
        feeStructure.setAmount(new BigDecimal("5000.00"));
        feeStructure.setDueDaysFromTermStart(5);
        feeStructure.setDescription("Semester tuition");
        feeStructure.setActive(true);
        feeStructureRepository.save(feeStructure);

        admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin = userRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    void adminCanInspectAuditTrailForOperationalActions() throws Exception {
        String adminToken = extractAccessToken("admin", "Admin@123");

        createTeacher(adminToken);
        Long studentId = createStudent(adminToken);
        Long semesterFeeId = generateSemesterFee(adminToken, studentId);
        recordPayment(adminToken, semesterFeeId);

        mockMvc.perform(get("/api/v1/admin/audit/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sortBy", "id")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[0].actionType").value("USER_CREATED"))
                .andExpect(jsonPath("$.data.content[0].entityType").value("TEACHER"))
                .andExpect(jsonPath("$.data.content[1].entityType").value("STUDENT"))
                .andExpect(jsonPath("$.data.content[2].actionType").value("SEMESTER_FEE_GENERATED"))
                .andExpect(jsonPath("$.data.content[3].actionType").value("PAYMENT_RECORDED"))
                .andExpect(jsonPath("$.data.content[3].actorUserId").value(admin.getId()))
                .andExpect(jsonPath("$.data.content[3].actorRole").value("ADMIN"));

        mockMvc.perform(get("/api/v1/admin/audit/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("actionType", "PAYMENT_RECORDED")
                        .param("entityType", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].summary").value(org.hamcrest.Matchers.containsString("PAY-001")));

        mockMvc.perform(get("/api/v1/admin/audit/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("actorUserId", String.valueOf(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        mockMvc.perform(get("/api/v1/admin/audit/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("createdFrom", LocalDate.now().toString())
                        .param("createdTo", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));

        mockMvc.perform(get("/api/v1/admin/audit/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("createdFrom", LocalDate.now().plusDays(1).toString())
                        .param("createdTo", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/admin/audit/logs/export")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("createdFrom", LocalDate.now().toString())
                        .param("createdTo", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id,actionType,actorUserId")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAYMENT_RECORDED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAY-001")));
    }

    private void createTeacher(String adminToken) throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-audit",
                                  "email": "teacher-audit@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-AUD-1",
                                  "departmentId": %s,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileCode").value("EMP-AUD-1"));
    }

    private Long createStudent(String adminToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/users/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student-audit",
                                  "email": "student-audit@sams.local",
                                  "password": "Student@123",
                                  "studentCode": "STU-AUD-1",
                                  "departmentId": %s,
                                  "programId": %s,
                                  "currentTermId": %s,
                                  "sectionId": %s,
                                  "academicStatus": "ACTIVE",
                                  "admissionDate": "%s"
                                }
                                """.formatted(
                                department.getId(),
                                program.getId(),
                                term.getId(),
                                section.getId(),
                                LocalDate.now().minus(30, ChronoUnit.DAYS)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileCode").value("STU-AUD-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("profileId").asLong();
    }

    private Long generateSemesterFee(String adminToken, Long studentId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/fees/semester-fees/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %s,
                                  "termId": %s
                                }
                                """.formatted(studentId, term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPayable").value(5000.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("id").asLong();
    }

    private void recordPayment(String adminToken, Long semesterFeeId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/{semesterFeeId}/payments", semesterFeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "PAY-001",
                                  "amount": 1200.00,
                                  "paymentMethod": "UPI",
                                  "paidAt": "2026-04-19T09:30:00Z",
                                  "remarks": "Initial installment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-001"))
                .andExpect(jsonPath("$.data.amount").value(1200.00));
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

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private void clearDatabase() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        attendanceRecordRepository.deleteAll();
        attendanceSessionRepository.deleteAll();
        markEntryRepository.deleteAll();
        examRepository.deleteAll();
        enrollmentRepository.deleteAll();
        paymentRecordRepository.deleteAll();
        semesterFeeRepository.deleteAll();
        feeStructureRepository.deleteAll();
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
    }
}
