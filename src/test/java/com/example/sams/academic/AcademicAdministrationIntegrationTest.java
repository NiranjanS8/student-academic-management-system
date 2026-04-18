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
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Role;
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
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Department department;
    private Program program;
    private AcademicTerm term;

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

    @Test
    void adminCanManageCourseOfferings() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "A",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CN201",
                                  "name": "Computer Networks",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-offering",
                                  "email": "teacher-offering@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-OFFER-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("CN201").orElseThrow().getId();
        Long sectionId = sectionRepository.findAll().stream()
                .filter(section -> "A".equals(section.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-OFFER-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 60,
                                  "enrollmentOpenAt": "2026-05-01T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-25T00:00:00Z",
                                  "status": "open"
                                }
                                """.formatted(subjectId, term.getId(), sectionId, teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject.code").value("CN201"))
                .andExpect(jsonPath("$.data.teacher.employeeCode").value("EMP-OFFER-01"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        Long offeringId = courseOfferingRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/v1/admin/offerings/{offeringId}", offeringId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 75,
                                  "enrollmentOpenAt": "2026-05-01T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-28T00:00:00Z",
                                  "status": "closed"
                                }
                                """.formatted(subjectId, term.getId(), sectionId, teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(75))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/admin/offerings/{offeringId}", offeringId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(offeringId));

        mockMvc.perform(get("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("teacherId", String.valueOf(teacherId))
                        .param("subjectId", String.valueOf(subjectId))
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(offeringId));
    }

    @Test
    void adminCannotCreateCourseOfferingWithInvalidEnrollmentWindow() throws Exception {
        String accessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Z",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OS301",
                                  "name": "Operating Systems",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-offering-2",
                                  "email": "teacher-offering-2@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-OFFER-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        Long subjectId = subjectRepository.findByCodeIgnoreCase("OS301").orElseThrow().getId();
        Long sectionId = sectionRepository.findAll().stream()
                .filter(section -> "Z".equals(section.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long teacherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-OFFER-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 30,
                                  "enrollmentOpenAt": "2026-05-25T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-01T00:00:00Z",
                                  "status": "DRAFT"
                                }
                                """.formatted(subjectId, term.getId(), sectionId, teacherId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("enrollmentCloseAt cannot be before enrollmentOpenAt"));
    }

    @Test
    void teacherCanViewOnlyAssignedOfferings() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "T1",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE401",
                                  "name": "Software Engineering",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "DM402",
                                  "name": "Data Mining",
                                  "credits": 3.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-assigned-1",
                                  "email": "teacher-assigned-1@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-ASSIGN-01",
                                  "departmentId": %d,
                                  "designation": "Lecturer"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-assigned-2",
                                  "email": "teacher-assigned-2@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-ASSIGN-02",
                                  "departmentId": %d,
                                  "designation": "Senior Lecturer"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        Long sectionId = sectionRepository.findAll().stream()
                .filter(section -> "T1".equals(section.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long softwareEngineeringId = subjectRepository.findByCodeIgnoreCase("SE401").orElseThrow().getId();
        Long dataMiningId = subjectRepository.findByCodeIgnoreCase("DM402").orElseThrow().getId();
        Long teacherOneId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-ASSIGN-01".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long teacherTwoId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-ASSIGN-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 50,
                                  "enrollmentOpenAt": "2026-05-01T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-20T00:00:00Z",
                                  "status": "OPEN"
                                }
                                """.formatted(softwareEngineeringId, term.getId(), sectionId, teacherOneId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 45,
                                  "enrollmentOpenAt": "2026-05-01T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-18T00:00:00Z",
                                  "status": "CLOSED"
                                }
                                """.formatted(dataMiningId, term.getId(), sectionId, teacherTwoId)))
                .andExpect(status().isOk());

        Long assignedOfferingId = courseOfferingRepository.findAllByTeacherId(teacherOneId).stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String teacherAccessToken = extractAccessToken("teacher-assigned-1", "Teacher@123");

        mockMvc.perform(get("/api/v1/teacher/offerings")
                        .header("Authorization", "Bearer " + teacherAccessToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(assignedOfferingId))
                .andExpect(jsonPath("$.data.content[0].subject.code").value("SE401"))
                .andExpect(jsonPath("$.data.content[0].teacher.employeeCode").value("EMP-ASSIGN-01"));

        mockMvc.perform(get("/api/v1/teacher/offerings/{offeringId}", assignedOfferingId)
                        .header("Authorization", "Bearer " + teacherAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignedOfferingId))
                .andExpect(jsonPath("$.data.subject.code").value("SE401"));
    }

    @Test
    void teacherCannotViewAnotherTeachersOffering() throws Exception {
        String adminAccessToken = extractAccessToken();

        mockMvc.perform(post("/api/v1/admin/academic/sections")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "T2",
                                  "programId": %d,
                                  "currentTermId": %d
                                }
                                """.formatted(program.getId(), term.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/academic/subjects")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AI403",
                                  "name": "Applied AI",
                                  "credits": 4.00,
                                  "departmentId": %d,
                                  "active": true
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-own",
                                  "email": "teacher-own@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-OWN-01",
                                  "departmentId": %d,
                                  "designation": "Assistant Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users/teachers")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher-other",
                                  "email": "teacher-other@sams.local",
                                  "password": "Teacher@123",
                                  "employeeCode": "EMP-OWN-02",
                                  "departmentId": %d,
                                  "designation": "Professor"
                                }
                                """.formatted(department.getId())))
                .andExpect(status().isOk());

        Long sectionId = sectionRepository.findAll().stream()
                .filter(section -> "T2".equals(section.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long subjectId = subjectRepository.findByCodeIgnoreCase("AI403").orElseThrow().getId();
        Long teacherOtherId = teacherRepository.findAll().stream()
                .filter(teacher -> "EMP-OWN-02".equals(teacher.getEmployeeCode()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/admin/offerings")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "termId": %d,
                                  "sectionId": %d,
                                  "teacherId": %d,
                                  "capacity": 40,
                                  "enrollmentOpenAt": "2026-05-01T00:00:00Z",
                                  "enrollmentCloseAt": "2026-05-15T00:00:00Z",
                                  "status": "OPEN"
                                }
                                """.formatted(subjectId, term.getId(), sectionId, teacherOtherId)))
                .andExpect(status().isOk());

        Long otherTeacherOfferingId = courseOfferingRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String teacherAccessToken = extractAccessToken("teacher-own", "Teacher@123");

        mockMvc.perform(get("/api/v1/teacher/offerings/{offeringId}", otherTeacherOfferingId)
                        .header("Authorization", "Bearer " + teacherAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course offering not found"));
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
}
