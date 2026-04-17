package com.example.sams.user.service;

import com.example.sams.academic.domain.Department;
import com.example.sams.academic.repository.DepartmentRepository;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.domain.User;
import com.example.sams.user.dto.TeacherProfileResponse;
import com.example.sams.user.dto.UpdateTeacherRequest;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherAdministrationService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public TeacherAdministrationService(
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse getTeacherById(Long teacherId) {
        return toTeacherProfileResponse(getTeacher(teacherId));
    }

    @Transactional(readOnly = true)
    public Page<TeacherProfileResponse> listTeachers(Long departmentId, String query, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return teacherRepository.search(departmentId, normalizedQuery, pageable)
                .map(this::toTeacherProfileResponse);
    }

    @Transactional
    public TeacherProfileResponse updateTeacher(Long teacherId, UpdateTeacherRequest request) {
        Teacher teacher = getTeacher(teacherId);
        User user = teacher.getUser();
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        ensureTeacherUniqueness(teacher, request);

        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setAccountStatus(parseAccountStatus(request.accountStatus()));

        teacher.setEmployeeCode(request.employeeCode().trim());
        teacher.setDepartment(department);
        teacher.setDesignation(request.designation());

        return toTeacherProfileResponse(teacher);
    }

    private Teacher getTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private void ensureTeacherUniqueness(Teacher teacher, UpdateTeacherRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        String employeeCode = request.employeeCode().trim();

        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(teacher.getUser().getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("User with username already exists");
                });

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(teacher.getUser().getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("User with email already exists");
                });

        teacherRepository.findAll().stream()
                .filter(existing -> existing.getEmployeeCode().equalsIgnoreCase(employeeCode))
                .filter(existing -> !existing.getId().equals(teacher.getId()))
                .findAny()
                .ifPresent(existing -> {
                    throw new ConflictException("Teacher with employeeCode already exists");
                });
    }

    private AccountStatus parseAccountStatus(String rawStatus) {
        try {
            return AccountStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ConflictException("Invalid accountStatus. Allowed values: ACTIVE, SUSPENDED, LOCKED, DISABLED");
        }
    }

    private TeacherProfileResponse toTeacherProfileResponse(Teacher teacher) {
        Department department = teacher.getDepartment();
        User user = teacher.getUser();
        return new TeacherProfileResponse(
                teacher.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAccountStatus(),
                teacher.getEmployeeCode(),
                new TeacherProfileResponse.DepartmentSummary(
                        department.getId(),
                        department.getCode(),
                        department.getName()
                ),
                teacher.getDesignation(),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt()
        );
    }
}
