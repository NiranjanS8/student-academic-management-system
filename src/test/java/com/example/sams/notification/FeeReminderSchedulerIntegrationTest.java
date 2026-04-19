package com.example.sams.notification;

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
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
import com.example.sams.fee.repository.FeeStructureRepository;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.notification.service.FeeReminderSchedulerService;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeeReminderSchedulerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private SemesterFeeRepository semesterFeeRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private MarkEntryRepository markEntryRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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

    @Autowired
    private FeeReminderSchedulerService feeReminderSchedulerService;

    private Department department;
    private Program program;
    private Student student;

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

        createAdmin();
        student = createStudent();
    }

    @Test
    void feeReminderJobCreatesUpcomingDueTodayAndOverdueNotificationsWithoutDuplicates() throws Exception {
        LocalDate today = LocalDate.now();

        createSemesterFee(createTerm("Semester 1", "2026-2027", today.minusDays(30), today.plusDays(60)), today.plusDays(3));
        createSemesterFee(createTerm("Semester 2", "2026-2027", today.minusDays(90), today.plusDays(1)), today);
        createSemesterFee(createTerm("Semester 3", "2025-2026", today.minusDays(180), today.minusDays(1)), today.minusDays(1));

        int firstRunCount = feeReminderSchedulerService.processFeeDueReminders(today);
        int secondRunCount = feeReminderSchedulerService.processFeeDueReminders(today);

        assertThat(firstRunCount).isEqualTo(3);
        assertThat(secondRunCount).isZero();
        assertThat(notificationRepository.count()).isEqualTo(3);
        assertThat(notificationRepository.findAll())
                .extracting(notification -> notification.getTitle())
                .containsExactlyInAnyOrder("Fee due reminder", "Fee due today", "Fee overdue");

        String studentToken = extractAccessToken("student-fee-reminder", "Student@123");

        mockMvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].type").value("FEE_DUE_REMINDER"));
    }

    private void createAdmin() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
    }

    private Student createStudent() {
        AcademicTerm currentTerm = createTerm(
                "Current Semester",
                "2026-2027",
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(90)
        );

        Section section = new Section();
        section.setProgram(program);
        section.setName("A");
        section.setCurrentTerm(currentTerm);
        sectionRepository.save(section);

        User studentUser = new User();
        studentUser.setUsername("student-fee-reminder");
        studentUser.setEmail("student-fee-reminder@sams.local");
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(studentUser);

        Student entity = new Student();
        entity.setUser(studentUser);
        entity.setStudentCode("STU-FEE-REMINDER");
        entity.setDepartment(department);
        entity.setProgram(program);
        entity.setCurrentTerm(currentTerm);
        entity.setSection(section);
        entity.setAcademicStatus(AcademicStatus.ACTIVE);
        entity.setAdmissionDate(LocalDate.of(2026, 4, 10));
        return studentRepository.save(entity);
    }

    private AcademicTerm createTerm(String name, String academicYear, LocalDate startDate, LocalDate endDate) {
        AcademicTerm term = new AcademicTerm();
        term.setName(name);
        term.setAcademicYear(academicYear);
        term.setStartDate(startDate);
        term.setEndDate(endDate);
        term.setStatus("ACTIVE");
        return academicTermRepository.save(term);
    }

    private void createSemesterFee(AcademicTerm term, LocalDate dueDate) {
        SemesterFee semesterFee = new SemesterFee();
        semesterFee.setStudent(student);
        semesterFee.setTerm(term);
        semesterFee.setBaseAmount(new BigDecimal("10000.00"));
        semesterFee.setFineAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setTotalPayable(new BigDecimal("10000.00"));
        semesterFee.setPaidAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setDueDate(dueDate);
        semesterFee.setStatus(SemesterFeeStatus.DUE);
        semesterFeeRepository.save(semesterFee);
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
}
