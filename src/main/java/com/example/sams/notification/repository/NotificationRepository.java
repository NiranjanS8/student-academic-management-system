package com.example.sams.notification.repository;

import com.example.sams.notification.domain.Notification;
import com.example.sams.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    Page<Notification> findAllByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long notificationId, Long userId);

    boolean existsByDedupKey(String dedupKey);

    List<Notification> findAllByTypeAndReadFalseAndCreatedAtBefore(NotificationType type, Instant createdAt);

    List<Notification> findAllByUserIdAndReadFalse(Long userId);

    long deleteByReadTrueAndReadAtBefore(Instant cutoff);

    long countByUserIdAndReadFalse(Long userId);
}
