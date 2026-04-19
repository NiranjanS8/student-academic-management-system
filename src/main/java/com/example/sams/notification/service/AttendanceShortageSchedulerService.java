package com.example.sams.notification.service;

import com.example.sams.attendance.service.AttendanceAnalyticsService;
import com.example.sams.notification.domain.NotificationType;
import com.example.sams.notification.repository.NotificationRepository;
import com.example.sams.user.domain.User;
import com.example.sams.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceShortageSchedulerService {

    private final AttendanceAnalyticsService attendanceAnalyticsService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BigDecimal minimumPercentage;

    public AttendanceShortageSchedulerService(
            AttendanceAnalyticsService attendanceAnalyticsService,
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            @Value("${sams.scheduler.attendance-shortage.minimum-percentage:75}") BigDecimal minimumPercentage
    ) {
        this.attendanceAnalyticsService = attendanceAnalyticsService;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.minimumPercentage = minimumPercentage;
    }

    @Scheduled(cron = "${sams.scheduler.attendance-shortage.cron:0 0 8 * * *}")
    @Transactional
    public void scanAttendanceShortages() {
        processAttendanceShortages();
    }

    @Transactional
    public int processAttendanceShortages() {
        java.util.List<AttendanceAnalyticsService.AttendanceShortageCandidate> candidates =
                attendanceAnalyticsService.findShortageCandidates(minimumPercentage);
        Map<Long, User> usersById = userRepository.findAllById(
                        candidates.stream().map(AttendanceAnalyticsService.AttendanceShortageCandidate::userId).distinct().toList()
                ).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));

        int sentCount = 0;
        for (AttendanceAnalyticsService.AttendanceShortageCandidate candidate
                : candidates) {
            String dedupKey = "attendance-shortage:%d:%d".formatted(candidate.courseOfferingId(), candidate.studentId());
            if (notificationRepository.existsByDedupKey(dedupKey)) {
                continue;
            }

            notificationService.createNotification(
                    java.util.Optional.ofNullable(usersById.get(candidate.userId())).orElseThrow(),
                    NotificationType.ATTENDANCE_WARNING,
                    "Low attendance warning",
                    "Your attendance in %s - %s is %s%% (%d/%d sessions), below the required %s%%."
                            .formatted(
                                    candidate.subjectCode(),
                                    candidate.subjectName(),
                                    candidate.attendancePercentage(),
                                    candidate.presentSessions(),
                                    candidate.totalSessions(),
                                    minimumPercentage.setScale(0)
                            ),
                    dedupKey
            );
            sentCount++;
        }
        return sentCount;
    }
}
