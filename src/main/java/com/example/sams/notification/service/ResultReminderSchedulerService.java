package com.example.sams.notification.service;

import com.example.sams.notification.domain.Notification;
import com.example.sams.notification.domain.NotificationType;
import com.example.sams.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultReminderSchedulerService {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final long delayDays;

    public ResultReminderSchedulerService(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            @Value("${sams.scheduler.result-reminder.delay-days:1}") long delayDays
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.delayDays = delayDays;
    }

    @Scheduled(cron = "${sams.scheduler.result-reminder.cron:0 30 9 * * *}")
    @Transactional
    public void sendResultReminders() {
        processResultReminders(Instant.now());
    }

    @Transactional
    public int processResultReminders(Instant now) {
        Instant cutoff = now.minus(delayDays, ChronoUnit.DAYS);
        List<Notification> notifications = notificationRepository
                .findAllByTypeAndReadFalseAndCreatedAtBefore(NotificationType.RESULT_PUBLISHED, cutoff);

        int sentCount = 0;
        for (Notification publishedNotification : notifications) {
            String dedupKey = "result-reminder:%d".formatted(publishedNotification.getId());
            if (notificationRepository.existsByDedupKey(dedupKey)) {
                continue;
            }

            notificationService.createNotification(
                    publishedNotification.getUser(),
                    NotificationType.RESULT_REMINDER,
                    "Result reminder",
                    "You still have an unread published result notification. %s".formatted(publishedNotification.getMessage()),
                    dedupKey
            );
            sentCount++;
        }

        return sentCount;
    }
}
