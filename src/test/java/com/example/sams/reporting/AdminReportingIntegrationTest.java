package com.example.sams.reporting;

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
import com.example.sams.attendance.domain.AttendanceRecord;
import com.example.sams.attendance.domain.AttendanceSession;
import com.example.sams.attendance.domain.AttendanceStatus;
import com.example.sams.attendance.repository.AttendanceRecordRepository;
import com.example.sams.attendance.repository.AttendanceSessionRepository;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.Exam;
import com.example.sams.exam.domain.MarkEntry;
import com.example.sams.exam.repository.ExamRepository;
import com.example.sams.exam.repository.MarkEntryRepository;
import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.domain.SemesterFeeStatus;
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
class AdminReportingIntegrationTest {

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
    private com.example.sams.exam.service.GradeCalculationService gradeCalculationService;

    private Program program;
    private Section section;
    private AcademicTerm term;
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

        Department department = new Department();
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

        Subject subject = new Subject();
        subject.setDepartment(department);
        subject.setCode("SE601");
        subject.setName("Software Engineering");
        subject.setCredits(new BigDecimal("3.00"));
        subject.setActive(true);
        subjectRepository.save(subject);

        createAdmin();
        teacher = createTeacher(department);
        student = createStudent(department);
        offering = createOffering(subject);
        createEnrollment();
        createPublishedExamWithMarks();
        createOutstandingFee();
        createLowAttendance();
    }

    @Test
    void adminCanViewDashboardAndOperationalReports() throws Exception {
        String adminToken = extractAccessToken("admin", "Admin@123");

        mockMvc.perform(get("/api/v1/admin/reports/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(1))
                .andExpect(jsonPath("$.data.totalTeachers").value(1))
                .andExpect(jsonPath("$.data.totalOfferings").value(1))
                .andExpect(jsonPath("$.data.activeEnrollments").value(1))
                .andExpect(jsonPath("$.data.publishedResults").value(1))
                .andExpect(jsonPath("$.data.studentsWithOutstandingDues").value(1))
                .andExpect(jsonPath("$.data.lowAttendanceCases").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/fee-defaulters")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("termId", String.valueOf(term.getId()))
                        .param("query", "STU-REP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].studentCode").value("STU-REP-1"))
                .andExpect(jsonPath("$.data.content[0].outstandingAmount").value(5000.00));

        mockMvc.perform(get("/api/v1/admin/reports/attendance-shortages")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("termId", String.valueOf(term.getId()))
                        .param("programId", String.valueOf(program.getId()))
                        .param("sectionId", String.valueOf(section.getId()))
                        .param("query", "SE601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].studentCode").value("STU-REP-1"))
                .andExpect(jsonPath("$.data.content[0].attendancePercentage").value(33.33));

        mockMvc.perform(get("/api/v1/admin/reports/results-summary")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("termId", String.valueOf(term.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishedExamCount").value(1))
                .andExpect(jsonPath("$.data.offeringsWithPublishedResults").value(1))
                .andExpect(jsonPath("$.data.finalResultsCount").value(1))
                .andExpect(jsonPath("$.data.averageWeightedScore").value(84.00))
                .andExpect(jsonPath("$.data.offerings[0].subjectCode").value("SE601"));

        mockMvc.perform(get("/api/v1/admin/reports/students/{studentId}/academic-snapshot", student.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCode").value("STU-REP-1"))
                .andExpect(jsonPath("$.data.summary.cgpa").value(3.70))
                .andExpect(jsonPath("$.data.summary.totalCompletedCredits").value(3.00))
                .andExpect(jsonPath("$.data.publishedResults[0].subjectCode").value("SE601"))
                .andExpect(jsonPath("$.data.publishedResults[0].resultStatus").value("FINAL"));
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

    private Teacher createTeacher(Department department) {
        User teacherUser = new User();
        teacherUser.setUsername("teacher-report");
        teacherUser.setEmail("teacher-report@sams.local");
        teacherUser.setPasswordHash(passwordEncoder.encode("Teacher@123"));
        teacherUser.setRole(Role.TEACHER);
        teacherUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(teacherUser);

        Teacher entity = new Teacher();
        entity.setUser(teacherUser);
        entity.setEmployeeCode("EMP-REP-1");
        entity.setDepartment(department);
        entity.setDesignation("Assistant Professor");
        return teacherRepository.save(entity);
    }

    private Student createStudent(Department department) {
        User studentUser = new User();
        studentUser.setUsername("student-report");
        studentUser.setEmail("student-report@sams.local");
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(studentUser);

        Student entity = new Student();
        entity.setUser(studentUser);
        entity.setStudentCode("STU-REP-1");
        entity.setDepartment(department);
        entity.setProgram(program);
        entity.setCurrentTerm(term);
        entity.setSection(section);
        entity.setAcademicStatus(AcademicStatus.ACTIVE);
        entity.setAdmissionDate(LocalDate.of(2026, 4, 10));
        return studentRepository.save(entity);
    }

    private CourseOffering createOffering(Subject subject) {
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

    private void createEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseOffering(offering);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(Instant.now());
        enrollmentRepository.save(enrollment);
    }

    private void createPublishedExamWithMarks() {
        Exam exam = new Exam();
        exam.setCourseOffering(offering);
        exam.setTitle("Midterm");
        exam.setExamType("THEORY");
        exam.setMaxMarks(new BigDecimal("100.00"));
        exam.setWeightage(new BigDecimal("100.00"));
        exam.setScheduledAt(Instant.parse("2026-07-10T10:00:00Z"));
        exam.setPublished(true);
        exam.setPublishedAt(Instant.now());
        examRepository.save(exam);

        MarkEntry markEntry = new MarkEntry();
        markEntry.setExam(exam);
        markEntry.setStudent(student);
        markEntry.setMarksObtained(new BigDecimal("84.00"));
        com.example.sams.exam.service.GradeCalculationService.GradeSnapshot grade = gradeCalculationService.calculate(
                new BigDecimal("84.00"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        );
        markEntry.setPercentageScore(grade.percentageScore());
        markEntry.setWeightedScore(grade.weightedScore());
        markEntry.setLetterGrade(grade.letterGrade());
        markEntry.setGradePoints(grade.gradePoints());
        markEntryRepository.save(markEntry);
    }

    private void createOutstandingFee() {
        SemesterFee semesterFee = new SemesterFee();
        semesterFee.setStudent(student);
        semesterFee.setTerm(term);
        semesterFee.setBaseAmount(new BigDecimal("5000.00"));
        semesterFee.setFineAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setTotalPayable(new BigDecimal("5000.00"));
        semesterFee.setPaidAmount(BigDecimal.ZERO.setScale(2));
        semesterFee.setDueDate(LocalDate.now().plusDays(5));
        semesterFee.setStatus(SemesterFeeStatus.DUE);
        semesterFeeRepository.save(semesterFee);
    }

    private void createLowAttendance() {
        AttendanceSession first = new AttendanceSession();
        first.setCourseOffering(offering);
        first.setSessionDate(LocalDate.of(2026, 6, 10));
        attendanceSessionRepository.save(first);

        AttendanceSession second = new AttendanceSession();
        second.setCourseOffering(offering);
        second.setSessionDate(LocalDate.of(2026, 6, 12));
        attendanceSessionRepository.save(second);

        AttendanceSession third = new AttendanceSession();
        third.setCourseOffering(offering);
        third.setSessionDate(LocalDate.of(2026, 6, 14));
        attendanceSessionRepository.save(third);

        saveAttendance(first, AttendanceStatus.PRESENT);
        saveAttendance(second, AttendanceStatus.ABSENT);
        saveAttendance(third, AttendanceStatus.ABSENT);
    }

    private void saveAttendance(AttendanceSession session, AttendanceStatus status) {
        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setStatus(status);
        record.setMarkedAt(Instant.now());
        attendanceRecordRepository.save(record);
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
