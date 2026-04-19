package com.example.sams.notification;

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
import com.example.sams.attendance.repository.AttendanceRecordRepository;
import com.example.sams.attendance.repository.AttendanceSessionRepository;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.fee.repository.FeeStructureRepository;
import com.example.sams.fee.repository.PaymentRecordRepository;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.notification.domain.Notification;
import com.example.sams.notification.domain.NotificationType;
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.notification.service.NotificationCleanupSchedulerService;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.AcademicStatus;
import com.example.sams.user.domain.Role;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
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
class NotificationLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

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
    private NotificationCleanupSchedulerService notificationCleanupSchedulerService;

    private User studentUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        attendanceRecordRepository.deleteAll();
        attendanceSessionRepository.deleteAll();
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

        Department department = new Department();
        department.setCode("CSE");
        department.setName("Computer Science");
        departmentRepository.save(department);

        Program program = new Program();
        program.setCode("BTECH-CSE");
        program.setName("BTech CSE");
        program.setDepartment(department);
        programRepository.save(program);

        AcademicTerm term = new AcademicTerm();
        term.setName("Semester 1");
        term.setAcademicYear("2026-2027");
        term.setStartDate(LocalDate.of(2026, 6, 1));
        term.setEndDate(LocalDate.of(2026, 11, 30));
        term.setStatus("ACTIVE");
        academicTermRepository.save(term);

        Section section = new Section();
        section.setProgram(program);
        section.setName("A");
        section.setCurrentTerm(term);
        sectionRepository.save(section);

        Subject subject = new Subject();
        subject.setDepartment(department);
        subject.setCode("SE601");
        subject.setName("Software Engineering");
        subject.setCredits(new BigDecimal("3.00"));
        subject.setActive(true);
        subjectRepository.save(subject);

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@sams.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);

        studentUser = new User();
        studentUser.setUsername("student-notification-lifecycle");
        studentUser.setEmail("student-notification-lifecycle@sams.local");
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(studentUser);

        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentCode("STU-NOTIFY-LIFE");
        student.setDepartment(department);
        student.setProgram(program);
        student.setCurrentTerm(term);
        student.setSection(section);
        student.setAcademicStatus(AcademicStatus.ACTIVE);
        student.setAdmissionDate(LocalDate.of(2026, 4, 10));
        studentRepository.save(student);
    }

    @Test
    void userCanMarkUnreadAndMarkAllRead() throws Exception {
        Notification first = createNotification("Welcome", false, null);
        Notification second = createNotification("Reminder", false, null);

        String accessToken = extractAccessToken("student-notification-lifecycle", "Student@123");

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", first.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/unread", first.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(false))
                .andExpect(jsonPath("$.data.readAt").doesNotExist());

        mockMvc.perform(post("/api/v1/notifications/me/read-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affectedCount").value(2));

        mockMvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("read", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/v1/notifications/me/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void cleanupJobDeletesOnlyStaleReadNotifications() {
        Notification staleRead = createNotification("Old read", true, Instant.now().minusSeconds(40L * 24 * 60 * 60));
        Notification recentRead = createNotification("Recent read", true, Instant.now().minusSeconds(5L * 24 * 60 * 60));
        Notification unread = createNotification("Unread", false, null);

        long deleted = notificationCleanupSchedulerService.processCleanup(Instant.now());

        assertThat(deleted).isEqualTo(1);
        assertThat(notificationRepository.findById(staleRead.getId())).isEmpty();
        assertThat(notificationRepository.findById(recentRead.getId())).isPresent();
        assertThat(notificationRepository.findById(unread.getId())).isPresent();
    }

    private Notification createNotification(String title, boolean read, Instant readAt) {
        Notification notification = new Notification();
        notification.setUser(studentUser);
        notification.setType(NotificationType.TEACHER_ANNOUNCEMENT);
        notification.setTitle(title);
        notification.setMessage(title + " message");
        notification.setRead(read);
        notification.setReadAt(readAt);
        return notificationRepository.save(notification);
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

        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("accessToken").asText();
    }
}
