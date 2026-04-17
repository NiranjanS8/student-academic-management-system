package com.example.sams.auth.service;

import com.example.sams.auth.domain.RefreshToken;
import com.example.sams.auth.dto.AuthResponse;
import com.example.sams.auth.dto.CurrentUserResponse;
import com.example.sams.auth.dto.LoginRequest;
import com.example.sams.auth.dto.RefreshTokenRequest;
import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.auth.repository.RefreshTokenRepository;
import com.example.sams.auth.security.JwtTokenProvider;
import com.example.sams.user.domain.AccountStatus;
import com.example.sams.user.domain.Student;
import com.example.sams.user.domain.Teacher;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.StudentRepository;
import com.example.sams.user.repository.TeacherRepository;
import com.example.sams.user.repository.UserRepository;
import com.example.sams.user.service.AppUserDetails;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AuthenticationException("Authenticated user could not be loaded"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationException("Account is not active");
        }

        user.setLastLoginAt(Instant.now());
        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(jwtTokenProvider.extractExpiration(refreshTokenValue));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return toAuthResponse(user, accessToken, refreshTokenValue);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshTokenValue = request.refreshToken();
        if (!jwtTokenProvider.isRefreshToken(refreshTokenValue) || !jwtTokenProvider.isTokenValid(refreshTokenValue)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthenticationException("Refresh token not found"));

        if (savedToken.isRevoked() || savedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException("Refresh token is expired or revoked");
        }

        User user = savedToken.getUser();
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationException("Account is not active");
        }

        savedToken.setRevoked(true);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(user);

        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setToken(newRefreshTokenValue);
        replacement.setExpiresAt(jwtTokenProvider.extractExpiration(newRefreshTokenValue));
        replacement.setRevoked(false);
        refreshTokenRepository.save(replacement);

        return toAuthResponse(user, newAccessToken, newRefreshTokenValue);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        CurrentUserResponse.StudentSummary studentSummary = studentRepository.findByUserId(user.getId())
                .map(this::toStudentSummary)
                .orElse(null);

        CurrentUserResponse.TeacherSummary teacherSummary = teacherRepository.findByUserId(user.getId())
                .map(this::toTeacherSummary)
                .orElse(null);

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                studentSummary,
                teacherSummary
        );
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private AuthResponse toAuthResponse(User user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.accessTokenExpiresInSeconds(),
                new AuthResponse.UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole())
        );
    }

    private CurrentUserResponse.StudentSummary toStudentSummary(Student student) {
        return new CurrentUserResponse.StudentSummary(
                student.getId(),
                student.getStudentCode(),
                student.getAcademicStatus().name()
        );
    }

    private CurrentUserResponse.TeacherSummary toTeacherSummary(Teacher teacher) {
        return new CurrentUserResponse.TeacherSummary(
                teacher.getId(),
                teacher.getEmployeeCode(),
                teacher.getDesignation()
        );
    }
}
