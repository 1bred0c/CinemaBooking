package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationResponse> getMyNotifications(
            UUID currentUserId,
            Pageable pageable
    );

    long getUnreadCount(UUID currentUserId);

    NotificationResponse markRead(
            UUID currentUserId,
            UUID notificationId
    );

    int markAllRead(UUID currentUserId);
}
