package com.example.sams.notification.service;

import com.example.sams.auth.exception.AuthenticationException;
import com.example.sams.common.exception.ConflictException;
import com.example.sams.common.exception.ResourceNotFoundException;
import com.example.sams.enrollment.domain.Enrollment;
import com.example.sams.enrollment.domain.EnrollmentStatus;
import com.example.sams.enrollment.repository.EnrollmentRepository;
import com.example.sams.exam.domain.Exam;
import com.example.sams.notification.domain.Notification;
import com.example.sams.notification.domain.NotificationType;
import com.example.sams.notification.dto.AnnouncementDispatchResponse;
import com.example.sams.notification.dto.NotificationResponse;
import com.example.sams.notification.dto.TeacherAnnouncementRequest;
import com.example.sams.notification.dto.UnreadNotificationCountResponse;
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.offering.domain.CourseOffering;
import com.example.sams.offering.repository.CourseOfferingRepository;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.UserRepository;
import com.example.sams.user.service.AppUserDetails;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationResponseMapper notificationResponseMapper;
    private final UserRepository userRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationResponseMapper notificationResponseMapper,
            UserRepository userRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationResponseMapper = notificationResponseMapper;
        this.userRepository = userRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public void notifyEnrollmentConfirmed(Enrollment enrollment) {
        CourseOffering offering = enrollment.getCourseOffering();
        String subjectCode = offering.getSubject().getCode();
        String subjectName = offering.getSubject().getName();
        String termName = offering.getTerm().getName();

        createNotification(
                enrollment.getStudent().getUser(),
                NotificationType.ENROLLMENT_CONFIRMED,
                "Enrollment confirmed",
                "You are enrolled in %s - %s for %s.".formatted(subjectCode, subjectName, termName)
        );
    }

    @Transactional
    public void notifyExamPublished(Exam exam) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByCourseOfferingIdAndStatus(
                exam.getCourseOffering().getId(),
                EnrollmentStatus.ENROLLED
        );

        if (enrollments.isEmpty()) {
            return;
        }

        String subjectCode = exam.getCourseOffering().getSubject().getCode();
        String subjectName = exam.getCourseOffering().getSubject().getName();
        String title = "Result published";
        String message = "Results for %s - %s (%s) are now available."
                .formatted(subjectCode, subjectName, exam.getTitle());

        createNotifications(
                enrollments.stream().map(enrollment -> enrollment.getStudent().getUser()).toList(),
                NotificationType.RESULT_PUBLISHED,
                title,
                message
        );
    }

    @Transactional
    public AnnouncementDispatchResponse publishTeacherAnnouncement(TeacherAnnouncementRequest request) {
        Long teacherUserId = getAuthenticatedUserId();
        CourseOffering offering = courseOfferingRepository.findByIdAndTeacherUserId(request.courseOfferingId(), teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));

        List<User> recipients = enrollmentRepository.findAllByCourseOfferingIdAndStatus(
                        offering.getId(),
                        EnrollmentStatus.ENROLLED
                ).stream()
                .map(enrollment -> enrollment.getStudent().getUser())
                .toList();

        if (recipients.isEmpty()) {
            throw new ConflictException("Cannot publish announcement without active enrollments");
        }

        String title = request.title().trim();
        String message = "%s [%s - %s]".formatted(
                request.message().trim(),
                offering.getSubject().getCode(),
                offering.getSubject().getName()
        );

        createNotifications(recipients, NotificationType.TEACHER_ANNOUNCEMENT, title, message);

        return new AnnouncementDispatchResponse(offering.getId(), title, recipients.size());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMyNotifications(Boolean read, Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        Page<Notification> page = read == null
                ? notificationRepository.findAllByUserId(userId, pageable)
                : notificationRepository.findAllByUserIdAndRead(userId, read, pageable);
        return page.map(notificationResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount() {
        return new UnreadNotificationCountResponse(notificationRepository.countByUserIdAndReadFalse(getAuthenticatedUserId()));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }

        return notificationResponseMapper.toResponse(notification);
    }

    @Transactional
    public void createNotification(User user, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotifications(List<User> users, NotificationType type, String title, String message) {
        List<Notification> notifications = users.stream()
                .distinct()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setType(type);
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setRead(false);
                    return notification;
                })
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        return userRepository.findById(getAuthenticatedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails principal)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return principal.getUserId();
    }
}
