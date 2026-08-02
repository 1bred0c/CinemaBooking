package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.NotificationResponse;
import congtuong.dev.cinemabooking.entity.Notification;
import congtuong.dev.cinemabooking.exception.NotificationException;
import congtuong.dev.cinemabooking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<NotificationResponse> getMyNotifications(
            UUID currentUserId,
            Pageable pageable
    ) {
        return notificationRepository
                .findAllByRecipientId(currentUserId, pageable)
                .map(this::toResponse);
    }

    @Override
    public long getUnreadCount(UUID currentUserId) {
        return notificationRepository
                .countByRecipientIdAndReadAtIsNull(currentUserId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(
            UUID currentUserId,
            UUID notificationId
    ) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, currentUserId)
                .orElseThrow(() ->
                        new NotificationException("Notification not found")
                );
        notification.markRead(Instant.now());
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(UUID currentUserId) {
        return notificationRepository.markAllRead(
                currentUserId,
                Instant.now()
        );
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
