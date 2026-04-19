package com.example.sams.fee;

import com.example.sams.academic.domain.AcademicTerm;
import com.example.sams.academic.domain.Department;
import com.example.sams.academic.domain.Program;
import com.example.sams.academic.domain.Section;
import com.example.sams.academic.domain.Subject;
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
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.Teacher;
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

    @Autowired
    private NotificationRepository notificationRepository;

    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
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

    @Test
    void adminCanGenerateSemesterFeeAndRecordPartialAndFullPayments() throws Exception {
        String accessToken = extractAccessToken();
        Student student = createStudent("student-fee-01", "student-fee-01@sams.local", "STU-FEE-01");

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Tuition Fee",
                                  "feeCategory": "tuition",
                                  "amount": 50000.00,
                                  "dueDaysFromTermStart": 12,
                                  "description": "Core tuition",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Exam Fee",
                                  "feeCategory": "exam",
                                  "amount": 5000.00,
                                  "dueDaysFromTermStart": 7,
                                  "description": "Exam processing",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "termId": %d
                                }
                                """.formatted(student.getId(), term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.studentCode").value("STU-FEE-01"))
                .andExpect(jsonPath("$.data.baseAmount").value(55000.0))
                .andExpect(jsonPath("$.data.totalPayable").value(55000.0))
                .andExpect(jsonPath("$.data.paidAmount").value(0.0))
                .andExpect(jsonPath("$.data.outstandingAmount").value(55000.0))
                .andExpect(jsonPath("$.data.dueDate").value("2026-06-08"))
                .andExpect(jsonPath("$.data.status").value("DUE"));

        Long semesterFeeId = semesterFeeRepository.findAll().stream().findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/{semesterFeeId}/payments", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "PAY-001",
                                  "amount": 20000.00,
                                  "paymentMethod": "upi",
                                  "paidAt": "2026-06-05T10:00:00Z",
                                  "remarks": "First installment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-001"))
                .andExpect(jsonPath("$.data.amount").value(20000.0))
                .andExpect(jsonPath("$.data.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.data.paymentStatus").value("RECORDED"));

        mockMvc.perform(get("/api/v1/admin/fees/semester-fees/{semesterFeeId}", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(20000.0))
                .andExpect(jsonPath("$.data.outstandingAmount").value(35000.0))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"));

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/{semesterFeeId}/payments", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "PAY-002",
                                  "amount": 35000.00,
                                  "paymentMethod": "bank_transfer",
                                  "paidAt": "2026-06-10T10:00:00Z",
                                  "remarks": "Final settlement"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-002"))
                .andExpect(jsonPath("$.data.amount").value(35000.0))
                .andExpect(jsonPath("$.data.paymentMethod").value("BANK_TRANSFER"));

        mockMvc.perform(get("/api/v1/admin/fees/semester-fees")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("studentId", String.valueOf(student.getId()))
                        .param("termId", String.valueOf(term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.content[0].paidAmount").value(55000.0))
                .andExpect(jsonPath("$.data.content[0].outstandingAmount").value(0.0));

        mockMvc.perform(get("/api/v1/admin/fees/semester-fees/{semesterFeeId}/payments", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].paymentReference").value("PAY-002"))
                .andExpect(jsonPath("$.data.content[1].paymentReference").value("PAY-001"));
    }

    @Test
    void adminCannotGenerateDuplicateSemesterFeeOrOverpayOutstandingBalance() throws Exception {
        String accessToken = extractAccessToken();
        Student student = createStudent("student-fee-02", "student-fee-02@sams.local", "STU-FEE-02");

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Transport Fee",
                                  "feeCategory": "transport",
                                  "amount": 12000.00,
                                  "dueDaysFromTermStart": 14,
                                  "description": "Transport support",
                                  "active": true
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        String generationBody = """
                {
                  "studentId": %d,
                  "termId": %d
                }
                """.formatted(student.getId(), term.getId());

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generationBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generationBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Semester fee already exists for this student and term"));

        Long semesterFeeId = semesterFeeRepository.findByStudentIdAndTermId(student.getId(), term.getId())
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/{semesterFeeId}/payments", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "PAY-OVER",
                                  "amount": 15000.00,
                                  "paymentMethod": "cash",
                                  "paidAt": "2026-06-05T10:00:00Z",
                                  "remarks": "Too much"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment amount cannot exceed the outstanding balance"));
    }

    @Test
    void overdueFeeAppliesFineAndBlocksEnrollmentAndHallTicketEligibility() throws Exception {
        String accessToken = extractAccessToken();
        Student student = createStudent("student-fee-03", "student-fee-03@sams.local", "STU-FEE-03");
        AcademicTerm pastTerm = new AcademicTerm();
        pastTerm.setName("Semester Past");
        pastTerm.setAcademicYear("2025-2026");
        pastTerm.setStartDate(LocalDate.of(2026, 1, 1));
        pastTerm.setEndDate(LocalDate.of(2026, 5, 15));
        pastTerm.setStatus("ACTIVE");
        academicTermRepository.save(pastTerm);
        Section pastSection = new Section();
        pastSection.setProgram(program);
        pastSection.setName("PAST");
        pastSection.setCurrentTerm(pastTerm);
        sectionRepository.save(pastSection);
        student.setCurrentTerm(pastTerm);
        student.setSection(pastSection);
        studentRepository.save(student);

        mockMvc.perform(post("/api/v1/admin/fees/structures")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": %d,
                                  "termId": %d,
                                  "name": "Past Tuition Due",
                                  "feeCategory": "tuition",
                                  "amount": 10000.00,
                                  "dueDaysFromTermStart": 0,
                                  "description": "Old unpaid fee",
                                  "active": true
                                }
                                """.formatted(program.getId(), pastTerm.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/fees/semester-fees/generate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "termId": %d
                                }
                                """.formatted(student.getId(), pastTerm.getId())))
                .andExpect(status().isOk());

        Long semesterFeeId = semesterFeeRepository.findByStudentIdAndTermId(student.getId(), pastTerm.getId())
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/admin/fees/semester-fees/{semesterFeeId}", semesterFeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fineAmount").value(1000.0))
                .andExpect(jsonPath("$.data.totalPayable").value(11000.0))
                .andExpect(jsonPath("$.data.status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.outstandingAmount").value(11000.0));

        String studentToken = extractAccessToken("student-fee-03", "Student@123");

        mockMvc.perform(get("/api/v1/student/fees/eligibility")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("termId", String.valueOf(pastTerm.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrollmentAllowed").value(false))
                .andExpect(jsonPath("$.data.hallTicketEligible").value(false))
                .andExpect(jsonPath("$.data.totalOutstandingAmount").value(11000.0))
                .andExpect(jsonPath("$.data.overdueOutstandingAmount").value(11000.0))
                .andExpect(jsonPath("$.data.blockers[0].status").value("OVERDUE"));

        Subject subject = new Subject();
        subject.setCode("FEE701");
        subject.setName("Finance Restricted Enrollment");
        subject.setCredits(new java.math.BigDecimal("4.00"));
        subject.setDepartment(department);
        subject.setActive(true);
        subjectRepository.save(subject);

        User teacherUser = new User();
        teacherUser.setUsername("teacher-fee-block");
        teacherUser.setEmail("teacher-fee-block@sams.local");
        teacherUser.setPasswordHash(passwordEncoder.encode("Teacher@123"));
        teacherUser.setRole(Role.TEACHER);
        teacherUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(teacherUser);

        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacher.setEmployeeCode("EMP-FEE-BLOCK-01");
        teacher.setDepartment(department);
        teacher.setDesignation("Professor");
        teacherRepository.save(teacher);

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + accessToken)
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
                                """.formatted(subject.getId(), pastTerm.getId(), pastSection.getId(), teacher.getId())))
                .andExpect(status().isOk());

        Long offeringId = courseOfferingRepository.findAllBySectionId(pastSection.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .filter(offering -> offering.getTerm().getId().equals(pastTerm.getId()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offeringId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enrollment blocked due to unpaid dues"));
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

    private Student createStudent(String username, String email, String studentCode) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Student@123"));
        user.setRole(Role.STUDENT);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(studentCode);
        student.setDepartment(department);
        student.setProgram(program);
        student.setCurrentTerm(term);
        student.setSection(section);
        student.setAcademicStatus(AcademicStatus.ACTIVE);
        student.setAdmissionDate(LocalDate.of(2026, 4, 10));
        return studentRepository.save(student);
    }
}
