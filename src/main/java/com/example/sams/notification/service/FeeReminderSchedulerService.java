package com.example.sams.notification.service;

import com.example.sams.fee.domain.SemesterFee;
import com.example.sams.fee.repository.SemesterFeeRepository;
import com.example.sams.fee.service.FeePolicyService;
import com.example.sams.notification.domain.NotificationType;
import com.example.sams.notification.repository.NotificationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeReminderSchedulerService {

    private final SemesterFeeRepository semesterFeeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final FeePolicyService feePolicyService;
    private final int upcomingReminderDays;

    public FeeReminderSchedulerService(
            SemesterFeeRepository semesterFeeRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            FeePolicyService feePolicyService,
            @Value("${sams.scheduler.fee-reminder.upcoming-days:3}") int upcomingReminderDays
    ) {
        this.semesterFeeRepository = semesterFeeRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.feePolicyService = feePolicyService;
        this.upcomingReminderDays = upcomingReminderDays;
    }

    @Scheduled(cron = "${sams.scheduler.fee-reminder.cron:0 0 9 * * *}")
    @Transactional
    public void sendDailyFeeDueReminders() {
        processFeeDueReminders(LocalDate.now());
    }

    @Transactional
    public int processFeeDueReminders(LocalDate today) {
        LocalDate maxCandidateDate = today.plusDays(upcomingReminderDays);
        List<SemesterFee> candidates = semesterFeeRepository.findOutstandingReminderCandidates(maxCandidateDate).stream()
                .map(feePolicyService::synchronizeFeeState)
                .toList();

        int sentCount = 0;
        for (SemesterFee semesterFee : candidates) {
            ReminderStage reminderStage = determineStage(semesterFee, today);
            if (reminderStage == null) {
                continue;
            }

            String dedupKey = buildDedupKey(semesterFee, reminderStage);
            if (notificationRepository.existsByDedupKey(dedupKey)) {
                continue;
            }

            notificationService.createNotification(
                    semesterFee.getStudent().getUser(),
                    NotificationType.FEE_DUE_REMINDER,
                    reminderStage.title(),
                    buildMessage(semesterFee, reminderStage, today),
                    dedupKey
            );
            sentCount++;
        }

        return sentCount;
    }

    private ReminderStage determineStage(SemesterFee semesterFee, LocalDate today) {
        if (semesterFee.getPaidAmount().compareTo(semesterFee.getTotalPayable()) >= 0) {
            return null;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, semesterFee.getDueDate());
        if (daysUntilDue == upcomingReminderDays) {
            return ReminderStage.UPCOMING;
        }
        if (daysUntilDue == 0) {
            return ReminderStage.DUE_TODAY;
        }
        if (daysUntilDue < 0) {
            return ReminderStage.OVERDUE;
        }
        return null;
    }

    private String buildMessage(SemesterFee semesterFee, ReminderStage reminderStage, LocalDate today) {
        BigDecimal outstanding = semesterFee.getTotalPayable()
                .subtract(semesterFee.getPaidAmount())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        String termLabel = "%s (%s)".formatted(
                semesterFee.getTerm().getName(),
                semesterFee.getTerm().getAcademicYear()
        );

        return switch (reminderStage) {
            case UPCOMING -> "Your semester fee of %s for %s is due on %s. Please pay before the deadline."
                    .formatted(outstanding, termLabel, semesterFee.getDueDate());
            case DUE_TODAY -> "Your semester fee of %s for %s is due today. Please complete the payment to avoid overdue penalties."
                    .formatted(outstanding, termLabel);
            case OVERDUE -> "Your semester fee of %s for %s is overdue since %s. Please pay as soon as possible."
                    .formatted(outstanding, termLabel, semesterFee.getDueDate());
        };
    }

    private String buildDedupKey(SemesterFee semesterFee, ReminderStage reminderStage) {
        return "fee-reminder:%d:%s".formatted(semesterFee.getId(), reminderStage.name());
    }

    private enum ReminderStage {
        UPCOMING("Fee due reminder"),
        DUE_TODAY("Fee due today"),
        OVERDUE("Fee overdue");

        private final String title;

        ReminderStage(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }
}
