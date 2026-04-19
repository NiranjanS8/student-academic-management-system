package com.example.sams.attendance;

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
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherAttendanceIntegrationTest {

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

    private Department department;
    private Program program;
    private AcademicTerm term;
    private Section section;
    private Subject subject;
    private Teacher assignedTeacher;
    private Teacher otherTeacher;
    private Student enrolledStudent;
    private Student nonEnrolledStudent;
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

        createAdmin();
        assignedTeacher = createTeacher("teacher-att-owner", "teacher-att-owner@sams.local", "EMP-ATT-1");
        otherTeacher = createTeacher("teacher-att-other", "teacher-att-other@sams.local", "EMP-ATT-2");
        enrolledStudent = createStudent("student-att-enrolled", "student-att-enrolled@sams.local", "STU-ATT-1");
        nonEnrolledStudent = createStudent("student-att-other", "student-att-other@sams.local", "STU-ATT-2");
        offering = createOffering(assignedTeacher);
        createEnrollment(enrolledStudent);
    }

    @Test
    void teacherCanManageAttendanceForAssignedOffering() throws Exception {
        String teacherToken = extractAccessToken("teacher-att-owner", "Teacher@123");

        mockMvc.perform(get("/api/v1/teacher/attendance/offerings/{offeringId}/students", offering.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].studentId").value(enrolledStudent.getId()))
                .andExpect(jsonPath("$.data[0].studentCode").value("STU-ATT-1"));

        String createResponse = mockMvc.perform(post("/api/v1/teacher/attendance/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "sessionDate": "2026-06-10",
                                  "records": [
                                    {
                                      "studentId": %d,
                                      "status": "PRESENT"
                                    }
                                  ]
                                }
                                """.formatted(offering.getId(), enrolledStudent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseOfferingId").value(offering.getId()))
                .andExpect(jsonPath("$.data.totalRecords").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("PRESENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long sessionId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/teacher/attendance/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("offeringId", String.valueOf(offering.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(sessionId));

        mockMvc.perform(get("/api/v1/teacher/attendance/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].studentId").value(enrolledStudent.getId()))
                .andExpect(jsonPath("$.data.records[0].status").value("PRESENT"));

        mockMvc.perform(put("/api/v1/teacher/attendance/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "records": [
                                    {
                                      "studentId": %d,
                                      "status": "ABSENT"
                                    }
                                  ]
                                }
                                """.formatted(enrolledStudent.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].status").value("ABSENT"));
    }

    @Test
    void teacherAttendanceRejectsNonEnrolledStudentsAndNonOwnerAccess() throws Exception {
        String teacherToken = extractAccessToken("teacher-att-owner", "Teacher@123");
        String otherTeacherToken = extractAccessToken("teacher-att-other", "Teacher@123");

        mockMvc.perform(post("/api/v1/teacher/attendance/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "sessionDate": "2026-06-11",
                                  "records": [
                                    {
                                      "studentId": %d,
                                      "status": "ABSENT"
                                    }
                                  ]
                                }
                                """.formatted(offering.getId(), nonEnrolledStudent.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Attendance can only be marked for actively enrolled students"));

        String createResponse = mockMvc.perform(post("/api/v1/teacher/attendance/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseOfferingId": %d,
                                  "sessionDate": "2026-06-12",
                                  "records": [
                                    {
                                      "studentId": %d,
                                      "status": "PRESENT"
                                    }
                                  ]
                                }
                                """.formatted(offering.getId(), enrolledStudent.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long sessionId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/teacher/attendance/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + otherTeacherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Attendance session not found"));
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

    private Teacher createTeacher(String username, String email, String employeeCode) {
        User teacherUser = new User();
        teacherUser.setUsername(username);
        teacherUser.setEmail(email);
        teacherUser.setPasswordHash(passwordEncoder.encode("Teacher@123"));
        teacherUser.setRole(Role.TEACHER);
        teacherUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(teacherUser);

        Teacher entity = new Teacher();
        entity.setUser(teacherUser);
        entity.setEmployeeCode(employeeCode);
        entity.setDepartment(department);
        entity.setDesignation("Assistant Professor");
        return teacherRepository.save(entity);
    }

    private Student createStudent(String username, String email, String studentCode) {
        User studentUser = new User();
        studentUser.setUsername(username);
        studentUser.setEmail(email);
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(Role.STUDENT);
        studentUser.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(studentUser);

        Student entity = new Student();
        entity.setUser(studentUser);
        entity.setStudentCode(studentCode);
        entity.setDepartment(department);
        entity.setProgram(program);
        entity.setCurrentTerm(term);
        entity.setSection(section);
        entity.setAcademicStatus(AcademicStatus.ACTIVE);
        entity.setAdmissionDate(LocalDate.of(2026, 4, 10));
        return studentRepository.save(entity);
    }

    private CourseOffering createOffering(Teacher teacher) {
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

    private void createEnrollment(Student student) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseOffering(offering);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(Instant.now());
        enrollmentRepository.save(enrollment);
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
