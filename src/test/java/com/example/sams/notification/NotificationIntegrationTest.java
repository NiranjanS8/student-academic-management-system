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
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.domain.CourseOfferingStatus;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

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
    private MarkEntryRepository markEntryRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private SemesterFeeRepository semesterFeeRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

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

    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;
    private Subject subject;
    private Teacher teacher;
    private Student student;
    private CourseOffering offering;

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

        subject = new Subject();
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

        teacher = createTeacher();
        student = createStudent();
        offering = createOffering();
    }

    @Test
    void enrollmentCreatesNotificationAndStudentCanMarkItRead() throws Exception {
        String studentToken = extractAccessToken("student-notify", "Student@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offering.getId())))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ENROLLMENT_CONFIRMED"))
                .andExpect(jsonPath("$.data.content[0].title").value("Enrollment confirmed"))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listJson = objectMapper.readTree(listResponse);
        long notificationId = listJson.path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/notifications/me/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(get("/api/v1/notifications/me/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void resultPublicationAndTeacherAnnouncementCreateNotifications() throws Exception {
        String studentToken = extractAccessToken("student-notify", "Student@123");
        String teacherToken = extractAccessToken("teacher-notify", "Teacher@123");

        mockMvc.perform(post("/api/v1/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d
                                }
                                """.formatted(offering.getId())))
                .andExpect(status().isOk());

        String examResponse = mockMvc.perform(post("/api/v1/teacher/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "title": "Midterm",
                                  "examType": "THEORY",
                                  "maxMarks": 100,
                                  "weightage": 40,
                                  "scheduledAt": "2026-07-10T10:00:00Z"
                                }
                                """.formatted(offering.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long examId = objectMapper.readTree(examResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/teacher/exams/{examId}/marks", examId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": %d,
                                  "marksObtained": 81,
                                  "remarks": "Strong performance"
                                }
                                """.formatted(student.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teacher/exams/{examId}/publish", examId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/teacher/notifications/announcements")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "title": "Class update",
                                  "message": "Please review the published marks before next class."
                                }
                                """.formatted(offering.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientCount").value(1));

        mockMvc.perform(get("/api/v1/notifications/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].type").value("TEACHER_ANNOUNCEMENT"))
                .andExpect(jsonPath("$.data.content[1].type").value("RESULT_PUBLISHED"))
                .andExpect(jsonPath("$.data.content[2].type").value("ENROLLMENT_CONFIRMED"));
    }

    private Teacher createTeacher() {
        User teacherUser = new User();
        teacherUser.setUsername("teacher-notify");
        teacherUser.setEmail("teacher-notify@sams.local");
        teacherUser.setPasswordHash(passwordEncoder.encode("Teacher@123"));
        teacherUser.setRole(Role.TEACHER);
        teacherUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(teacherUser);

        Teacher entity = new Teacher();
        entity.setUser(teacherUser);
        entity.setEmployeeCode("EMP-NOTIFY");
        entity.setDepartment(department);
        entity.setDesignation("Assistant Professor");
        return teacherRepository.save(entity);
    }

    private Student createStudent() {
        User studentUser = new User();
        studentUser.setUsername("student-notify");
        studentUser.setEmail("student-notify@sams.local");
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(studentUser);

        Student entity = new Student();
        entity.setUser(studentUser);
        entity.setStudentCode("STU-NOTIFY");
        entity.setDepartment(department);
        entity.setProgram(program);
        entity.setCurrentTerm(term);
        entity.setSection(section);
        entity.setAcademicStatus(AcademicStatus.ACTIVE);
        entity.setAdmissionDate(LocalDate.of(2026, 4, 10));
        return studentRepository.save(entity);
    }

    private CourseOffering createOffering() {
        CourseOffering entity = new CourseOffering();
        entity.setSubject(subject);
        entity.setTerm(term);
        entity.setSection(section);
        entity.setTeacher(teacher);
        entity.setCapacity(30);
        entity.setRoomCode("R-101");
        entity.setScheduleDays("MON,WED");
        entity.setScheduleStartTime(LocalTime.of(10, 0));
        entity.setScheduleEndTime(LocalTime.of(11, 0));
        entity.setEnrollmentOpenAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setEnrollmentCloseAt(Instant.parse("2027-01-01T00:00:00Z"));
        entity.setStatus(CourseOfferingStatus.OPEN);
        return courseOfferingRepository.save(entity);
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
