package com.example.sams.notification.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCleanupSchedulerService {

    private final NotificationService notificationService;
    private final long retentionDays;

    public NotificationCleanupSchedulerService(
            NotificationService notificationService,
            @Value("${sams.scheduler.notification-cleanup.retention-days:30}") long retentionDays
    ) {
        this.notificationService = notificationService;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${sams.scheduler.notification-cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupStaleNotifications() {
        processCleanup(Instant.now());
    }

    @Transactional
    public long processCleanup(Instant now) {
        Instant cutoff = now.minus(retentionDays, ChronoUnit.DAYS);
        return notificationService.deleteReadNotificationsBefore(cutoff);
    }
}
