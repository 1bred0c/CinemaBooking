package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.dto.response.NotificationResponse;
import congtuong.dev.cinemabooking.entity.Notification;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.entity.enums.NotificationType;
import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.NotificationRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationEventService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Optional<NotificationResponse> create(
            UserRegisteredEvent event
    ) {
        return createIfAbsent(
                event.eventId(),
                event.userId(),
                NotificationType.WELCOME,
                "Chào mừng đến CinemaBooking",
                "Xin chào %s! Tài khoản của bạn đã được tạo thành công."
                        .formatted(event.fullName()),
                event.userId(),
                event.occurredAt()
        );
    }

    @Transactional
    public Optional<NotificationResponse> create(
            BookingNotificationEvent event
    ) {
        NotificationContent content = contentOf(event);
        return createIfAbsent(
                event.eventId(),
                event.userId(),
                event.type(),
                content.title(),
                content.message(),
                event.bookingId(),
                event.occurredAt()
        );
    }

    private Optional<NotificationResponse> createIfAbsent(
            UUID eventId,
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID referenceId,
            java.time.Instant occurredAt
    ) {
        if (notificationRepository.existsById(eventId)) {
            return Optional.empty();
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        Notification saved = notificationRepository.save(
                Notification.create(
                        eventId,
                        user,
                        type,
                        title,
                        message,
                        referenceId,
                        occurredAt
                )
        );
        return Optional.of(toResponse(saved));
    }

    private NotificationContent contentOf(
            BookingNotificationEvent event
    ) {
        return switch (event.type()) {
            case BOOKING_CONFIRMED -> new NotificationContent(
                    "Đặt vé thành công",
                    "Booking cho phim \"%s\" đã được xác nhận."
                            .formatted(event.movieTitle())
            );
            case BOOKING_EXPIRED -> new NotificationContent(
                    "Booking đã hết hạn",
                    "Booking cho phim \"%s\" đã hết thời gian thanh toán."
                            .formatted(event.movieTitle())
            );
            case PAYMENT_FAILED -> new NotificationContent(
                    "Thanh toán thất bại",
                    "Thanh toán booking phim \"%s\" thất bại%s."
                            .formatted(
                                    event.movieTitle(),
                                    event.reason() == null
                                            ? ""
                                            : ": " + event.reason()
                            )
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported booking notification type: " + event.type()
            );
        };
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

    private record NotificationContent(String title, String message) {
    }
}
